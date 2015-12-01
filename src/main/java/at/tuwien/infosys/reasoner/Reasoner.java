package at.tuwien.infosys.reasoner;

import at.tuwien.infosys.datasources.DockerHostRepository;
import at.tuwien.infosys.entities.ScalingAction;
import at.tuwien.infosys.monitoring.Monitor;
import at.tuwien.infosys.processingNodeDeployment.OpenstackVmManagement;
import at.tuwien.infosys.processingNodeDeployment.ProcessingNodeManagement;
import at.tuwien.infosys.topology.TopologyManagement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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
    Monitor monitor;

    @Value("${visp.dockerhost.image}")
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

        //TODO consider host scaling also in here
        //TODO implement a host configuration (always maximize the deployment of operators on one host)
        //TODO implement a migraiton mechanism (spawn a new one and scale down the old one)
        //TODO monitor the resource usage of a host

        for (String operator : topologyMgmt.getOperatorsAsList()) {
            ScalingAction action = monitor.analyze(operator, infrastructureHost);

            if (action.equals(ScalingAction.SCALEDOWN)) {
                pcm.scaleDown(operator);
                LOG.info("VISP - Scale DOWN " + operator);
            }

            if (action.equals(ScalingAction.SCALEUP)) {
                pcm.scaleup(operator, selectSuitableDockerHost(), infrastructureHost);
                LOG.info("VISP - Scale UP " + operator);
            }

            if (action.equals(ScalingAction.SCALEUPDOUBLE)) {
                pcm.scaleup(operator, selectSuitableDockerHost(), infrastructureHost);
                pcm.scaleup(operator, selectSuitableDockerHost(), infrastructureHost);
                LOG.info("VISP - Double scale UP " + operator);
            }
        }
        LOG.info("VISP - Finished Reasoner");
    }

    public String selectSuitableDockerHost() {


        //TODO implement me
        return dockerHost;
    }
}
