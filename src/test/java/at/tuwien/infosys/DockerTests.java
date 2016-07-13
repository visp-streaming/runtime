package at.tuwien.infosys;

import at.tuwien.infosys.configuration.OperatorConfiguration;
import at.tuwien.infosys.datasources.DockerContainerRepository;
import at.tuwien.infosys.datasources.DockerHostRepository;
import at.tuwien.infosys.entities.DockerContainer;
import at.tuwien.infosys.entities.DockerHost;
import at.tuwien.infosys.resourceManagement.DockerContainerManagement;
import at.tuwien.infosys.resourceManagement.OpenstackConnector;
import at.tuwien.infosys.resourceManagement.ProcessingNodeManagement;
import at.tuwien.infosys.utility.Utilities;
import com.spotify.docker.client.exceptions.DockerException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

import static org.junit.Assert.assertEquals;

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
    private DockerHostRepository dhr;

    @Autowired
    private OpenstackConnector opm;

    @Autowired
    private OperatorConfiguration opConf;

    @Autowired
    private Utilities utility;

    private DockerHost dh;
    private DockerContainer dc;

    private static final Logger LOG = LoggerFactory.getLogger(DockerTests.class);

    @Before
    public void setup() {
        dh = new DockerHost("testhost");
        dh.setFlavour("m2.medium");
        dh.setUrl("128.130.172.220");
        dc = opConf.createDockerContainerConfiguration("step1");
    }


    @Test
    public void startupContainer() throws DockerException, InterruptedException {
        dcm.startContainer(dh, dc, "128.130.172.225");
    }

    @Test
    public void scaledownContainer() throws DockerException, InterruptedException {
        pcm.scaleDown("step1");
    }

    @Ignore
    @Test
    public void initializeTopology() throws DockerException, InterruptedException {
        utility.initializeTopology(dh, "128.130.172.225");
    }


    @Test
    public void scalingTest() {
        pcm.scaleup(dc, dh, "128.130.172.225");
        for (DockerContainer dc : dcr.findByOperator("speed")) {
            LOG.info(dc.toString());
        }
        pcm.scaleDown("speed");
        for (DockerContainer dc : dcr.findByOperator("speed")) {
            LOG.info(dc.toString());
        }
    }

    @Test
    public void scaledown() throws InterruptedException {
        dh = opm.startVM(dh);
        pcm.scaleup(opConf.createDockerContainerConfiguration("step1"), dh, "128.130.172.225");
        pcm.scaleup(opConf.createDockerContainerConfiguration("step1"), dh, "128.130.172.225");
        pcm.scaleup(opConf.createDockerContainerConfiguration("step1"), dh, "128.130.172.225");

        List<DockerContainer> containers = dcr.findByOperator("step1");
        assertEquals(containers.size(), 3);

        pcm.scaleDown("step1");

        Boolean scaledownFinished = false;


        while (!scaledownFinished) {

            Thread.sleep(10000);

            containers = dcr.findByOperator("step1");


            if (containers.size()>2) {
                continue;
            } else {
                scaledownFinished = true;
            }

        }
    }

}
