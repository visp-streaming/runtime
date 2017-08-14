package ac.at.tuwien.infosys.visp.runtime.reasoner;

import java.util.List;
import java.util.Map;

import ac.at.tuwien.infosys.visp.common.operators.Operator;
import ac.at.tuwien.infosys.visp.common.resources.ResourceTriple;
import ac.at.tuwien.infosys.visp.runtime.configuration.Configurationprovider;
import ac.at.tuwien.infosys.visp.runtime.datasources.DockerContainerRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.DockerHostRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.ScalingActivityRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.DockerContainer;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.DockerHost;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.ScalingActivity;
import ac.at.tuwien.infosys.visp.runtime.exceptions.ResourceException;
import ac.at.tuwien.infosys.visp.runtime.monitoring.AvailabilityWatchdog;
import ac.at.tuwien.infosys.visp.runtime.monitoring.Monitor;
import ac.at.tuwien.infosys.visp.runtime.monitoring.entities.ScalingAction;
import ac.at.tuwien.infosys.visp.runtime.resourceManagement.ProcessingNodeManagement;
import ac.at.tuwien.infosys.visp.runtime.resourceManagement.ResourceProvider;
import ac.at.tuwien.infosys.visp.runtime.topology.TopologyManagement;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@DependsOn({"configurationprovider", "resourceProvider"})
public class ReasonerBasic {

    @Autowired
    private TopologyManagement topologyMgmt;

    @Autowired
    private ResourceProvider resourceProvider;

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

    @Autowired
    private ScalingActivityRepository sar;

    private static final Logger LOG = LoggerFactory.getLogger(ReasonerBasic.class);

    /**
     * This Scheduling approach provides a simple threshold based scheduling approach.
     */
    @Scheduled(fixedRateString = "#{@configurationprovider.reasoninginterval}")
    public synchronized void updateResourceconfiguration() {
        if (!config.getReasoner().equals("basic")) {
            return;
        }

        availabilityWatchdog.checkAvailablitiyOfContainer();
        pcm.removeContainerWhichAreFlaggedToShutdown();

        resourceProvider.getResourceProviders().keySet().stream()
                .forEach(i -> resourceProvider.get(i).removeHostsWhichAreFlaggedToShutdown());

        LOG.info("VISP - Start Reasoner");
        LOG.info("VISP - Start check if any Hosts can be shut down");

        if (dhr.count() > 1) {

            for (DockerHost dh : dhr.findAll()) {
                if (dcr.findByHost(dh.getName()).isEmpty()) {
                    resourceProvider.get(dh.getResourcepool()).markHostForRemoval(dh);
                }

                DateTime btuEnd = new DateTime(dh.getBTUend());

                if (btuEnd.isBefore(new DateTime(DateTimeZone.UTC))) {
                    dh.setBTUend((btuEnd.plusSeconds(config.getBtu())));
                    dhr.save(dh);
                    sar.save(new ScalingActivity("host", new DateTime(DateTimeZone.UTC), "", "prolongLease", dh.getName()));
                }
            }
        }

        LOG.info("VISP - Start Container scaleup ");

        for (Operator op : topologyMgmt.getOperatorsForAConcreteLocation(config.getRuntimeIP())) {
            ScalingAction action = monitor.analyzeBasic(op, 250, 20);

            if (action.equals(ScalingAction.SCALEUPDOUBLE)) {
                try {
                    pcm.scaleup(reasonerUtility.selectSuitableDockerHost(op), op);
                    pcm.scaleup(reasonerUtility.selectSuitableDockerHost(op), op);
                } catch (ResourceException e) {
                    LOG.error(e.getMessage());
                }
            }

            if (action.equals(ScalingAction.SCALEUP)) {
                try {
                    pcm.scaleup(reasonerUtility.selectSuitableDockerHost(op), op);
                } catch (ResourceException e) {
                    LOG.error(e.getMessage());
                }
            }

            if (action.equals(ScalingAction.SCALEDOWN)) {

                List<DockerContainer> operators = dcr.findByOperatorNameAndStatus(op.getName(), "running");

                if (operators.size() < 2) {
                    LOG.warn("Could not scale down because only one operator instance is left.");
                    continue;
                }

                Map<DockerHost, ResourceTriple> resources = reasonerUtility.calculateFreeResourcesforHosts(null);

                ResourceTriple mostResources = null;
                DockerContainer loneliestOne = operators.get(0);

                for (DockerContainer dc : operators) {
                    if (mostResources == null) {
                        mostResources = resources.get(dhr.findFirstByName(dc.getHost()));
                        loneliestOne = dc;
                    } else {
                        ResourceTriple currentResources = resources.get(dhr.findFirstByName(dc.getHost()));
                        if ((currentResources.getMemory() > mostResources.getMemory()) &&
                                (currentResources.getCores() > mostResources.getCores())) {
                            mostResources = currentResources;
                            loneliestOne = dc;
                        }
                    }
                }
                pcm.triggerShutdown(loneliestOne);
            }
        }

        LOG.info("VISP - Finished Reasoner");
    }

}
