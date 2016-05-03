package at.tuwien.infosys;


import at.tuwien.infosys.entities.DockerHost;
import at.tuwien.infosys.resourceManagement.OpenstackConnector;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = VISPRuntime.class)
@TestPropertySource(locations="classpath:test.properties")
public class OpenstackTests {

    @Autowired
    private OpenstackConnector openstackConnector;


    @Test
    public void startnewVM() {
        DockerHost dh = new DockerHost("deploydockerhost");
        dh.setFlavour("m2.medium");

        dh = openstackConnector.startVM(dh);
        Assert.assertNotNull(dh.getUrl());
    }


}
