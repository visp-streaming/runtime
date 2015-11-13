package at.tuwien.infosys.reasoner;

import at.tuwien.infosys.processingNodeDeployment.OpenstackVmManagement;
import at.tuwien.infosys.processingNodeDeployment.ProcessingNodeManagement;
import at.tuwien.infosys.topology.TopologyManagement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class Reasoner {

    @Autowired
    TopologyManagement topologyMgmt;

    @Autowired
    OpenstackVmManagement openstackVmMgmt;

    @Autowired
    ProcessingNodeManagement pcm;

    private String dockerHost = "http://128.130.172.224:2375";
    //TODO split up for different hosts broker, redis, ...
    private String infrastructureHost = "128.130.172.225";



    //TODO initial setup --> realize topology

    //e351847d-1ffc-4566-a16b-17fa80105459  image id on openstack


    public void setup() {

        topologyMgmt.createMapping(infrastructureHost);

        //TODO initial openstack host

        //TODO initial topology structure

    }

}
