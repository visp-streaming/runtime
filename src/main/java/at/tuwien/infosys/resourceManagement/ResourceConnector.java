package at.tuwien.infosys.resourceManagement;

import at.tuwien.infosys.datasources.DockerHostRepository;
import at.tuwien.infosys.datasources.entities.DockerHost;
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

import java.net.URI;


public abstract class ResourceConnector {

    @Value("${visp.shutdown.graceperiod}")
    private Integer graceperiod;

    @Value("${visp.entropyContainerName}")
    private String entropyContainerName;

    @Autowired
    protected DockerHostRepository dhr;

    private static final Logger LOG = LoggerFactory.getLogger(OpenstackConnector.class);

    public abstract DockerHost startVM(DockerHost dh);

    public abstract void stopDockerHost(DockerHost dh);

    public abstract void markHostForRemoval(DockerHost dh);

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
