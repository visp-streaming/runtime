package at.tuwien.infosys;


import at.tuwien.infosys.processingNodeDeployment.OpenstackConnector;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.FloatingIP;
import org.openstack4j.model.compute.Server;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = VispApplication.class)
@TestPropertySource(locations="classpath:test.properties")
public class OpenstackTests {

    @Autowired
    private OpenstackConnector openstackConnector;


    @Test
    public void showFloatingIPs() {
        OSClient os = openstackConnector.getOs();
        List<FloatingIP> ips = (List<FloatingIP>) os.compute().floatingIps().list();
        for (FloatingIP ip : ips) {
            System.out.println(ip.getFixedIpAddress() + " - " + ip.getFloatingIpAddress() + " - " + ip.getInstanceId());
        }
    }


    @Test
    public void startnewVM() {
        String server = openstackConnector.startVM("testCaseVM");
        Assert.assertNotNull(server);
    }


//    @After
    public void cleanup() {
        OSClient os = openstackConnector.getOs();
        for (Server server : os.compute().servers().list()) {
            if (server.getName().equals("testCaseVM")) {
                os.compute().servers().delete(server.getId());
            }
        }
    }

}
