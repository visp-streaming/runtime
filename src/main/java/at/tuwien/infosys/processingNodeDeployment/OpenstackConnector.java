package at.tuwien.infosys.processingNodeDeployment;


import at.tuwien.infosys.datasources.DockerHostRepository;
import at.tuwien.infosys.entities.DockerHost;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.DockerException;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.HostConfig;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
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
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Service
public class OpenstackConnector {

    @Value("${visp.dockerhost.image}")
    private String dockerhostImage;

    @Value("${visp.dockerhost.flavor}")
    private String dockerhostFlavor;

    @Value("${visp.shutdown.graceperiod}")
    private Integer graceperiod;

    @Value("${visp.entropyContainerName}")
    private String entropyContainerName;

    @Autowired
    private DockerHostRepository dhr;

    private static final Logger LOG = LoggerFactory.getLogger(OpenstackConnector.class);

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
            LOG.error("Could not load properties.", e);
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
        Server server = os.compute()
                .servers()
                .bootAndWaitActive(sc, 120000);

        DockerHost dh = new DockerHost();
        dh.setCores(flavor.getVcpus() + 0.0);
        dh.setRam(flavor.getRam());
        dh.setHostid(server.getId());
        dh.setStorage(flavor.getDisk());
        dh.setScheduledForShutdown(false);

        String floatingIP = assignFloatingIP(server);
        String hostUrl = "http://" + floatingIP + ":2375";
        dh.setUrl(hostUrl);

        dhr.save(dh);

        LOG.info("VISP - Server with id: " + server.getId() + " and IP " + floatingIP + " was started.");

        try {
            Thread.sleep(60000);
        } catch (InterruptedException e) {
            LOG.error("Could not start VM", e);
        }

        startupEntropyContainer(hostUrl);

        return hostUrl;
    }

    private void startupEntropyContainer(String dockerHost) {
        final DockerClient docker = DefaultDockerClient.builder().
                uri(URI.create(dockerHost)).
                connectTimeoutMillis(3000000).
                build();

        try {
            docker.pull(entropyContainerName);

            final HostConfig expected = HostConfig.builder()
                    .privileged(true)
                    .build();

            final ContainerConfig containerConfig = ContainerConfig.builder()
                    .image(entropyContainerName)
                    .hostConfig(expected)
                    .build();

        final ContainerCreation creation = docker.createContainer(containerConfig);
        final String id = creation.id();

        docker.startContainer(id);

        } catch (DockerException e) {
            LOG.error("Could not start container", e);
        } catch (InterruptedException e) {
            LOG.error("Could not start container", e);
        }
    }

    private String assignFloatingIP(Server server) {
        List<FloatingIP> ips = (List<FloatingIP>) os.compute().floatingIps().list();
        for (FloatingIP ip : ips) {
            if (ip.getInstanceId() == null) {
                ActionResponse r = os.compute().floatingIps().addFloatingIP(server, ip.getFloatingIpAddress());
                return ip.getFloatingIpAddress();
            }
        }
        return null;
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

    public void markHostForRemoval(String hostId) {
        DockerHost dh = dhr.findByHostid(hostId).get(0);
        dh.setScheduledForShutdown(true);
        dh.setTerminationTime(new DateTime(DateTimeZone.UTC).toString());
        dhr.save(dh);
    }

    public void removeHostsWhichAreFlaggedToShutdown() {
        for (DockerHost dh : dhr.findAll()) {
            DateTime now = new DateTime(DateTimeZone.UTC);
            LOG.info("Housekeeping shuptdown host: current time: " + now + " - " + "termination time:" + new DateTime(dh.getTerminationTime()).plusMinutes(graceperiod * 2));
            if (now.isAfter(new DateTime(dh.getTerminationTime()).plusSeconds(graceperiod * 2))) {
                    stopVM(dh.getHostid());
            }
        }
    }
}
