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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Service
public class OpenstackVmManagement {

    @Value("${visp.dockerhost.image}")
    private String dockerhostImage;

    @Value("${visp.dockerhost.flavor}")
    private String dockerhostFlavor;

    @Autowired
    private DockerHostRepository dhr;

    private static final Logger LOG = LoggerFactory.getLogger(OpenstackVmManagement.class);

    private String OPENSTACK_AUTH_URL;
    private String OPENSTACK_USERNAME;
    private String OPENSTACK_PASSWORD;
    private String OPENSTACK_TENANT_NAME;

    private OSClient os;

    public void setup() {

        Properties prop = new Properties();
        try {
            prop.load(getClass().getClassLoader().getResourceAsStream("credential.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        OPENSTACK_AUTH_URL = prop.getProperty("os.auth.url");
        OPENSTACK_USERNAME = prop.getProperty("os.username");
        OPENSTACK_PASSWORD = prop.getProperty("os.password");
        OPENSTACK_TENANT_NAME = prop.getProperty("os.tenant.name");

        os = OSFactory.builder()
                .endpoint(OPENSTACK_AUTH_URL)
                .credentials(OPENSTACK_USERNAME,OPENSTACK_PASSWORD)
                .tenantName(OPENSTACK_TENANT_NAME)
                .authenticate();

        LOG.info("VISP - Successfully connected to " + OPENSTACK_AUTH_URL + " on tenant " + OPENSTACK_TENANT_NAME + " with user " + OPENSTACK_USERNAME);
    }

    public String startVM(String name)  {
        setup();
        List<? extends Flavor> flavors = os.compute().flavors().list();
        Flavor flavor = null;
        Image image = null;

        for(Flavor f : flavors)  {
            if (f.getName().equals(dockerhostFlavor)) {
                flavor = f;
                break;
            }
        }

        for (Image img : os.compute().images().list()) {
            if (img.getId().trim().equals(dockerhostImage)) {
                image = img;
                break;
            }
        }

        ServerCreate sc = Builders.server()
                .name(name)
                .flavor(flavor)
                .image(image)
                .keypairName(OPENSTACK_USERNAME)
                .build();
        Server server = os.compute().servers().bootAndWaitActive(sc, 120000);

        DockerHost dh = new DockerHost();
        dh.setCores(flavor.getVcpus());
        dh.setRam(flavor.getRam());
        dh.setUrl(server.getAccessIPv4());
        dh.setHostid(server.getId());

        dhr.save(dh);

        LOG.info("VISP - Server with id: " + server.getId() + " was started.");

        return server.getId();
    }

    public void stopVM(String serverId) {
        setup();

        os.compute().servers().delete(serverId);
        DockerHost dh = dhr.findByHostid(serverId).get(0);
        dhr.delete(dh);

        LOG.info("VISP - Server with id: " + serverId + " was stopped.");
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

    //TODO use in monitor
    public Map<String, ? extends Number> getDiagnostics(String serverId) {
        setup();
        return os.compute().servers().diagnostics(serverId);
    }
}
