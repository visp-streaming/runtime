package at.tuwien.infosys;

import at.tuwien.infosys.processingNodeDeployment.ProcessingNodeManagement;
import at.tuwien.infosys.reasoner.Reasoner;
import at.tuwien.infosys.topology.TopologyManagement;
import com.spotify.docker.client.DockerException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = VispApplication.class)

public class SetupCleanupTests {

    private String dockerHost = "http://128.130.172.224:2375";
    private String infrastructureHost = "128.130.172.225";

    @Autowired
    private Reasoner reasoner;

    @Autowired
    private TopologyManagement tmgmt;

    @Autowired
    private ProcessingNodeManagement pcm;

    @Test
    public void setupTopology() {
        tmgmt.createMapping(infrastructureHost);
    }

    @Test
    public void cleanupTopology() {
        tmgmt.cleanup(infrastructureHost);
    }

    @Test
    public void initializeDockerContainer() {
        reasoner.setup();
    }

    @Test
    public void cleanupDockerContainer() throws DockerException, InterruptedException {
        pcm.cleanup();
    }


}
