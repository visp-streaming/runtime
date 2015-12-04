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

    @Value("${visp.dockerhost}")
    private String dockerHost;

    @Value("${visp.infrastructurehost}")
    private String infrastructureHost;

    private static final Logger LOG = LoggerFactory.getLogger(OpenstackVmManagement.class);

    public void setup() {

        topologyMgmt.createMapping(infrastructureHost);
        omgmt.startVM("dockerHost");

        pcm.initializeTopology(dockerHost, infrastructureHost);


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


        //TODO implement cleanup and migration functionality for docker hosts

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

            //TODO consider specs of container for scaling up/down - store them for the operator
            DockerContainer dc = new DockerContainer();

            if (action.equals(ScalingAction.SCALEDOWN)) {
                pcm.scaleDown(operator);
                LOG.info("VISP - Scale DOWN " + operator);
            }

            if (action.equals(ScalingAction.SCALEUP)) {
                pcm.scaleup(operator, selectSuitableDockerHost(dc), infrastructureHost);
                LOG.info("VISP - Scale UP " + operator);
            }

            if (action.equals(ScalingAction.SCALEUPDOUBLE)) {
                pcm.scaleup(operator, selectSuitableDockerHost(dc), infrastructureHost);
                pcm.scaleup(operator, selectSuitableDockerHost(dc), infrastructureHost);
                LOG.info("VISP - Double scale UP " + operator);
            }
        }
        LOG.info("VISP - Finished Reasoner");
    }

    public String selectSuitableDockerHost(DockerContainer dc) {
        List<ResourceAvailability> freeResources = calculateFreeResources();

        String host = null;
        host = equalDistributionStrategy(dc, freeResources);
        //host = binPackingStrategy(dc, freeResources);


        if (host == null) {
            //Scale up docker hosts
            return omgmt.startVM("dockerhost");
        } else {
            return host;
        }

        //TODO set dockercontainer cpu, ram and storage when they are spawned
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
        for (ResourceAvailability ra : freeResources) {
            if (ra.getCpuCores() < dc.getCpuCores()) {
                continue;
            }
            if (ra.getRam() < dc.getRam()) {
                continue;
            }
            if (ra.getStorage() < dc.getStorage()) {
                continue;
            }
            return ra.getHostId();
        }
        return null;
    }

    private List<ResourceAvailability> calculateFreeResources() {
        Map<String, ResourceAvailability> hostResourceUsage = new HashMap<>();
        List<ResourceAvailability> freeResources = new ArrayList<>();

        //collect current usage of cloud resources
        for (DockerContainer dc : dcr.findAll()) {
            if (hostResourceUsage.containsKey(dc.getHost())) {
                ResourceAvailability rc = hostResourceUsage.get(dc.getHost());
                rc.setAmountOfContainer(rc.getAmountOfContainer() + 1);
                rc.setCpuCores(rc.getCpuCores() + dc.getCpuCores());
                rc.setRam(rc.getRam() + rc.getRam());
                rc.setStorage(rc.getStorage() + rc.getStorage());
                hostResourceUsage.put(dc.getHost(), rc);
            } else {
                ResourceAvailability rc = new ResourceAvailability(dc.getHost(), 1, dc.getCpuCores(), dc.getRam(), dc.getStorage());
                hostResourceUsage.put(dc.getHost(), rc);
            }
        }

        //calculate how much resources are left on a specific host


        for (Map.Entry<String, ResourceAvailability> entry : hostResourceUsage.entrySet()) {
            String hostId = entry.getKey();
            ResourceAvailability usage = entry.getValue();
            DockerHost dh = dhr.findByHostid(hostId).get(0);

            //omit all dockerhosts which are scheduled for shutdown
            if (dh.getScheduledForShutdown()) {
                continue;
            }

            ResourceAvailability availability = new ResourceAvailability();
            availability.setAmountOfContainer(usage.getAmountOfContainer());
            availability.setCpuCores(dh.getCores()-usage.getCpuCores());
            availability.setRam(dh.getRam()-usage.getRam());
            availability.setStorage(dh.getStorage()-usage.getStorage());
            freeResources.add(availability);
        }
        return freeResources;
    }
}
