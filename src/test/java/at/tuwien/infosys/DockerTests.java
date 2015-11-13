package at.tuwien.infosys;

import at.tuwien.infosys.processingNodeDeployment.DockerContainerManagement;
import at.tuwien.infosys.processingNodeDeployment.ProcessingNodeManagement;
import com.spotify.docker.client.DockerException;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = VispApplication.class)
public class DockerTests {

    @Autowired
    private DockerContainerManagement dcm;

    @Autowired
    private ProcessingNodeManagement pcm;

    @Ignore
    @Test
    public void startupContainer() throws DockerException, InterruptedException {
        dcm.startContainer("http://128.130.172.224:2375", "monitor", "http://128.130.172.225");
    }

    @Ignore
    @Test
    public void initializeTopology() throws DockerException, InterruptedException {
        pcm.initializeTopology();
    }

    @Ignore
    @Test
    public void cleanupImages() throws DockerException, InterruptedException {
        pcm.cleanup();
    }

    @Test
    public void scalingTest() {
        pcm.scaleup("speed");
        pcm.scaleDown("speed");
    }

}
