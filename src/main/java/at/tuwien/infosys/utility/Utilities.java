package at.tuwien.infosys.utility;

import at.tuwien.infosys.configuration.OperatorConfiguration;
import at.tuwien.infosys.datasources.DockerContainerRepository;
import at.tuwien.infosys.datasources.DockerHostRepository;
import at.tuwien.infosys.entities.DockerContainer;
import at.tuwien.infosys.entities.DockerHost;
import at.tuwien.infosys.entities.Operator;
import at.tuwien.infosys.resourceManagement.OpenstackConnector;
import at.tuwien.infosys.resourceManagement.ProcessingNodeManagement;
import at.tuwien.infosys.topology.TopologyManagement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class Utilities {

    @Autowired
    private TopologyManagement topologyMgmt;

    @Autowired
    ProcessingNodeManagement processingNodeManagement;

    @Autowired
    OpenstackConnector openstackConnector;

    @Autowired
    OperatorConfiguration opConfig;

    @Autowired
    private DockerHostRepository dhr;

    @Autowired
    private DockerContainerRepository dcr;

    @Value("${visp.infrastructurehost}")
    private String infrastructureHost;

    private static final Logger LOG = LoggerFactory.getLogger(Utilities.class);

    public void initializeTopology(DockerHost dh, String infrastructureHost) {
            for (Operator op : topologyMgmt.getTopologyAsList()) {
                if (op.getName().equals("source")) {
                    continue;
                }
                DockerContainer dc = opConfig.createDockerContainerConfiguration(op.getName());
                processingNodeManagement.scaleup(dc, dh, infrastructureHost);
            }
    }

    public void createInitialStatus() {
        dhr.deleteAll();
        dcr.deleteAll();
        topologyMgmt.createMapping(infrastructureHost);
        DockerHost dh = new DockerHost("initialhost");
        dh.setFlavour("m2.medium");
        dh = openstackConnector.startVM(dh);

        try {
            Thread.sleep(60000);
        } catch (InterruptedException e) {
            LOG.error("Could not startup initial Host.", e);
        }

        initializeTopology(dh, infrastructureHost);
    }

}
