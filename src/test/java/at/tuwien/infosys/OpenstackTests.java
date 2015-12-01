package at.tuwien.infosys;


import at.tuwien.infosys.processingNodeDeployment.OpenstackVmManagement;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.Server;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = VispApplication.class)
public class OpenstackTests {

    @Autowired
    private OpenstackVmManagement openstackVmManagement;



    @Test
    public void startnewVM() {
        String server = openstackVmManagement.startVM("testCaseVM");
        Assert.assertNotNull(server);
    }


//    @After
    public void cleanup() {
        OSClient os = openstackVmManagement.getOs();
        for (Server server : os.compute().servers().list()) {
            if (server.getName().equals("testCaseVM")) {
                os.compute().servers().delete(server.getId());
            }
        }
    }

}
