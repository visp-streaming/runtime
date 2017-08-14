package ac.at.tuwien.infosys.visp.runtime.resourceManagement.connectors;

import java.net.URI;

import ac.at.tuwien.infosys.visp.runtime.configuration.Configurationprovider;
import ac.at.tuwien.infosys.visp.runtime.datasources.DockerHostRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.DockerHost;
import ac.at.tuwien.infosys.visp.runtime.resourceManagement.connectors.impl.OpenstackConnector;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.HostConfig;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;


public abstract class ResourceConnector {

    @Value("${visp.entropyContainerName}")
    private String entropyContainerName;

    @Autowired
    protected DockerHostRepository dhr;

    @Autowired
    protected Configurationprovider config;

    private static final Logger LOG = LoggerFactory.getLogger(OpenstackConnector.class);

    public abstract DockerHost startVM(DockerHost dh);

    protected abstract void stopDockerHost(DockerHost dh);

    public abstract void markHostForRemoval(DockerHost dh);

    public void removeHostsWhichAreFlaggedToShutdown() {
        for (DockerHost dh : dhr.findAll()) {
            if (dh.getScheduledForShutdown()) {
                DateTime now = new DateTime(DateTimeZone.UTC);
                LOG.info("Housekeeping shuptdown host: current time: " + now + " - " + "termination time:" + new DateTime(dh.getTerminationTime()).plusSeconds(config.getShutdowngrace() * 3));
                if (now.isAfter(new DateTime(dh.getTerminationTime()).plusSeconds(config.getShutdowngrace() * 2))) {
                    stopDockerHost(dh);
                }
            }
        }
    }

    protected void startupEntropyContainer(DockerHost dh) {
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

        } catch (DockerException | InterruptedException e) {
            LOG.error("Could not start container", e);
        }
    }
}
