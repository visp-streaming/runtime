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
@SpringApplicationConfiguration(classes = VispApplication.class)
@TestPropertySource(locations="classpath:test.properties")
public class OpenstackTests {

    @Autowired
    private OpenstackConnector openstackConnector;

    //TODO // FIXME: 11/04/16
/**
    @Test
    public void showFloatingIPs() {
        OSClient os = openstackConnector.getOs();
        List<FloatingIP> ips = (List<FloatingIP>) os.compute().floatingIps().list();
        for (FloatingIP ip : ips) {
            System.out.println(ip.getFixedIpAddress() + " - " + ip.getFloatingIpAddress() + " - " + ip.getInstanceId());
        }
    }
*/

    @Test
    public void startnewVM() {
        DockerHost dh = new DockerHost("testcasevm");
        dh.setFlavour("m2.medium");

        dh = openstackConnector.startVM(dh);
        Assert.assertNotNull(dh.getUrl());
    }


    //TODO // FIXME: 11/04/16

/**
//    @After
    public void cleanup() {
        OSClient os = openstackConnector.getOs();
        for (Server server : os.compute().servers().list()) {
            if (server.getName().equals("testCaseVM")) {
                os.compute().servers().delete(server.getId());
            }
        }
    }
*/
}
