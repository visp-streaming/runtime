package ac.at.tuwien.infosys.visp.runtime.reasoner;

import ac.at.tuwien.infosys.visp.runtime.configuration.OperatorConfigurationBootstrap;
import ac.at.tuwien.infosys.visp.runtime.datasources.DockerContainerRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.DockerHostRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.DockerContainer;
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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
public class ReasonerBasic {

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

    @Value("${visp.shutdown.graceperiod}")
    private Integer graceperiod;

    @Value("${visp.btu}")
    private Integer btu;

    @Value("${visp.infrastructurehost}")
    private String infrastructureHost;

    @Value("${visp.reasoner}")
    private String reasoner;

    private static final Logger LOG = LoggerFactory.getLogger(ReasonerBasic.class);

    private String RESOURCEPOOL = "";

    @PostConstruct
    public void init() {
        //get first resourcepool
        RESOURCEPOOL = resourceProvider.getResourceProviders().entrySet().iterator().next().getKey();
    }

    @Scheduled(fixedRateString = "${visp.reasoning.timespan}")
    public synchronized void updateResourceconfiguration() {

        if (!reasoner.equals("basic")) {
            return;
        }

        availabilityWatchdog.checkAvailablitiyOfContainer();
        pcm.removeContainerWhichAreFlaggedToShutdown();
        resourceProvider.get(RESOURCEPOOL).removeHostsWhichAreFlaggedToShutdown();

        LOG.info("VISP - Start Reasoner");

        LOG.info("VISP - Start check if any Hosts need to be shut down");

        if (dhr.count() > 1) {

            for (DockerHost dh : dhr.findAll()) {

                if (dcr.findByHost(dh.getName()).size()<1) {
                    resourceProvider.get(RESOURCEPOOL).markHostForRemoval(dh);
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

            if (blackListedHost != null) {
                return blackListedHost;
            }

            //TODO move this somewhere else - the algorithm should not decide on the size of the VM
            DockerHost dh = new DockerHost("additionaldockerhost");
            dh.setFlavour("m2.medium");
            return resourceProvider.get(RESOURCEPOOL).startVM(dh);
        } else {
            return host;
        }
    }

}
