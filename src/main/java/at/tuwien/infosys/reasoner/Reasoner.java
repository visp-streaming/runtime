package at.tuwien.infosys.reasoner;

import at.tuwien.infosys.datasources.DockerContainerRepository;
import at.tuwien.infosys.datasources.DockerHostRepository;
import at.tuwien.infosys.entities.*;
import at.tuwien.infosys.monitoring.Monitor;
import at.tuwien.infosys.processingNodeDeployment.OpenstackVmManagement;
import at.tuwien.infosys.processingNodeDeployment.ProcessingNodeManagement;
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
    OpenstackVmManagement omgmt;

    @Autowired
    ProcessingNodeManagement pcm;

    @Autowired
    private DockerHostRepository dhr;

    @Autowired
    private DockerContainerRepository dcr;

    @Autowired
    Monitor monitor;

    @Value("${visp.shutdown.graceperiod}")
    private Integer graceperiod;

    @Value("${visp.infrastructurehost}")
    private String infrastructureHost;

    @Value("${visp.operator.cpu}")
    private Double operatorCPU;

    @Value("${visp.operator.ram}")
    private Integer operatorRAM;

    @Value("${visp.operator.storage}")
    private Integer operatorStorage;

    private static final Logger LOG = LoggerFactory.getLogger(Reasoner.class);

    public void setup() {

        topologyMgmt.createMapping(infrastructureHost);
        String host = omgmt.startVM("dockerHost");

        try {
            Thread.sleep(60000);
        } catch (InterruptedException e) {
            LOG.error("Could not startup initial Host.", e);
        }

        //TODO fixme
        pcm.initializeTopology(host, infrastructureHost);


        //TODO implement host management in reasoner
    }

    public void updateResourceconfiguration() {
        pcm.housekeeping();

        LOG.info("VISP - Start Reasoner");

        List<ResourceAvailability> freeResources = calculateFreeResources();
        Collections.sort(freeResources, ResourceComparator.AMOUNTOFCONTAINERASC);

        //binpacking strategy
        //kill only those hosts with no containers


        //average strategy
        //TODO come up with a decision approach when to scale down, e.g. 30 % of the resources are not used
        //migrate containers
        //kill the hosts


        //TODO consider host scaling also in here
        //TODO implement a host configuration (always maximize the deployment of operators on one host)
        //TODO implement a migration mechanism (spawn a new one and scale down the old one)
        //TODO monitor the resource usage of a host


        //TODO implement cleanup and migration functionality for docker hosts --> start new container on other host

        //host scaledown:
        // select the host with least containers:         Collections.sort(raList, ResourceComparator.AMOUNTOFCONTAINERASC);


        //migration:
        // mark the host to be shutdown
        // start containes on new host
        // kill container on old host
        // mark host to be shutdown
        //kill host after 2 * graceperiod

        //TODO implement shutdownproperty for dockerhost




        for (String operator : topologyMgmt.getOperatorsAsList()) {
            ScalingAction action = monitor.analyze(operator, infrastructureHost);

            DockerContainer dc = new DockerContainer();
            dc.setCpuCores(operatorCPU);
            dc.setStorage(operatorStorage);
            dc.setRam(operatorRAM);

            if (action.equals(ScalingAction.SCALEDOWN)) {
                pcm.scaleDown(operator);
                LOG.info("VISP - Scale DOWN " + operator);
            }

            if (action.equals(ScalingAction.SCALEUP)) {
                pcm.scaleup(operator, selectSuitableDockerHost(dc), infrastructureHost);
                LOG.info("VISP - Scale UP " + operator);
            }

        }
        LOG.info("VISP - Finished Reasoner");
    }

    public synchronized String selectSuitableDockerHost(DockerContainer dc) {
        List<ResourceAvailability> freeResources = calculateFreeResources();

        String host = null;
        //host = equalDistributionStrategy(dc, freeResources);
        host = binPackingStrategy(dc, freeResources);

        if (host == null) {
            //Scale up docker hosts
            return omgmt.startVM("dockerhost");
        } else {
            return host;
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
            LOG.info("Host found: " + ra.getUrl());
            LOG.info("###### select suitable container for ######");
            return ra.getUrl();
        }
        LOG.info("No suitable host found.");
        LOG.info("###### select suitable container for ######");
        return null;
    }

    private List<ResourceAvailability> calculateFreeResources() {
        Map<String, ResourceAvailability> hostResourceUsage = new HashMap<>();
        List<ResourceAvailability> freeResources = new ArrayList<>();

        for (DockerHost dh : dhr.findAll()) {
            ResourceAvailability rc = new ResourceAvailability(dh.getUrl(), 0, 0.0, 0, 0, "dockercontainer");
            hostResourceUsage.put(dh.getUrl(), rc);
        }

        //collect current usage of cloud resources
        for (DockerContainer dc : dcr.findAll()) {
                ResourceAvailability rc = hostResourceUsage.get(dc.getHost());
                rc.setAmountOfContainer(rc.getAmountOfContainer() + 1);
                rc.setCpuCores(rc.getCpuCores() + dc.getCpuCores());
                rc.setRam(rc.getRam() + rc.getRam());
                rc.setStorage(rc.getStorage() + rc.getStorage());
                rc.setUrl(rc.getUrl());
                hostResourceUsage.put(dc.getHost(), rc);
        }

        //calculate how much resources are left on a specific host


        for (Map.Entry<String, ResourceAvailability> entry : hostResourceUsage.entrySet()) {
            String url = entry.getKey();
            ResourceAvailability usage = entry.getValue();
            DockerHost dh = dhr.findByUrl(url).get(0);

            //omit all dockerhosts which are scheduled for shutdown
            if (dh.getScheduledForShutdown()) {
                LOG.info("omitted host: " + dh.getUrl() + "for scheduling, since it is scheduled to shut down.");
                continue;
            }

            ResourceAvailability availability = new ResourceAvailability();
            availability.setHostId(dh.getHostid());
            availability.setUrl(dh.getUrl());
            availability.setAmountOfContainer(usage.getAmountOfContainer());
            availability.setCpuCores(dh.getCores()-usage.getCpuCores());
            availability.setRam(dh.getRam()-usage.getRam());
            availability.setStorage(dh.getStorage()-usage.getStorage());
            freeResources.add(availability);

        }

        LOG.info("###### free resources ######");
        for (ResourceAvailability ra : freeResources) {
            LOG.info(ra.getHostId() + " - Container: " + ra.getAmountOfContainer() + " - CPU: " + ra.getCpuCores() + " - RAM: " + ra.getRam() + " - Storage: " + ra.getStorage());
        }
        LOG.info("###### free resources ######");

        return freeResources;
    }
}
