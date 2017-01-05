package at.tuwien.infosys.resourceManagement;


import at.tuwien.infosys.datasources.DockerHostRepository;
import at.tuwien.infosys.datasources.ScalingActivityRepository;
import at.tuwien.infosys.entities.DockerHost;
import at.tuwien.infosys.entities.ScalingActivity;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.HostConfig;
import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.internal.util.Base64;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.openstack4j.api.Builders;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.compute.*;
import org.openstack4j.openstack.OSFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

@Service
public class OpenstackConnector implements ResourceConnector {

    @Value("${visp.dockerhost.image}")
    private String dockerhostImage;

    @Value("${visp.shutdown.graceperiod}")
    private Integer graceperiod;

    @Value("${visp.entropyContainerName}")
    private String entropyContainerName;

    @Value("${visp.openstack.publicip}")
    private Boolean PUBLICIPUSAGE;

    @Value("${visp.btu}")
    private Integer BTU;

    @Autowired
    private ScalingActivityRepository sar;

    @Autowired
    private DockerHostRepository dhr;

    private static final Logger LOG = LoggerFactory.getLogger(OpenstackConnector.class);

    private String OPENSTACK_AUTH_URL;
    private String OPENSTACK_USERNAME;
    private String OPENSTACK_PASSWORD;
    private String OPENSTACK_TENANT_NAME;
    private String OPENSTACK_KEYPAIR_NAME;

    private OSClient.OSClientV2 os;

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
        OPENSTACK_KEYPAIR_NAME = prop.getProperty("os.keypair.name");


        os = OSFactory.builderV2()
                .endpoint(OPENSTACK_AUTH_URL)
                .credentials(OPENSTACK_USERNAME,OPENSTACK_PASSWORD)
                .tenantName(OPENSTACK_TENANT_NAME)
                .authenticate();

        LOG.info("VISP - Successfully connected to " + OPENSTACK_AUTH_URL + " on tenant " + OPENSTACK_TENANT_NAME + " with user " + OPENSTACK_USERNAME);
    }


    @Override
    public DockerHost startVM(DockerHost dh) {
        setup();

        String cloudInit = "";
        try {
            cloudInit = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("docker-config/cloud-init"), "UTF-8");
        } catch (IOException e) {
            LOG.error("Could not load cloud init file");
        }

        Flavor flavor = os.compute().flavors().get(dh.getFlavour());

        //TODO check if the flavors can be retrieved in future releases
        for (Flavor f : os.compute().flavors().list()) {
            if (f.getName().equals(dh.getFlavour())) {
                flavor = f;
                break;
            }
        }

        ServerCreate sc = Builders.server()
                .name("dockerhost")
                .flavor(flavor)
                .image(dockerhostImage)
                .userData(Base64.encodeAsString(cloudInit))
                .keypairName(OPENSTACK_KEYPAIR_NAME)
                .addSecurityGroup("default")
                .build();

        Server server = os.compute().servers().boot(sc);

        String uri = server.getAccessIPv4();

        if (PUBLICIPUSAGE) {

            FloatingIP freeIP = null;

            for (FloatingIP ip : os.compute().floatingIps().list()) {
                if (ip.getFixedIpAddress() == null) {
                    freeIP = ip;
                    break;
                }
            }
            if (freeIP == null) {
                freeIP = os.compute().floatingIps().allocateIP("cloud");
            }

            ActionResponse ipresponse = os.compute().floatingIps().addFloatingIP(server, freeIP.getFloatingIpAddress());
            if (!ipresponse.isSuccess()) {
                LOG.error("Dockerhost could not be started", ipresponse.getFault());
            }
            uri = freeIP.getFloatingIpAddress();
        }


        dh.setName(server.getId());
        dh.setUrl(uri);
        dh.setCores(flavor.getVcpus() + 0.0);
        dh.setRam(flavor.getRam());
        //size in GB
        dh.setStorage(flavor.getDisk() * 1024 + 0F);
        dh.setScheduledForShutdown(false);
        DateTime btuEnd = new DateTime(DateTimeZone.UTC);
        btuEnd = btuEnd.plusSeconds(BTU);
        dh.setBTUend(btuEnd);


        LOG.info("VISP - Server with id: " + dh.getName() + " and IP " + uri + " was started.");

        //wait until the dockerhost is available
        Boolean connection = false;
        while (!connection) {
            try {
                TimeUnit.SECONDS.sleep(1);
                final DockerClient docker = DefaultDockerClient.builder().
                        uri(URI.create("http://" + dh.getUrl() + ":2375")).
                        connectTimeoutMillis(3000000).
                        build();
                docker.ping();
                connection = true;
            }  catch (InterruptedException e) {
                LOG.debug("Dockerhost is not available yet.");
            } catch (DockerException e) {
                LOG.debug(e.getMessage());
            }
        }

        dhr.save(dh);
        sar.save(new ScalingActivity("host", new DateTime(DateTimeZone.UTC), "", "startVM", dh.getName()));

        //startupEntropyContainer(dh);
        return dh;
    }

    private void startupEntropyContainer(DockerHost dh) {
        final DockerClient docker = DefaultDockerClient.builder().
                uri(URI.create("http://" + dh.getUrl() + ":2375")).
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


    @Override
    public final void stopDockerHost(final DockerHost dh) {
        ActionResponse r = os.compute().servers().action(dh.getName(), Action.STOP);

        if (!r.isSuccess()) {
            LOG.error("Dockerhost could not be started", r.getFault());
        } else {
            LOG.info("DockerHost terminated " + dh.getName());
            dhr.delete(dh);
            sar.save(new ScalingActivity("host", new DateTime(DateTimeZone.UTC), "", "stopWM", dh.getName()));
        }
    }


    @Override
    public void markHostForRemoval(DockerHost dh) {
        dh.setScheduledForShutdown(true);
        dh.setTerminationTime(new DateTime(DateTimeZone.UTC));
        dhr.save(dh);
    }

    @Override
    public void removeHostsWhichAreFlaggedToShutdown() {
        for (DockerHost dh : dhr.findAll()) {
            if (dh.getScheduledForShutdown()) {
                DateTime now = new DateTime(DateTimeZone.UTC);
                LOG.info("Housekeeping shuptdown host: current time: " + now + " - " + "termination time:" + new DateTime(dh.getTerminationTime()).plusSeconds(graceperiod * 3));
                if (now.isAfter(new DateTime(dh.getTerminationTime()).plusSeconds(graceperiod * 2))) {
                    stopDockerHost(dh);
                }
            }
        }
    }

}
