package at.tuwien.infosys.resourceManagement;


import at.tuwien.infosys.datasources.DockerHostRepository;
import at.tuwien.infosys.datasources.ScalingActivityRepository;
import at.tuwien.infosys.entities.DockerHost;
import at.tuwien.infosys.entities.ScalingActivity;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.HostConfig;
import org.apache.commons.io.IOUtils;
import org.jclouds.ContextBuilder;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.RunNodesException;
import org.jclouds.compute.domain.Hardware;
import org.jclouds.compute.domain.Image;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.Template;
import org.jclouds.compute.options.TemplateOptions;
import org.jclouds.javax.annotation.Nullable;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.compute.options.NovaTemplateOptions;
import org.jclouds.openstack.nova.v2_0.domain.FloatingIP;
import org.jclouds.openstack.nova.v2_0.extensions.FloatingIPApi;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
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


    private Map<String, Hardware> hardwareProfiles = new HashMap<>();
    private Map<String, Image> imageProfiles = new HashMap<>();


    private NovaApi novaApi;
    private ComputeService compute;


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

        Iterable<Module> modules = ImmutableSet.<Module>of(new SLF4JLoggingModule());

        novaApi = ContextBuilder.newBuilder("openstack-nova")
                .endpoint(OPENSTACK_AUTH_URL)
                .credentials(OPENSTACK_TENANT_NAME + ":" + OPENSTACK_USERNAME, OPENSTACK_PASSWORD)
                .modules(modules)
                .buildApi(NovaApi.class);

        ComputeServiceContext context = ContextBuilder.newBuilder("openstack-nova")
                .endpoint(OPENSTACK_AUTH_URL)
                .credentials(OPENSTACK_TENANT_NAME + ":" + OPENSTACK_USERNAME, OPENSTACK_PASSWORD)
                .modules(modules)
                .buildView(ComputeServiceContext.class);
        compute = context.getComputeService();

        loadOpenStackData();

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

        TemplateOptions options = NovaTemplateOptions.Builder
                .userData(cloudInit.getBytes())
                .keyPairName(OPENSTACK_KEYPAIR_NAME)
                .securityGroups("default");

        Hardware hardware = hardwareProfiles.get(dh.getFlavour());

        Template template = compute.templateBuilder()
                .locationId("myregion")
                .options(options)
                .fromHardware(hardware)
                .fromImage(imageProfiles.get(dockerhostImage))
                .build();

        Set<? extends NodeMetadata> nodes = null;

        try {
            nodes = compute.createNodesInGroup(dh.getName(), 1, template);
        } catch (RunNodesException e) {
            LOG.error("Could not start Dockerhost." + e.getMessage());
        }

        NodeMetadata nodeMetadata = nodes.iterator().next();

        String ip = nodeMetadata.getPrivateAddresses().iterator().next();

        if (PUBLICIPUSAGE) {
            FloatingIPApi floatingIPs = novaApi.getFloatingIPApi("myregion").get();

            String publicIP = null;
            for (FloatingIP floatingIP : floatingIPs.list()) {
                if (floatingIP.getInstanceId() == null) {
                    publicIP = floatingIP.getIp();
                    floatingIPs.addToServer(publicIP, nodeMetadata.getProviderId());
                    break;
                }
            }

            if (publicIP == null) {
                publicIP = floatingIPs.allocateFromPool("cloud").getIp();
                floatingIPs.addToServer(publicIP, nodeMetadata.getProviderId());
            }
            ip = publicIP;
        }

        dh.setName(nodeMetadata.getHostname());
        dh.setUrl(ip);
        dh.setCores(hardware.getProcessors().get(0).getCores() + 0.0);
        dh.setRam(hardware.getRam());
        dh.setStorage(hardware.getVolumes().get(0).getSize());
        dh.setScheduledForShutdown(false);
        DateTime btuEnd = new DateTime(DateTimeZone.UTC);
        btuEnd = btuEnd.plusSeconds(BTU);
        dh.setBTUend(btuEnd);


        LOG.info("VISP - Server with id: " + dh.getId() + " and IP " + ip + " was started.");

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
        Set<? extends NodeMetadata> nodeMetadatas = compute.destroyNodesMatching(new Predicate<NodeMetadata>() {
            @Override
            public boolean apply(@Nullable NodeMetadata input) {
                boolean contains = input.getName().contains(dh.getName());
                contains = (contains & dh.getScheduledForShutdown());
                return contains;
            }
        });
        for (NodeMetadata nodeMetadata : nodeMetadatas) {
            LOG.info("DockerHost terminated " + nodeMetadata.getName());
        }
        dhr.delete(dh);
        sar.save(new ScalingActivity("host", new DateTime(DateTimeZone.UTC), "", "stopWM", dh.getName()));

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

    protected void loadOpenStackData() {
        Set<? extends Hardware> profiles = compute.listHardwareProfiles();
        for (Hardware profile : profiles) {
            hardwareProfiles.put(profile.getName(), profile);
        }
        Set<? extends org.jclouds.compute.domain.Image> images = compute.listImages();
        for (org.jclouds.compute.domain.Image image : images) {
            imageProfiles.put(image.getProviderId(), image);
        }
    }
}
