package at.tuwien.infosys.reasoner;

import at.tuwien.infosys.configuration.OperatorConfiguration;
import at.tuwien.infosys.datasources.DockerContainerRepository;
import at.tuwien.infosys.datasources.DockerHostRepository;
import at.tuwien.infosys.datasources.ScalingActivityRepository;
import at.tuwien.infosys.entities.DockerContainer;
import at.tuwien.infosys.entities.DockerHost;
import at.tuwien.infosys.entities.ScalingAction;
import at.tuwien.infosys.monitoring.AvailabilityWatchdog;
import at.tuwien.infosys.monitoring.Monitor;
import at.tuwien.infosys.resourceManagement.ProcessingNodeManagement;
import at.tuwien.infosys.resourceManagement.ResourceProvider;
import at.tuwien.infosys.topology.TopologyManagement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class ReasonerBasic {

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

    @Autowired
    private ScalingActivityRepository sar;

    private static final Logger LOG = LoggerFactory.getLogger(ReasonerBasic.class);

    @Scheduled(fixedRateString = "${visp.reasoning.timespan}")
    public synchronized void updateResourceconfiguration() {
        availabilityWatchdog.checkAvailablitiyOfContainer();
        pcm.removeContainerWhichAreFlaggedToShutdown();
        resourceProvider.get().removeHostsWhichAreFlaggedToShutdown();

        LOG.info("VISP - Start Reasoner");

        LOG.info("VISP - Start check if any Hosts need to be shut down");

        if (dhr.count() > 1) {

            for (DockerHost dh : dhr.findAll()) {

                if (dcr.findByHost(dh.getName()).size()<1) {
                    resourceProvider.get().markHostForRemoval(dh);
                }
            }
        }

        LOG.info("VISP - Start Container scaleup ");


        for (String operator : topologyMgmt.getOperatorsAsList()) {
            ScalingAction action = monitor.analyze(operator, infrastructureHost);

            if (action.equals(ScalingAction.SCALEUP)) {
                DockerContainer dc = opConfig.createDockerContainerConfiguration(operator);
                pcm.scaleup(dc, selectSuitableDockerHost(dc, null), infrastructureHost);
            }
        }

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

            DockerHost dh = new DockerHost("additionaldockerhost");
            dh.setFlavour("m2.medium");
            return resourceProvider.get().startVM(dh);
        } else {
            return host;
        }
    }

}
