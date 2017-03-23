package ac.at.tuwien.infosys.visp.runtime.resourceManagement.connectors.impl;


import ac.at.tuwien.infosys.visp.runtime.configuration.Configurationprovider;
import ac.at.tuwien.infosys.visp.runtime.datasources.DockerHostRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.ScalingActivityRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.DockerHost;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.ScalingActivity;
import ac.at.tuwien.infosys.visp.runtime.resourceManagement.connectors.ResourceConnector;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerException;
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
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

@Service
@DependsOn("configurationprovider")
public class OpenstackConnector extends ResourceConnector {

    @Value("${visp.shutdown.graceperiod}")
    private Integer graceperiod;

    @Value("${visp.openstack.publicip}")
    private Boolean PUBLICIPUSAGE;

    @Value("${visp.btu}")
    private Integer BTU;

    @Autowired
    private ScalingActivityRepository sar;

    @Autowired
    private DockerHostRepository dhr;

    @Autowired
    private Configurationprovider config;

    private static final Logger LOG = LoggerFactory.getLogger(OpenstackConnector.class);

    private String OPENSTACK_KEYPAIR_NAME;

    private OSClient.OSClientV2 os;

    private void setup() {

        Properties prop = new Properties();
        try {
            prop.load(getClass().getClassLoader().getResourceAsStream("credential.properties"));
        } catch (IOException e) {
            LOG.error("Could not load properties.", e);
        }

        String OPENSTACK_AUTH_URL = prop.getProperty("os.auth.url");
        String OPENSTACK_USERNAME = prop.getProperty("os.username");
        String OPENSTACK_PASSWORD = prop.getProperty("os.password");
        String OPENSTACK_TENANT_NAME = prop.getProperty("os.tenant.name");
        OPENSTACK_KEYPAIR_NAME = prop.getProperty("os.keypair.name");


        os = OSFactory.builderV2()
                .endpoint(OPENSTACK_AUTH_URL)
                .credentials(OPENSTACK_USERNAME, OPENSTACK_PASSWORD)
                .tenantName(OPENSTACK_TENANT_NAME)
                .authenticate();

        LOG.info("VISP - Successfully connected to " + OPENSTACK_AUTH_URL + " on tenant " + OPENSTACK_TENANT_NAME + " with user " + OPENSTACK_USERNAME);
    }

    public List<String> getFlavours() {
        setup();
        List<String> flavours = new ArrayList<>();
        for (Flavor flavour : os.compute().flavors().list()) {
            flavours.add(flavour.getName());
        }
        return flavours;
    }



    @Override
    public DockerHost startVM(DockerHost dh) {
        setup();

        if (dh == null) {
            dh = new DockerHost("additionaldockerhost");
            dh.setFlavour("m2.medium");
        }

        String cloudInit = "";
        try {
            cloudInit = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("docker-config/cloud-init"), "UTF-8");
        } catch (IOException e) {
            LOG.error("Could not load cloud init file");
        }

        Flavor flavor = os.compute().flavors().get(dh.getFlavour());

        for (Flavor f : os.compute().flavors().list()) {
            if (f.getName().equals(dh.getFlavour())) {
                flavor = f;
                break;
            }
        }

        ServerCreate sc = Builders.server()
                .name("dockerhost")
                .flavor(flavor)
                .image(config.getOpenstackProcessingHostImage())
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

            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                //TODO remove if openstack behaves again
            }

            ActionResponse ipresponse = os.compute().floatingIps().addFloatingIP(server, freeIP.getFloatingIpAddress());
            if (!ipresponse.isSuccess()) {
                LOG.error("IP could not be retrieved:" + ipresponse.getFault());
            }
            uri = freeIP.getFloatingIpAddress();
        }

        dh.setResourcepool("openstack");
        dh.setName(server.getId());
        dh.setUrl(uri);
        dh.setCores(flavor.getVcpus() + 0.0);
        dh.setMemory(flavor.getRam());
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
                        connectTimeoutMillis(100000).build();
                docker.ping();
                connection = true;
            } catch (InterruptedException | DockerException e) {
                LOG.debug("Dockerhost is not available yet.", e);
            }
        }

        dhr.save(dh);
        sar.save(new ScalingActivity("host", new DateTime(DateTimeZone.UTC), "", "startVM", dh.getName()));

        return dh;
    }

    @Override
    public final void stopDockerHost(final DockerHost dh) {
        stopDockerHost(dh.getName());
        dhr.delete(dh);
        sar.save(new ScalingActivity("host", new DateTime(DateTimeZone.UTC), "", "stopWM", dh.getName()));
    }

    public final void stopDockerHost(final String id) {
        setup();
        ActionResponse r = os.compute().servers().delete(id);

        if (!r.isSuccess()) {
            LOG.error("Dockerhost could not be stopped: " +  r.getFault());
        } else {
            LOG.info("DockerHost terminated " + id);
        }
    }

    @Override
    public void markHostForRemoval(DockerHost dh) {
        dh.setScheduledForShutdown(true);
        dh.setTerminationTime(new DateTime(DateTimeZone.UTC));
        dhr.save(dh);
    }

}
