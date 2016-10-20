package at.tuwien.infosys.resourceManagement;


import at.tuwien.infosys.configuration.OperatorConfiguration;
import at.tuwien.infosys.datasources.DockerContainerRepository;
import at.tuwien.infosys.datasources.DockerHostRepository;
import at.tuwien.infosys.entities.DockerContainer;
import at.tuwien.infosys.entities.DockerHost;
import at.tuwien.infosys.topology.TopologyManagement;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.LogStream;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.*;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class DockerContainerManagement {

    @Autowired
    private TopologyManagement topologyManagement;

    @Autowired
    private OperatorConfiguration operatorConfiguration;

    @Autowired
    private DockerContainerRepository dcr;

    @Autowired
    private DockerHostRepository dhr;

    @Value("${visp.simulation}")
    private Boolean SIMULATION;

    private static final Logger LOG = LoggerFactory.getLogger(DockerContainerManagement.class);


    //TODO get actual infrastructure host from the topology information to realize distributed topology deployments
    public void startContainer(DockerHost dh, DockerContainer container, String infrastructureHost) throws DockerException, InterruptedException {
        if (SIMULATION) {
            LOG.info("Simulate DockerContainer Startup");
            try {
                TimeUnit.SECONDS.sleep(4);
            } catch (InterruptedException ignore) {
                LOG.error("Simulate DockerContainer Startup failed");
            }

            container.setImage(operatorConfiguration.getImage(container.getOperator()));
            container.setHost(dh.getName());

            dcr.save(container);

            return;
        }


        final DockerClient docker = DefaultDockerClient.builder().uri("http://" + dh.getUrl() + ":2375").connectTimeoutMillis(60000).build();

        if (!dh.getAvailableImages().contains(operatorConfiguration.getImage(container.getOperator()))) {
            docker.pull(operatorConfiguration.getImage(container.getOperator()));
            List<String> availableImages = dh.getAvailableImages();
            availableImages.add(operatorConfiguration.getImage(container.getOperator()));
            dh.setAvailableImages(availableImages);
            dhr.save(dh);
        }


        List<String> environmentVariables = new ArrayList<>();
        environmentVariables.add("SPRING_RABBITMQ_HOST=" + infrastructureHost);
        environmentVariables.add("SPRING_RABBITMQ_OUTGOING_HOST=" + infrastructureHost);
        environmentVariables.add("SPRING_REDIS_HOST=" + infrastructureHost);
        environmentVariables.add("OUTGOINGEXCHANGE=" + container.getOperator());
        environmentVariables.add("INCOMINGQUEUES=" + topologyManagement.getIncomingQueues(container.getOperator()));
        environmentVariables.add("ROLE=" + container.getOperator());


        Double vmCores = dh.getCores();
        Double containerCores = container.getCpuCores();

        long containerRam = (long) container.getRam().doubleValue() * 1024 * 1024;
        long cpuShares = 1024 / (long) Math.ceil(vmCores / containerCores);

        final HostConfig hostConfig = HostConfig.builder()
                .cpuShares(cpuShares)
                .memoryReservation(containerRam)
                .build();

        final ContainerConfig containerConfig = ContainerConfig.builder()
                .hostConfig(hostConfig)
                .image(operatorConfiguration.getImage(container.getOperator()))
                .cmd("sh", "-c", "java -jar vispProcessingNode-0.0.1.jar -Djava.security.egd=file:/dev/./urandom")
                .env(environmentVariables)
                .build();



        final ContainerCreation creation = docker.createContainer(containerConfig);
        final String id = creation.id();

        docker.startContainer(id);

        container.setContainerid(id);
        container.setImage(operatorConfiguration.getImage(container.getOperator()));
        container.setHost(dh.getName());


        ContainerInfo config = docker.inspectContainer(id);

        dcr.save(container);

        LOG.info("VISP - A new container with the ID: " + id + " for the operator: " + container.getOperator() + " on the host: " + dh.getName() + " has been started.");
    }

    public void removeContainer(DockerContainer dc) throws DockerException, InterruptedException {
        if (SIMULATION) {
            LOG.info("Simulate DockerContainer Removal");
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException ignore) {
                LOG.error("Simulate DockerContainer Removal failed");
            }

            dcr.delete(dc);

            return;
        }

        DockerHost dh = dhr.findByName(dc.getHost()).get(0);
        final DockerClient docker = DefaultDockerClient.builder().uri("http://" + dh.getUrl() + ":2375").connectTimeoutMillis(60000).build();

        try {
            docker.killContainer(dc.getContainerid());
            docker.removeContainer(dc.getContainerid());
        } catch (Exception e) {
            LOG.error("Could not kill the container", e);
        }
        dcr.delete(dc);

        LOG.info("VISP - The container: " + dc.getContainerid() + " for the operator: " + dc.getOperator() + " on the host: " + dc.getHost() + " was removed.");
    }

    public String executeCommand(DockerContainer dc, String cmd) throws DockerException, InterruptedException {
        if (SIMULATION) {
            LOG.info("Simulate DockerContainer Command execution");
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException ignore) {
                LOG.error("Simulate DockerContainer Command execution failed");
            }

            return "The command execution was simulated";
        }

        final String[] command = {"bash", "-c", cmd};
        DockerHost dh = dhr.findByName(dc.getHost()).get(0);
        final DockerClient docker = DefaultDockerClient.builder().uri("http://" + dh.getUrl() + ":2375").connectTimeoutMillis(60000).build();

        final ExecCreation execId = docker.execCreate(dc.getContainerid(), command, DockerClient.ExecCreateParam.attachStdout(), DockerClient.ExecCreateParam.attachStderr());
        final LogStream output = docker.execStart(execId.id());
        String result = output.readFully();
        LOG.info("VISP - the command " + cmd + " was executed on the container: " + dc.getContainerid() + " for the operator: " + dc.getOperator() + " on the host: " + dc.getHost() + "with the result: " + result);
        return result;
    }

    public void markContainerForRemoval(DockerContainer dc) {
        dc.setStatus("stopping");
        dc.setTerminationTime(new DateTime(DateTimeZone.UTC).toString());
        dcr.save(dc);
    }
}
