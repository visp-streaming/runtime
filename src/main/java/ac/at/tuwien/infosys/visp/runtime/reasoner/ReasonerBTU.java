package ac.at.tuwien.infosys.visp.runtime.reasoner;

import ac.at.tuwien.infosys.visp.common.operators.Operator;
import ac.at.tuwien.infosys.visp.runtime.configuration.Configurationprovider;
import ac.at.tuwien.infosys.visp.runtime.configuration.OperatorConfigurationBootstrap;
import ac.at.tuwien.infosys.visp.runtime.datasources.DockerContainerRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.DockerHostRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.ScalingActivityRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.DockerContainer;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.DockerHost;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.ScalingActivity;
import ac.at.tuwien.infosys.visp.runtime.entities.ResourceAvailability;
import ac.at.tuwien.infosys.visp.runtime.entities.ResourceComparator;
import ac.at.tuwien.infosys.visp.runtime.entities.ScalingAction;
import ac.at.tuwien.infosys.visp.runtime.monitoring.AvailabilityWatchdog;
import ac.at.tuwien.infosys.visp.runtime.monitoring.Monitor;
import ac.at.tuwien.infosys.visp.runtime.resourceManagement.ProcessingNodeManagement;
import ac.at.tuwien.infosys.visp.runtime.resourceManagement.ResourceProvider;
import ac.at.tuwien.infosys.visp.runtime.topology.TopologyManagement;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;


@Service
@DependsOn({"configurationprovider","resourceProvider"})
public class ReasonerBTU {

    @Autowired
    private TopologyManagement topologyMgmt;

    @Autowired
    private ResourceProvider resourceProvider;

    @Autowired
    private OperatorConfigurationBootstrap opConfig;

    @Autowired
    private ProcessingNodeManagement pcm;

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

    @Autowired
    private Configurationprovider config;

    @Value("${visp.shutdown.graceperiod}")
    private Integer graceperiod;

    @Value("${visp.btu}")
    private Integer btu;

    @Autowired
    private ScalingActivityRepository sar;

    private static final Logger LOG = LoggerFactory.getLogger(ReasonerBTU.class);

    @Scheduled(fixedRateString = "${visp.reasoning.timespan}")
    public synchronized void updateResourceconfiguration() {
        if (!config.getReasoner().equals("btu")) {
            return;
        }

        availabilityWatchdog.checkAvailablitiyOfContainer();

        pcm.removeContainerWhichAreFlaggedToShutdown();

        for (String key : resourceProvider.getResourceProviders().keySet()) {
            resourceProvider.get(key).removeHostsWhichAreFlaggedToShutdown();
        }

        LOG.info("VISP - Start Reasoner");

        LOG.info("VISP - Start check if any Hosts need to be shut down");

        /////////////////////
        // Check whether any hosts reach the end of their BTU (within the last 5 % of their BTU)
        /////////////////////

        /**
         * TODO:
         * - simulate downscaling
         *   + check for minimal configuration for topology
         *   + check if there are enough downscaling possibilities according to the utility fkt
         *   + provide simulation for utility fkt to evaluate suitable values
         */

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

                        Operator operator = topologyMgmt.getOperatorByIdentifier(dc.getOperatorType());

                        if (operator==null) {
                            LOG.error("Operator is null");
                        }

                        if (operator.getName()==null) {
                            LOG.error("Operator is null");
                        }


                        DockerHost selectedHost = selectSuitableDockerHost(operator, dh);
                        if (selectedHost.equals(dh)) {
                                LOG.info("the host " + dh.getName() + " could not be scaled down, since the container could not be migrated.");
                                dh.setBTUend((btuEnd.plusSeconds(btu)));
                                dhr.save(dh);
                                sar.save(new ScalingActivity("host", new DateTime(DateTimeZone.UTC), "", "prolongLease", dh.getName()));
                                LOG.info("the host: " + dh.getName() + " was leased for another BTU");
                                return;
                        } else {
                            //Migrate container
                            Operator op = topologyMgmt.getOperatorByIdentifier(dc.getOperatorName());
                            if (pcm.scaleup(selectSuitableDockerHost(op, dh), op)) {
                                pcm.triggerShutdown(dc);
                                sar.save(new ScalingActivity("container", new DateTime(DateTimeZone.UTC), dc.getOperatorType(), "migration", dc.getHost()));
                            }
                        }
                    }

                    resourceProvider.get(dh.getResourcepool()).markHostForRemoval(dh);
                }
            }
        }


        ////////////////////


        LOG.info("VISP - Start container scaling");


        for (Operator op : topologyMgmt.getOperatorsForAConcreteLocation(config.getRuntimeIP())) {
            ScalingAction action = monitor.analyze(op);

            if (action.equals(ScalingAction.SCALEUP)) {
                //TODO consider critical path of topology for scaling down and up

                pcm.scaleup(selectSuitableDockerHost(op, null), op);
            }
        }
        LOG.info("VISP - Finished container scaling");

        LOG.info("VISP - Finished Reasoner");
    }



    public synchronized DockerHost selectSuitableDockerHost(Operator op, DockerHost blackListedHost) {

        if (op.getName()==null) {
            LOG.error("op name = null");
        }

        DockerContainer dc = opConfig.createDockerContainerConfiguration(op);
        DockerHost host = reasonerUtility.selectSuitableHostforContainer(dc, blackListedHost);

        if (host == null) {
            String scaledownoperator = reasonerUtility.selectOperatorTobeScaledDown();
            while (scaledownoperator != null) {
                //TODO user operator name for scaledown
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

            return resourceProvider.get(op.getConcreteLocation().getResourcePool()).startVM(null);
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
