package at.tuwien.infosys.reasoner;

import at.tuwien.infosys.configuration.OperatorConfiguration;
import at.tuwien.infosys.datasources.DockerContainerRepository;
import at.tuwien.infosys.datasources.DockerHostRepository;
import at.tuwien.infosys.datasources.ScalingActivityRepository;
import at.tuwien.infosys.entities.*;
import at.tuwien.infosys.monitoring.AvailabilityWatchdog;
import at.tuwien.infosys.monitoring.Monitor;
import at.tuwien.infosys.resourceManagement.ProcessingNodeManagement;
import at.tuwien.infosys.resourceManagement.ResourceProvider;
import at.tuwien.infosys.topology.TopologyManagement;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;

@Service
public class ReasonerPeerJ {

    @Autowired
    TopologyManagement topologyMgmt;

    @Autowired
    ResourceProvider resourceProvider;

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

    @Value("${visp.reasoner}")
    private String reasoner;

    @Autowired
    private ScalingActivityRepository sar;

    private static final Logger LOG = LoggerFactory.getLogger(ReasonerPeerJ.class);

    private String RESOURCEPOOL = "";

    @PostConstruct
    public void init() {
        //get first resourcepool
        RESOURCEPOOL = resourceProvider.getResourceProviders().entrySet().iterator().next().getKey();
    }

    @Scheduled(fixedRateString = "${visp.reasoning.timespan}")
    public synchronized void updateResourceconfiguration() {

        if (!reasoner.equals("peerj")) {
            return;
        }

        availabilityWatchdog.checkAvailablitiyOfContainer();

        pcm.removeContainerWhichAreFlaggedToShutdown();
        resourceProvider.get(RESOURCEPOOL).removeHostsWhichAreFlaggedToShutdown();

        LOG.info("VISP - Start Reasoner");

        LOG.info("VISP - Start check if any Hosts need to be shut down");

        /////////////////////
        // Check whether any hosts reach the end of their BTU (within the last 5 % of their BTU)
        /////////////////////


        if (dhr.count() > 1) {

            for (DockerHost dh : dhr.findAll()) {
                if (dh.getScheduledForShutdown()) {
                    continue;
                }
                DateTime btuEnd = new DateTime(dh.getBTUend());
                DateTime potentialHostTerminationTime = new DateTime(DateTimeZone.UTC);

                //ensure that the host has enough time to shut down
                Integer remainingfivepercent = (int) (btu * 0.05);
                if (remainingfivepercent < graceperiod * 2) {
                    remainingfivepercent = graceperiod * 2;
                }

                potentialHostTerminationTime = potentialHostTerminationTime.plusSeconds(remainingfivepercent);

                //BTU would end within 5 %
                if (btuEnd.isBefore(potentialHostTerminationTime)) {
                    LOG.info(dh.getName() + " arrives at the end of its BTU - initializing check scaledown procedure");

                    List<DockerContainer> containerToMigrate = dcr.findByHost(dh.getName());
                    LOG.info(dh.getName() + " has " + containerToMigrate.size() + " containers which need to be migrated.");

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
                                LOG.info("the host " + dh.getName() + " could not be scaled down, since the container could not be migrated.");
                                dh.setBTUend((btuEnd.plusSeconds(btu)));
                                dhr.save(dh);
                                sar.save(new ScalingActivity("host", new DateTime(DateTimeZone.UTC), "", "prolongLease", dh.getName()));
                                LOG.info("the host: " + dh.getName() + " was leased for another BTU");
                                return;
                        } else {
                            pcm.triggerShutdown(dc);
                            pcm.scaleup(dc, selectSuitableDockerHost(dc, dh), infrastructureHost);
                            sar.save(new ScalingActivity("container", new DateTime(DateTimeZone.UTC), dc.getOperator(), "migration", dc.getHost()));
                        }
                    }
                    resourceProvider.get(RESOURCEPOOL).markHostForRemoval(dh);
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

        LOG.info("VISP - Finished Reasoner");
    }

    public synchronized DockerHost selectSuitableDockerHost(DockerContainer dc, DockerHost blackListedHost) {

        DockerHost host = reasonerUtility.selectSuitableHostforContainer(dc, blackListedHost);

        if (host == null) {

            String scaledownoperator = reasonerUtility.selectOperatorTobeScaledDown();
            while (scaledownoperator != null) {
                pcm.scaleDown(scaledownoperator);
                host = reasonerUtility.selectSuitableHostforContainer(dc, blackListedHost);
                if (host != null) {
                    break;
                }
                scaledownoperator = reasonerUtility.selectOperatorTobeScaledDown();
            }

            if (blackListedHost != null) {
                return blackListedHost;
            }

            //TODO move this somewhere else - reasoner should not be in charge of deciding the size of the VM
            DockerHost dh = new DockerHost("additionaldockerhost");
            dh.setFlavour("m2.medium");
            return resourceProvider.get(RESOURCEPOOL).startVM(dh);
        } else {
            return host;
        }
    }

    private DockerHost equalDistributionStrategy(DockerContainer dc, List<ResourceAvailability> freeResources) {
        //use all hosts equally
        //select the host with most free CPU resources
        Collections.sort(freeResources, ResourceComparator.FREECPUCORESASC);
        return selectFirstFitForResources(dc, freeResources);
    }

    private DockerHost binPackingStrategy(DockerContainer dc, List<ResourceAvailability> freeResources) {
        //minimize the hosts
        //select the host with least free CPU resources
        Collections.sort(freeResources, ResourceComparator.FREECPUCORESDESC);
        return selectFirstFitForResources(dc, freeResources);
    }

    private DockerHost selectFirstFitForResources(DockerContainer dc, List<ResourceAvailability> freeResources) {
        LOG.info("###### select suitable container for: ######");
        LOG.info("Containerspecs: CPU: " + dc.getCpuCores() + " - RAM: " + dc.getMemory() + " - Storage: " + dc.getStorage());
        for (ResourceAvailability ra : freeResources) {
            if (ra.getCpuCores() <= dc.getCpuCores()) {
                continue;
            }
            if (ra.getMemory() <= dc.getMemory()) {
                continue;
            }
            if (ra.getStorage() <= dc.getStorage()) {
                continue;
            }
            LOG.info("Host found: " + ra.getHost().getName());
            LOG.info("###### select suitable container for ######");
            return ra.getHost();
        }
        LOG.info("No suitable host found.");
        LOG.info("###### select suitable container for ######");
        return null;
    }

}
