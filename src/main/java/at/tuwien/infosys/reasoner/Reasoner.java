package at.tuwien.infosys.reasoner;

import at.tuwien.infosys.configuration.OperatorConfiguration;
import at.tuwien.infosys.datasources.DockerContainerRepository;
import at.tuwien.infosys.datasources.DockerHostRepository;
import at.tuwien.infosys.entities.*;
import at.tuwien.infosys.monitoring.AvailabilityWatchdog;
import at.tuwien.infosys.monitoring.Monitor;
import at.tuwien.infosys.resourceManagement.OpenstackConnector;
import at.tuwien.infosys.resourceManagement.ProcessingNodeManagement;
import at.tuwien.infosys.topology.TopologyManagement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class Reasoner {

    @Autowired
    TopologyManagement topologyMgmt;

    @Autowired
    OpenstackConnector openstackConnector;

    @Autowired
    OperatorConfiguration opConfig;

    @Autowired
    ProcessingNodeManagement pcm;

    @Autowired
    private DockerHostRepository dhr;

    @Autowired
    private DockerContainerRepository dcr;

    @Autowired
    private AvailabilityWatchdog availabilityWatchdog;

    @Autowired
    private Monitor monitor;

    @Value("${visp.shutdown.graceperiod}")
    private Integer graceperiod;

    @Value("${visp.infrastructurehost}")
    private String infrastructureHost;

    private static final Logger LOG = LoggerFactory.getLogger(Reasoner.class);

    public synchronized void updateResourceconfiguration() {
        availabilityWatchdog.checkAvailablitiyOfContainer();

        pcm.removeContainerWhichAreFlaggedToShutdown();
        openstackConnector.removeHostsWhichAreFlaggedToShutdown();

        LOG.info("VISP - Start Reasoner");

        LOG.info("VISP - Start container scaling");

        for (String operator : topologyMgmt.getOperatorsAsList()) {
            ScalingAction action = monitor.analyze(operator, infrastructureHost);

            if (action.equals(ScalingAction.SCALEDOWN)) {
                pcm.scaleDown(operator);
            }

            if (action.equals(ScalingAction.SCALEUP)) {
                DockerContainer dc = opConfig.createDockerContainerConfiguration(operator);
                pcm.scaleup(dc, selectSuitableDockerHost(dc), infrastructureHost);
            }
        }
        LOG.info("VISP - Finished container scaling");

        List<ResourceAvailability> freeResources = calculateFreeResourcesforHosts();

        ResourceAvailability aggregatedFreeResources = calculateFreeresources(freeResources);

        //TODO may be adopted
        //only scale down if there are 2 hosts availabe in general

        if ((aggregatedFreeResources.getCpuCores() > 5.0 && (aggregatedFreeResources.getRam() > 1000.0) && (aggregatedFreeResources.getStorage() > 5))) {
            Collections.sort(freeResources, ResourceComparator.AMOUNTOFCONTAINERASC);

            //select host with the least running container
            String hostToKill = freeResources.get(0).getHostId();

            openstackConnector.markHostForRemoval(hostToKill);

            List<DockerContainer> containerToMigrate = dcr.findByHost(hostToKill);

            //migrate Container
            for (DockerContainer dc : containerToMigrate) {
                if (dc.getStatus() == null) {
                    dc.setStatus("running");
                }

                if (dc.getStatus().equals("stopping")) {
                    continue;
                }
                pcm.triggerShutdown(dc);
                pcm.scaleup(dc, selectSuitableDockerHost(dc), infrastructureHost);
            }
        }

        LOG.info("VISP - Finished Reasoner");
    }

    public synchronized DockerHost selectSuitableDockerHost(DockerContainer dc) {
        List<ResourceAvailability> freeResources = calculateFreeResourcesforHosts();

        String host = binPackingStrategy(dc, freeResources);
        //String host = equalDistributionStrategy(dc, freeResources);

        if (host == null) {
            DockerHost dh = new DockerHost("additionaldockerhost");
            dh.setFlavour("m2.medium");
            return openstackConnector.startVM(dh);
        } else {
            return dhr.findByName(host).get(0);
        }
    }

    private String equalDistributionStrategy(DockerContainer dc, List<ResourceAvailability> freeResources) {
        //use all hosts equally
        //select the host with most free CPU resources
        Collections.sort(freeResources, ResourceComparator.FREECPUCORESASC);
        return selectFirstFitForResources(dc, freeResources);
    }

    private String binPackingStrategy(DockerContainer dc, List<ResourceAvailability> freeResources) {
        //minimize the hosts
        //select the host with least free CPU resources
        Collections.sort(freeResources, ResourceComparator.FREECPUCORESDESC);
        return selectFirstFitForResources(dc, freeResources);
    }

    private String selectFirstFitForResources(DockerContainer dc, List<ResourceAvailability> freeResources) {
        LOG.info("###### select suitable container for: ######");
        LOG.info("Containerspecs: CPU: " + dc.getCpuCores() + " - RAM: " + dc.getRam() + " - Storage: " + dc.getStorage());
        for (ResourceAvailability ra : freeResources) {
            if (ra.getCpuCores() <= dc.getCpuCores()) {
                continue;
            }
            if (ra.getRam() <= dc.getRam()) {
                continue;
            }
            if (ra.getStorage() <= dc.getStorage()) {
                continue;
            }
            LOG.info("Host found: " + ra.getName());
            LOG.info("###### select suitable container for ######");
            return ra.getName();
        }
        LOG.info("No suitable host found.");
        LOG.info("###### select suitable container for ######");
        return null;
    }

    private List<ResourceAvailability> calculateFreeResourcesforHosts() {
        Map<String, ResourceAvailability> hostResourceUsage = new HashMap<>();
        List<ResourceAvailability> freeResources = new ArrayList<>();

        for (DockerHost dh : dhr.findAll()) {
            ResourceAvailability rc = new ResourceAvailability(dh.getName(), 0, 0.0, 0, 0.0F, "dockercontainer", dh.getName());
            hostResourceUsage.put(dh.getName(), rc);
        }

        //collect current usage of cloud resources
        for (DockerContainer dc : dcr.findAll()) {
                ResourceAvailability rc = hostResourceUsage.get(dc.getHost());
                rc.setAmountOfContainer(rc.getAmountOfContainer() + 1);
                rc.setCpuCores(rc.getCpuCores() + dc.getCpuCores());
                rc.setRam(rc.getRam() + rc.getRam());
                rc.setStorage(rc.getStorage() + rc.getStorage());
                rc.setName(dc.getHost());
                hostResourceUsage.put(dc.getHost(), rc);
        }

        //calculate how much resources are left on a specific host

        for (Map.Entry<String, ResourceAvailability> entry : hostResourceUsage.entrySet()) {
            String name = entry.getKey();
            ResourceAvailability usage = entry.getValue();
            DockerHost dh = dhr.findByName(name).get(0);

            //omit all dockerhosts which are scheduled for shutdown
            if (dh.getScheduledForShutdown()) {
                LOG.info("omitted host: " + dh.getName() + "for scheduling, since it is scheduled to shut down.");
                continue;
            }

            ResourceAvailability availability = new ResourceAvailability();
            availability.setHostId(dh.getName());
            availability.setUrl(dh.getUrl());
            availability.setAmountOfContainer(usage.getAmountOfContainer());
            availability.setCpuCores(dh.getCores()-usage.getCpuCores());
            availability.setRam(dh.getRam()-usage.getRam());
            availability.setStorage(dh.getStorage()-usage.getStorage());
            freeResources.add(availability);

        }

        return freeResources;
    }

    public ResourceAvailability calculateFreeresources(List<ResourceAvailability> resources) {
        ResourceAvailability all = new ResourceAvailability();
        all.setAmountOfContainer(0);
        all.setCpuCores(0.0);
        all.setRam(0);
        all.setStorage(0.0F);


        LOG.info("###### free resources ######");
        for (ResourceAvailability ra : resources) {
            all.setAmountOfContainer(all.getAmountOfContainer() + ra.getAmountOfContainer());
            all.setCpuCores(all.getCpuCores() + ra.getCpuCores());
            all.setRam(all.getRam() + ra.getRam());
            all.setStorage(all.getStorage() + ra.getStorage());

            LOG.info(ra.getHostId() + " - Container: " + ra.getAmountOfContainer() + " - CPU: " + ra.getCpuCores() + " - RAM: " + ra.getRam() + " - Storage: " + ra.getStorage());
        }
        LOG.info("###### free resources ######");

        return all;
    }
}
