package at.tuwien.infosys;

import at.tuwien.infosys.datasources.PooledVMRepository;
import at.tuwien.infosys.datasources.entities.DockerHost;
import at.tuwien.infosys.datasources.entities.PooledVM;
import at.tuwien.infosys.resourceManagement.OpenstackConnector;
import at.tuwien.infosys.topology.TopologyManagement;
import at.tuwien.infosys.utility.Utilities;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@TestPropertySource(locations = "classpath:application.properties")


public class SetupCleanup {
    @Value("${visp.infrastructurehost}")
    private String infrastructureHost;

    @Autowired
    private Utilities utility;

    @Autowired
    private TopologyManagement tmgmt;

    @Autowired
    private OpenstackConnector opc;

    @Autowired
    private PooledVMRepository pvmr;

    @Test
    public void setupTopology() {
        tmgmt.createMapping(infrastructureHost);
    }

    @Test
    public void cleanupTopology() {
        tmgmt.cleanup(infrastructureHost);
    }

    @Test
    public void initializeEvaluation() {
        tmgmt.cleanup(infrastructureHost);
        utility.createInitialStatus();
    }

    @Test
    public void startVMs() {
        initializeVMs(3, "openstackPool");
    }

    private void initializeVMs(Integer amount, String ressourcePoolName) {
        for (int i = 0; i < amount; i++) {
            //TODO make the actual specifications parametizable and also consider single pools such as docker swarm
            DockerHost dh = new DockerHost("dockerhost");
            dh.setFlavour("m2.medium");

            dh = opc.startVM(dh);
            PooledVM pvm = new PooledVM();
            pvm.setName(ressourcePoolName + "-" + dh.getName());
            pvm.setUrl(dh.getUrl());
            pvm.setCores(dh.getCores());
            pvm.setMemory(dh.getMemory());
            pvm.setStorage(dh.getStorage());
            pvm.setFlavour(dh.getFlavour());
            pvmr.save(pvm);
        }
    }
}
