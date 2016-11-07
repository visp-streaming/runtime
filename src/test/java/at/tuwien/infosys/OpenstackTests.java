package at.tuwien.infosys;


import at.tuwien.infosys.entities.DockerHost;
import at.tuwien.infosys.resourceManagement.ResourceConnector;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@TestPropertySource(locations="classpath:application.properties")
public class OpenstackTests {

    @Autowired
    private ResourceConnector openstackConnector;


    @Test
    public void startnewVM() {
        DockerHost dh = new DockerHost("deploydockerhost");
        dh.setFlavour("m2.medium");

        dh = openstackConnector.startVM(dh);
        Assert.assertNotNull(dh.getUrl());
    }


}
