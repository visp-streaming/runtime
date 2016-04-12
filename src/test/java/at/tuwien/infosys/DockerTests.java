package at.tuwien.infosys;

import at.tuwien.infosys.datasources.DockerContainerRepository;
import at.tuwien.infosys.entities.DockerContainer;
import at.tuwien.infosys.entities.DockerHost;
import at.tuwien.infosys.resourceManagement.DockerContainerManagement;
import at.tuwien.infosys.resourceManagement.ProcessingNodeManagement;
import at.tuwien.infosys.utility.Utilities;
import com.spotify.docker.client.DockerException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = VISPRuntime.class)
public class DockerTests {

    @Autowired
    private DockerContainerManagement dcm;

    @Autowired
    private ProcessingNodeManagement pcm;

    @Autowired
    private DockerContainerRepository dcr;

    @Autowired
    private Utilities utility;

    private DockerHost dh;
    private DockerContainer dc;

    private static final Logger LOG = LoggerFactory.getLogger(DockerTests.class);

    @Before
    public void setup() {
        dh = new DockerHost("host");
        dh.setUrl("128.130.172.206");
        dc = new DockerContainer("monitor", 0.5, 300, 1);
    }


    @Ignore
    @Test
    public void startupContainer() throws DockerException, InterruptedException {
        DockerContainer dc = new DockerContainer("monitor", 0.5, 300, 1);
        dcm.startContainer(dh, dc, "http://128.130.172.225");
    }

    @Ignore
    @Test
    public void initializeTopology() throws DockerException, InterruptedException {
        utility.initializeTopology(dh, "http://128.130.172.225");
    }

    @Test
    public void cleanupImages() throws DockerException, InterruptedException {
        utility.cleanupContainer();
    }

    @Test
    public void scalingTest() {
        pcm.scaleup(dc, dh, "http://128.130.172.225");
        for (DockerContainer dc : dcr.findByOperator("speed")) {
            LOG.info(dc.toString());
        }
        pcm.scaleDown("speed");
        for (DockerContainer dc : dcr.findByOperator("speed")) {
            LOG.info(dc.toString());
        }
    }

    //TODO test actual scaledown with wait until the container is really gone
    @Test
    public void scaledown() {
        pcm.scaleDown("monitor");
    }

}
