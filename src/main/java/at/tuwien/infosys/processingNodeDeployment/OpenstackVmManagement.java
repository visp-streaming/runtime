package at.tuwien.infosys.processingNodeDeployment;


import at.tuwien.infosys.datasources.DockerHostRepository;
import at.tuwien.infosys.entities.DockerHost;
import org.openstack4j.api.Builders;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.*;
import org.openstack4j.openstack.OSFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class OpenstackVmManagement {

    private static final Logger LOG = LoggerFactory.getLogger(OpenstackVmManagement.class);

    private String OPENSTACK_AUTH_URL;
    private String OPENSTACK_USERNAME;
    private String OPENSTACK_PASSWORD;
    private String OPENSTACK_TENANT_NAME;

    private OSClient os;

    @Autowired
    private DockerHostRepository dhr;

    public void setup() {
        OPENSTACK_AUTH_URL = System.getenv("OS_AUTH_URL");
        OPENSTACK_USERNAME = System.getenv("OS_USERNAME");
        OPENSTACK_PASSWORD = System.getenv("OS_PASSWORD");
        OPENSTACK_TENANT_NAME = System.getenv("OS_TENANT_NAME");

        os = OSFactory.builder()
                .endpoint(OPENSTACK_AUTH_URL)
                .credentials(OPENSTACK_USERNAME,OPENSTACK_PASSWORD)
                .tenantName(OPENSTACK_TENANT_NAME)
                .authenticate();
        LOG.info("Successfully connected to " + OPENSTACK_AUTH_URL + " on tenant " + OPENSTACK_TENANT_NAME + " with user " + OPENSTACK_USERNAME);
    }

    public String startVM(String name, String flavorName, String imageId)  {
        setup();
        List<? extends Flavor> flavors = os.compute().flavors().list();
        Flavor flavor = null;
        Image image = null;

        for(Flavor f : flavors)  {
            if (f.getName().equals(flavorName)) {
                flavor = f;
                break;
            }
        }

        for (Image img : os.compute().images().list()) {
            if (img.getId().trim().equals(imageId)) {
                image = img;
                break;
            }
        }

        ServerCreate sc = Builders.server().name(name).flavor(flavor).image(image).build();
        Server server = os.compute().servers().bootAndWaitActive(sc, 120000);

        LOG.info("Server with id: " + server.getId() + " was started.");



        DockerHost dh = new DockerHost();
        //TODO set actual cores and ram
        dh.setCores(3);
        dh.setRam(5120);
        dh.setUrl(server.getAccessIPv4());
        dh.setHostid(server.getId());

        dhr.save(dh);

        return server.getId();
    }

    public void stopVM(String serverId) {
        setup();

        os.compute().servers().delete(serverId);
        DockerHost dh = dhr.findByHostid(serverId).get(0);
        dhr.delete(dh);

        LOG.info("Server with id: " + serverId + " was stopped.");
    }

    //TODO use in monitor
    public Map<String, ? extends Number> getDiagnostics(String serverId) {
        setup();
        return os.compute().servers().diagnostics(serverId);
    }

    public String getAddress(String serverId){
        Server server = os.compute().servers().get(serverId);
        for (Map.Entry<String, List<? extends Address>> entry : server.getAddresses().getAddresses().entrySet()) {
            for (Address address : entry.getValue()) {
                return address.getAddr();
            }
        }
        return null;
    }


    public OSClient getOs() {
        setup();
        return os;
    }
}
