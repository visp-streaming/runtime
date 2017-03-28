package ac.at.tuwien.infosys.visp.runtime.reasoner;

import ac.at.tuwien.infosys.visp.common.operators.Operator;
import ac.at.tuwien.infosys.visp.common.resources.ResourceTriple;
import ac.at.tuwien.infosys.visp.runtime.configuration.Configurationprovider;
import ac.at.tuwien.infosys.visp.runtime.configuration.OperatorConfigurationBootstrap;
import ac.at.tuwien.infosys.visp.runtime.datasources.DockerContainerRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.DockerHostRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.ScalingActivityRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.DockerContainer;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.DockerHost;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.ScalingActivity;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


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

        LOG.info("VISP - Start container scaling");


        for (Operator op : topologyMgmt.getOperatorsForAConcreteLocation(config.getRuntimeIP())) {
            ScalingAction action = monitor.analyze(op);

            if (action.equals(ScalingAction.SCALEUP)) {
                pcm.scaleup(selectSuitableDockerHost(op, null), op);
            }
        }
        LOG.info("VISP - Finished container scaling");

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


                    ResourceTriple migrationRequirements = new ResourceTriple();
                    for (DockerContainer dc : containerToMigrate) {
                        migrationRequirements.incrementCores(dc.getCpuCores());
                        migrationRequirements.incrementMemory(dc.getMemory());
                        migrationRequirements.incrementStorage(Float.valueOf(dc.getStorage()));
                    }

                    Boolean migrationIsPossible = simulateMigration(dh);

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
                            continue;
                        }

                        if (operator.getName()==null) {
                            LOG.error("Operator is null");
                            continue;
                        }

                        DockerHost selectedHost = selectSuitableDockerHost(operator, dh);
                        if (!migrationIsPossible || selectedHost.equals(dh)) {
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

        LOG.info("VISP - Finished Reasoner");
    }


    private Boolean simulateMigration(DockerHost dh) {
        List<DockerContainer> containerMigrateSimulation = dcr.findByHost(dh.getName());
        Map<DockerHost, ResourceTriple> simulationRAS = reasonerUtility.calculateFreeResourcesforHosts(dh);

        List<DockerContainer> migratedSucessful = new ArrayList<>();

        for (DockerContainer dc : containerMigrateSimulation) {
            for (Map.Entry<DockerHost, ResourceTriple> ra : simulationRAS.entrySet()) {
                ResourceTriple raSingle = ra.getValue();

                Double feasibilityThreshold = Math.min(raSingle.getCores() / dc.getCpuCores(), raSingle.getMemory() / dc.getMemory());

                if (feasibilityThreshold < 1) {
                    continue;
                }

                raSingle.decrement(dc.getCpuCores(), dc.getMemory(), Float.valueOf(dc.getStorage()));
                simulationRAS.put(ra.getKey(), raSingle);
                migratedSucessful.add(dc);
            }
        }

        containerMigrateSimulation.removeAll(migratedSucessful);

        if (!containerMigrateSimulation.isEmpty()) {
            //some operators need to be scaled down to migrate the operators
            TreeMap<String, Double> potentialScaledowns = reasonerUtility.selectOperatorTobeScaledDown();
            if (potentialScaledowns == null) {
                return false;
            }
            if (potentialScaledowns.size() < (containerMigrateSimulation.size() + 1)) {
                //migration cannot be performed, since not enough containes can be scaled down; we assume that all containers have a similar size
                return false;
            }
        }
        return true;
    }


    private synchronized DockerHost selectSuitableDockerHost(Operator op, DockerHost blackListedHost) {

        if (op.getName()==null) {
            LOG.error("op name = null");
        }

        DockerContainer dc = opConfig.createDockerContainerConfiguration(op);
        DockerHost host = reasonerUtility.selectSuitableHostforContainer(dc, blackListedHost);

        if (host == null) {
            TreeMap<String, Double> scaledowns = reasonerUtility.selectOperatorTobeScaledDown();

            if (scaledowns == null) {
                return resourceProvider.get(op.getConcreteLocation().getResourcePool()).startVM(null);
            }

            if (scaledowns.isEmpty()) {
                return resourceProvider.get(op.getConcreteLocation().getResourcePool()).startVM(null);
            }

            String scaledownoperator = scaledowns.firstKey();
            while (scaledownoperator != null) {
                //TODO use operator name for scaledown
                pcm.scaleDown(scaledownoperator);
                host = reasonerUtility.selectSuitableHostforContainer(dc, blackListedHost);
                if (host != null) {
                    break;
                }
                scaledownoperator = reasonerUtility.selectOperatorTobeScaledDown().firstKey();
            }

            if (blackListedHost != null) {
                return blackListedHost;
            }

            return resourceProvider.get(op.getConcreteLocation().getResourcePool()).startVM(null);
        } else {
            return host;
        }
    }

}
