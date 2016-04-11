package at.tuwien.infosys.resourceManagement;


import at.tuwien.infosys.datasources.DockerHostRepository;
import at.tuwien.infosys.entities.DockerHost;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.DockerException;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.HostConfig;
import org.jclouds.ContextBuilder;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.RunNodesException;
import org.jclouds.compute.domain.Hardware;
import org.jclouds.compute.domain.Image;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.Template;
import org.jclouds.domain.Location;
import org.jclouds.javax.annotation.Nullable;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.openstack.nova.v2_0.NovaApi;
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
    private String OPENSTACK_TENANT_ID;


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
        OPENSTACK_TENANT_ID = prop.getProperty("os.tenant.id");

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


    public DockerHost startVM(DockerHost dh) {
        setup();


        //TODO configure options
        //TODO activate REST api via SSH


        /**
         TemplateOptions options = NovaTemplateOptions.Builder
         .userData(nodeSpecificCloudInit.getBytes())
         .keyPairName(cloud_default_key_pair)
         .securityGroups(cloud_security_group);
         **/

        Hardware hardware = hardwareProfiles.get(dh.getFlavour());

        Set<? extends Location> locations = compute.listAssignableLocations();

        Template template = compute.templateBuilder()
                .locationId("myregion")
                //.options(options)
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

        String privateAddress = nodeMetadata.getPrivateAddresses().iterator().next();

        FloatingIPApi floatingIPs = novaApi.getFloatingIPApi("myregion").get();

        String publicIP = null;
        for (FloatingIP ip : floatingIPs.list()) {
            if (ip.getInstanceId() == null) {
                publicIP = ip.getIp();
                floatingIPs.addToServer(publicIP, nodeMetadata.getProviderId());
                break;
            }
        }

        if (publicIP == null) {
            publicIP = floatingIPs.allocateFromPool("cloud").getIp();
            floatingIPs.addToServer(publicIP, nodeMetadata.getProviderId());
        }

        dh.setName(nodeMetadata.getHostname());
        dh.setUrl(publicIP);
        dh.setCores(hardware.getProcessors().size() + 0.0);
        dh.setRam(hardware.getRam());
        dh.setStorage(hardware.getVolumes().get(0).getSize());
        dh.setScheduledForShutdown(false);

        dhr.save(dh);

        LOG.info("VISP - Server with id: " + dh.getId() + " and IP " + publicIP + " was started.");

        //TODO activate REST api via SSH


        //TODO create urls in the dedicated methods (d.h. add port info)
        // TODO startup entropy container
//        startupEntropyContainer(pub);

        return dh;
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
    }


    public void markHostForRemoval(String hostId) {
        DockerHost dh = dhr.findByName(hostId).get(0);
        dh.setScheduledForShutdown(true);
        dh.setTerminationTime(new DateTime(DateTimeZone.UTC).toString());
        dhr.save(dh);
    }

    public void removeHostsWhichAreFlaggedToShutdown() {
        for (DockerHost dh : dhr.findAll()) {
            DateTime now = new DateTime(DateTimeZone.UTC);
            LOG.info("Housekeeping shuptdown host: current time: " + now + " - " + "termination time:" + new DateTime(dh.getTerminationTime()).plusMinutes(graceperiod * 2));
            if (now.isAfter(new DateTime(dh.getTerminationTime()).plusSeconds(graceperiod * 2))) {
                stopDockerHost(dh);
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
