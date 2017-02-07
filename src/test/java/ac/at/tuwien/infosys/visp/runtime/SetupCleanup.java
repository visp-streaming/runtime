package ac.at.tuwien.infosys.visp.runtime;

import ac.at.tuwien.infosys.visp.runtime.datasources.entities.DockerHost;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.PooledVM;
import ac.at.tuwien.infosys.visp.runtime.topology.TopologyManagement;
import ac.at.tuwien.infosys.visp.runtime.datasources.PooledVMRepository;
import ac.at.tuwien.infosys.visp.runtime.resourceManagement.OpenstackConnector;
import ac.at.tuwien.infosys.visp.runtime.utility.Utilities;
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
            //TODO configure
            pvm.setCost(1.5);
            pvm.setCpuFrequency(2400);
            pvmr.save(pvm);
        }
    }
}
