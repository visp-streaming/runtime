package at.tuwien.infosys.reasoner;

import at.tuwien.infosys.entities.ScalingAction;
import at.tuwien.infosys.monitoring.Monitor;
import at.tuwien.infosys.processingNodeDeployment.OpenstackVmManagement;
import at.tuwien.infosys.processingNodeDeployment.ProcessingNodeManagement;
import at.tuwien.infosys.topology.TopologyManagement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
    Monitor monitor;

    private static final Logger LOG = LoggerFactory.getLogger(OpenstackVmManagement.class);

    private String dockerHost = "http://128.130.172.224:2375";
    //TODO split up for different hosts broker, redis, ...
    private String infrastructureHost = "128.130.172.225";

    //e351847d-1ffc-4566-a16b-17fa80105459  image id on openstack

    public void setup() {

        topologyMgmt.createMapping(infrastructureHost);

        //start initial Dockerhost
        //omgmt.startVM("dockerHost", "m2.medium", "e351847d-1ffc-4566-a16b-17fa80105459");

        //TODO start infrastructureHost
        //omgmt.startVM("infrastructureHost", "m2.medium", "TODO");

        pcm.initializeTopology(dockerHost, infrastructureHost);


        //TODO implement host management in reasoner

    }

    public void updateResourceconfiguration() {

        //TODO consider host scaling also in here
        //TODO implement a host configuration (always maximize the deployment of operators on one host)
        //TODO implement a migraiton mechanism (spawn a new one and scale down the old one)
        //TODO monitor the resource usage of a host

        for (String operator : topologyMgmt.getOperatorsAsList()) {
            ScalingAction action = monitor.analyze(operator);

            if (action.equals(ScalingAction.SCALEDOWN)) {
                pcm.scaleDown(operator);
                return;
            }

            if (action.equals(ScalingAction.SCALEUP)) {
                pcm.scaleup(operator, dockerHost, infrastructureHost);
                return;
            }

            if (action.equals(ScalingAction.SCALEUPDOUBLE)) {
                pcm.scaleup(operator, dockerHost, infrastructureHost);
                pcm.scaleup(operator, dockerHost, infrastructureHost);
                return;
            }
        }
        //TODO log activities in a mysql database on the infrastructure host
    }

}
