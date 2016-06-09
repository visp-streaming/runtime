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
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
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

    @Autowired
    private ReasonerUtility reasonerUtility;

    @Value("${visp.shutdown.graceperiod}")
    private Integer graceperiod;

    @Value("${visp.btu}")
    private Integer btu;

    @Value("${visp.infrastructurehost}")
    private String infrastructureHost;

    private static final Logger LOG = LoggerFactory.getLogger(Reasoner.class);

    public synchronized void updateResourceconfiguration() {
        availabilityWatchdog.checkAvailablitiyOfContainer();

        pcm.removeContainerWhichAreFlaggedToShutdown();
        openstackConnector.removeHostsWhichAreFlaggedToShutdown();


        LOG.info("VISP - Start Reasoner");

        LOG.info("VISP - Start check if any Hosts need to be shut down");

        /////////////////////
        // Check whether any hosts reach the end of their BTU (within the last 5 % of their BTU)
        /////////////////////


        for (DockerHost dh : dhr.findAll()) {
            if (dh.getScheduledForShutdown()){
                continue;
            }
            DateTime btuEnd = new DateTime(dh.getBTUend());
            DateTime potentialHostTerminationTime = new DateTime(DateTimeZone.UTC);

            //ensure that the host has enough time to shut down
            Integer remainingfivepercent = (int) (btu * 0.05);
            if (remainingfivepercent < graceperiod*2) {
                remainingfivepercent=graceperiod*2;
            }

            potentialHostTerminationTime = potentialHostTerminationTime.plusSeconds(remainingfivepercent);

            //BTU would end within 5 %
            if (btuEnd.isBefore(potentialHostTerminationTime)) {

                List<DockerContainer> containerToMigrate = dcr.findByHost(dh.getName());

                //migrate Container
                for (DockerContainer dc : containerToMigrate) {
                    if (dc.getStatus() == null) {
                        dc.setStatus("running");
                    }

                    if (dc.getStatus().equals("stopping")) {
                        continue;
                    }

                    DockerHost selectedHost = selectSuitableDockerHost(dc, dh);
                    if (selectedHost.equals(dh)) {

                        Boolean optimization = false;

                        //TODO consider critical path of topology for scaling down and up


                        //TODO gather space requirements for the remaining container which need to be migrated

                        //TODO check whether enough containers can be scaled down to realize the migration


                        if (!optimization) {
                            //Optimization was not possible and VM needs to leased for another BTU

                            dh.setBTUend((btuEnd.plusSeconds(btu)).toString());
                            dhr.save(dh);

                            LOG.info("the host: " + dh.getName() + " was leased for another BTU");
                            break;

                        } else {
                            pcm.triggerShutdown(dc);
                            pcm.scaleup(dc, selectSuitableDockerHost(dc, dh), infrastructureHost);
                        }

                    } else {
                        pcm.triggerShutdown(dc);
                        pcm.scaleup(dc, selectSuitableDockerHost(dc, dh), infrastructureHost);
                    }
                }
            }
        }

        ////////////////////


        LOG.info("VISP - Start container scaling");


        for (String operator : topologyMgmt.getOperatorsAsList()) {
            ScalingAction action = monitor.analyze(operator, infrastructureHost);

            if (action.equals(ScalingAction.SCALEUP)) {
                //TODO consider critical path of topology for scaling down and up
                DockerContainer dc = opConfig.createDockerContainerConfiguration(operator);
                pcm.scaleup(dc, selectSuitableDockerHost(dc, null), infrastructureHost);
            }
        }
        LOG.info("VISP - Finished container scaling");

        List<ResourceAvailability> freeResources = reasonerUtility.calculateFreeResourcesforHosts(null);

        ResourceAvailability aggregatedFreeResources = reasonerUtility.calculateFreeresources(freeResources);

        LOG.info("VISP - Finished Reasoner");
    }

    public synchronized DockerHost selectSuitableDockerHost(DockerContainer dc, DockerHost blackListedHost) {
        List<ResourceAvailability> freeResources = reasonerUtility.calculateFreeResourcesforHosts(blackListedHost);

        //String host = binPackingStrategy(dc, freeResources);
        String host = equalDistributionStrategy(dc, freeResources);

        if (host == null) {
            if (blackListedHost!=null) {
                return blackListedHost;
            }

            //TODO implement scaledown

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


}
