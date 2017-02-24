package ac.at.tuwien.infosys.visp.runtime.reasoner;

import ac.at.tuwien.infosys.visp.common.operators.Operator;
import ac.at.tuwien.infosys.visp.runtime.datasources.DockerContainerRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.DockerHostRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.DockerHost;
import ac.at.tuwien.infosys.visp.runtime.entities.ScalingAction;
import ac.at.tuwien.infosys.visp.runtime.monitoring.AvailabilityWatchdog;
import ac.at.tuwien.infosys.visp.runtime.monitoring.Monitor;
import ac.at.tuwien.infosys.visp.runtime.resourceManagement.ProcessingNodeManagement;
import ac.at.tuwien.infosys.visp.runtime.resourceManagement.ResourceProvider;
import ac.at.tuwien.infosys.visp.runtime.topology.TopologyManagement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@DependsOn("resourceProvider")
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


    @Value("${visp.reasoner}")
    private String reasoner;

    @Value("${visp.ip}")
    private String vispLocation;

    private static final Logger LOG = LoggerFactory.getLogger(ReasonerBasic.class);

    /**
     * This Scheduling approach provides a simple threshold based scheduling approach.
     */
    @Scheduled(fixedRateString = "${visp.reasoning.timespan}")
    public synchronized void updateResourceconfiguration() {

        if (!reasoner.equals("basic")) {
            return;
        }

        availabilityWatchdog.checkAvailablitiyOfContainer();
        pcm.removeContainerWhichAreFlaggedToShutdown();

        for (String key : resourceProvider.getResourceProviders().keySet()) {
            resourceProvider.get(key).removeHostsWhichAreFlaggedToShutdown();
        }

        LOG.info("VISP - Start Reasoner");
        LOG.info("VISP - Start check if any Hosts need to be shut down");

        if (dhr.count() > 1) {

            for (DockerHost dh : dhr.findAll()) {

                if (dcr.findByHost(dh.getName()).size()<1) {
                    resourceProvider.get(dh.getResourcepool()).markHostForRemoval(dh);
                }
            }
        }

        LOG.info("VISP - Start Container scaleup ");

        for (Operator op : topologyMgmt.getOperatorsForAConcreteLocation(vispLocation)) {
            ScalingAction action = monitor.analyze(op);

            if (action.equals(ScalingAction.SCALEUP)) {
                try {
                    pcm.scaleup(reasonerUtility.selectSuitableDockerHost(op), op);
                } catch (Exception e) {
                    LOG.error(e.getLocalizedMessage());
                }
            }

            if (action.equals(ScalingAction.SCALEDOWN)) {
                pcm.scaleDown(op.getName());
            }
        }

        LOG.info("VISP - Finished Reasoner");
    }

}
