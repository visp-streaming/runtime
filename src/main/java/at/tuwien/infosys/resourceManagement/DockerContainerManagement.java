package at.tuwien.infosys.resourceManagement;


import at.tuwien.infosys.configuration.OperatorConfiguration;
import at.tuwien.infosys.datasources.DockerContainerRepository;
import at.tuwien.infosys.datasources.ScalingActivityRepository;
import at.tuwien.infosys.entities.DockerContainer;
import at.tuwien.infosys.entities.DockerHost;
import at.tuwien.infosys.entities.ScalingActivity;
import at.tuwien.infosys.topology.TopologyManagement;
import com.spotify.docker.client.*;
import com.spotify.docker.client.messages.Container;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Service
public class DockerContainerManagement {

    @Autowired
    private TopologyManagement topologyManagement;

    @Autowired
    private OperatorConfiguration operatorConfiguration;

    @Autowired
    private DockerContainerRepository dcr;

    @Autowired
    private ScalingActivityRepository sar;

    @Value("${visp.simulation}")
    private Boolean SIMULATION;

    private static final Logger LOG = LoggerFactory.getLogger(DockerContainerManagement.class);

    public void updateDeployedContainer(List<String> hosts) throws DockerCertificateException, DockerException, InterruptedException {
        dcr.deleteAll();

        for (String host : hosts) {
            final DockerClient docker = DefaultDockerClient.builder().uri(URI.create(host)).build();

            DockerClient.ListContainersParam params = DockerClient.ListContainersParam.allContainers(false);
            List<Container> containers = docker.listContainers(params);

            if (containers == null) {
                continue;
            }

            for (Container container : containers) {
                DockerContainer dc = new DockerContainer();
                dc.setContainerid(container.id());
                dc.setImage(container.image());
                dc.setHost(host);
                dc.setStatus("running");
                dcr.save(dc);
            }
        }
    }


    //TODO get actual infrastructure host from the topology information to realize distributed topology deployments
    public void startContainer(DockerHost dockerHost, DockerContainer container, String infrastructureHost) throws DockerException, InterruptedException {
        if (SIMULATION) {
            LOG.info("Simulate DockerContainer Startup");
            try {
                //TODO differentiate between cold and warm startup
                Thread.sleep(1000 * 4);
            } catch (InterruptedException ignore) {
                LOG.error("Simulate DockerContainer Startup failed");
            }

            container.setImage(operatorConfiguration.getImage(container.getOperator()));
            container.setHost(dockerHost.getName());

            dcr.save(container);

            sar.save(new ScalingActivity(new DateTime(DateTimeZone.UTC).toString(), container.getOperator(), "scaleup", dockerHost.getName()));

            return;
        }


        final DockerClient docker = DefaultDockerClient.builder().uri("http://" + dockerHost.getUrl() + ":2375").connectTimeoutMillis(60000).build();

        docker.pull(operatorConfiguration.getImage(container.getOperator()));

        List<String> environmentVariables = new ArrayList<>();
        environmentVariables.add("SPRING_RABBITMQ_HOST=" + infrastructureHost);
        environmentVariables.add("SPRING_RABBITMQ_OUTGOING_HOST=" + infrastructureHost);
        environmentVariables.add("SPRING_REDIS_HOST=" + infrastructureHost);
        environmentVariables.add("OUTGOINGEXCHANGE=" + container.getOperator());
        environmentVariables.add("INCOMINGQUEUES=" + topologyManagement.getIncomingQueues(container.getOperator()));
        environmentVariables.add("ROLE=" + container.getOperator());


        Double vmCores = dockerHost.getCores();
        Double containerCores = container.getCpuCores();
        Double containerRam = container.getRam().doubleValue();

        long cpuShares = 1024 / (long) Math.ceil(vmCores / containerCores);
        long memory = (long) (containerRam * 1024 * 1024);

        //TODO implement memory restrictions
        final ContainerConfig containerConfig = ContainerConfig.builder()
                .image(operatorConfiguration.getImage(container.getOperator()))
                //.cpuShares(cpuShares)
                //.memory(memory)
                .env(environmentVariables)
                .cmd("sh", "-c", "java -jar vispProcessingNode-0.0.1.jar -Djava.security.egd=file:/dev/./urandom")
                .build();

        final ContainerCreation creation = docker.createContainer(containerConfig);
        final String id = creation.id();

        docker.startContainer(id);

        container.setContainerid(id);
        container.setImage(operatorConfiguration.getImage(container.getOperator()));
        container.setHost(dockerHost.getName());

        dcr.save(container);

        sar.save(new ScalingActivity(new DateTime(DateTimeZone.UTC).toString(), container.getOperator(), "scaleup", dockerHost.getName()));

        LOG.info("VISP - A new container with the ID: " + id + " for the operator: " + container.getOperator() + " on the host: " + dockerHost.getName());
    }

    public void removeContainer(DockerContainer dc) throws DockerException, InterruptedException {
        if (SIMULATION) {
            LOG.info("Simulate DockerContainer Removal");
            try {
                Thread.sleep(1000 * 1);
            } catch (InterruptedException ignore) {
                LOG.error("Simulate DockerContainer Removal failed");
            }

            dcr.delete(dc);
            sar.save(new ScalingActivity(new DateTime(DateTimeZone.UTC).toString(), dc.getOperator(), "scaledown", dc.getHost()));

            return;
        }

        final DockerClient docker = DefaultDockerClient.builder().uri(URI.create(dc.getHost())).build();
        docker.killContainer(dc.getContainerid());
        docker.removeContainer(dc.getContainerid());
        dcr.delete(dc);
        sar.save(new ScalingActivity(new DateTime(DateTimeZone.UTC).toString(), dc.getOperator(), "scaledown", dc.getHost()));

        LOG.info("VISP - The container: " + dc.getContainerid() + " for the operator: " + dc.getOperator() + " on the host: " + dc.getHost() + " was removed.");
    }

    public String executeCommand(DockerContainer dc, String cmd) throws DockerException, InterruptedException {
        if (SIMULATION) {
            LOG.info("Simulate DockerContainer Command execution");
            try {
                Thread.sleep(1000 * 2);
            } catch (InterruptedException ignore) {
                LOG.error("Simulate DockerContainer Command execution failed");
            }

            return "The command execution was simulated";
        }

        final String[] command = {"bash", "-c", cmd};
        final DockerClient docker = DefaultDockerClient.builder().uri(URI.create(dc.getHost())).build();

        final String execId = docker.execCreate(dc.getContainerid(), command, DockerClient.ExecCreateParam.attachStdout(), DockerClient.ExecCreateParam.attachStderr());
        final LogStream output = docker.execStart(execId);
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
