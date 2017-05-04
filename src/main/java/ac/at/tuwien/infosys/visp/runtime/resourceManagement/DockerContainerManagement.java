package ac.at.tuwien.infosys.visp.runtime.resourceManagement;


import ac.at.tuwien.infosys.visp.common.operators.Operator;
import ac.at.tuwien.infosys.visp.runtime.configuration.Configurationprovider;
import ac.at.tuwien.infosys.visp.runtime.configuration.OperatorConfigurationBootstrap;
import ac.at.tuwien.infosys.visp.runtime.datasources.DockerContainerRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.DockerHostRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.DockerContainer;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.DockerHost;
import ac.at.tuwien.infosys.visp.runtime.topology.TopologyManagement;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.LogStream;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.exceptions.ImageNotFoundException;
import com.spotify.docker.client.messages.*;
import jersey.repackaged.com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@DependsOn("configurationprovider")
public class DockerContainerManagement {

    @Autowired
    private TopologyManagement topologyManagement;

    @Autowired
    private OperatorConfigurationBootstrap operatorConfigurationBootstrap;

    @Autowired
    private DockerContainerRepository dcr;

    @Autowired
    private DockerHostRepository dhr;

    @Autowired
    private OperatorConfigurationBootstrap opConfig;

    @Autowired
    private Configurationprovider config;

    @Value("${visp.node.processing.port}")
    private String processingNodeServerPort;

    @Value("${visp.node.port.available}")
    private String encodedHostNodeAvailablePorts;

    private static final Logger LOG = LoggerFactory.getLogger(DockerContainerManagement.class);


    public synchronized void startContainer(DockerHost dh, Operator op) throws DockerException, InterruptedException {
        /* Connect to docker server of the host */
        final DockerClient docker = DefaultDockerClient.builder().uri("http://" + dh.getUrl() + ":2375").connectTimeoutMillis(60000).build();

        DockerContainer dc = opConfig.createDockerContainerConfiguration(op);
        String usedImage = "";

        /* Update the list of available docker images */
        if (!dh.getAvailableImages().contains(op.getType())) {
            //check if type is a valid dockerimage
            try {
                docker.pull(op.getType());
                List<String> availableImages = dh.getAvailableImages();
                availableImages.add(op.getType());
                dh.setAvailableImages(availableImages);
                dhr.save(dh);
                usedImage=op.getType();
            } catch (ImageNotFoundException ex) {
                LOG.info("Operator type docker image (" + op.getType() + ") is not available - falling back to default image: " + config.getProcessingNodeImage());
                if (!dh.getAvailableImages().contains(config.getProcessingNodeImage())) {
                    docker.pull(config.getProcessingNodeImage());
                    List<String> availableImages = dh.getAvailableImages();
                    availableImages.add(config.getProcessingNodeImage());
                    dh.setAvailableImages(availableImages);
                    dhr.save(dh);
                }
                usedImage = config.getProcessingNodeImage();
            }
        }

        /* Configure environment variables */
        List<String> environmentVariables = new ArrayList<>();
        String outgoingHost = op.getConcreteLocation().getIpAddress().equals(config.getRabbitMQHost()) ? config.getRuntimeIP() : op.getConcreteLocation().getIpAddress(); // generalized deployment
        environmentVariables.add("SPRING_RABBITMQ_OUTGOING_HOST=" + outgoingHost);
        environmentVariables.add("SPRING_REDIS_HOST=" + config.getRedisHost());
        environmentVariables.add("OUTGOINGEXCHANGE=" + op.getName());
        environmentVariables.add("INCOMINGQUEUES=" + topologyManagement.getIncomingQueues(op.getName()));
        environmentVariables.add("ROLE=" + op.getType());
        environmentVariables.add("OPERATOR_SUBSCRIBED_OPERATORS=" + topologyManagement.getDownstreamOperators(op.getName()));

        /* Configure docker container */
        Double vmCores = dh.getCores();
        Double containerCores = dc.getCpuCores();

        long containerMemory = (long) dc.getMemory().doubleValue() * 1024 * 1024;
        long cpuShares = 1024 / (long) Math.ceil(vmCores / containerCores);

        /* Bind container port (processingNodeServerPort) to an available host port */
        String hostPort = getAvailablePortOnHost(dh);
        if (hostPort == null)
            throw new DockerException("Not available port on host " + dh.getName() + " to bind a new container");

        final Map<String, List<PortBinding>> portBindings = new HashMap<>();
        portBindings.put(processingNodeServerPort, Lists.newArrayList(PortBinding.of("0.0.0.0", hostPort)));

        final HostConfig hostConfig = HostConfig.builder()
                .cpuShares(cpuShares)
                .memoryReservation(containerMemory)
                .portBindings(portBindings)
                .networkMode("bridge")
                .build();

        final ContainerConfig containerConfig = ContainerConfig.builder()
                .hostConfig(hostConfig)
                .image(usedImage)
                .exposedPorts(processingNodeServerPort)
                .cmd("sh", "-c", "java -jar vispProcessingNode-0.0.1.jar -Djava.security.egd=file:/dev/./urandom")
                .env(environmentVariables)
                .build();

        /* Start docker container */
        final ContainerCreation creation = docker.createContainer(containerConfig);
        final String id = creation.id();
        docker.startContainer(id);

        /* Save docker container information on repository */
        dc.setContainerid(id);
        dc.setImage(usedImage);
        dc.setHost(dh.getName());
        dc.setMonitoringPort(hostPort);
        dc.setStatus("running");
        dc.setTerminationTime(null);
        dcr.save(dc);

        /* Update the set of used port on docker host */
        List<String> usedPorts = dh.getUsedPorts();
        usedPorts.add(hostPort);
        dh.setUsedPorts(usedPorts);
        dhr.save(dh);

        LOG.info("VISP - A new container with the ID: " + id + " for the operatorType: " + dc.getOperatorType() + " on the host: " + dh.getName() + " has been started.");
    }

    public void removeContainer(DockerContainer dc) {
        DockerHost dh = dhr.findFirstByName(dc.getHost());
        final DockerClient docker = DefaultDockerClient.builder().uri("http://" + dh.getUrl() + ":2375").connectTimeoutMillis(60000).build();


        try {
            int count = 0;
            int maxTries = 5;
            while(true) {
                try {
                    if (docker.ping().equals("OK")) {
                        docker.killContainer(dc.getContainerid());
                    }
                    break;
                } catch (InterruptedException | DockerException e) {
                    LOG.warn("Could not kill a docker container - trying again.", e);
                    if (++count == maxTries) throw e;
                }
            }
        } catch (DockerException | InterruptedException e) {
            LOG.error("Could not kill the container", e);
        }

        try {
            int count = 0;
            int maxTries = 5;
            while(true) {
                try {
                    docker.removeContainer(dc.getContainerid());
                    break;
                } catch (InterruptedException | DockerException e) {
                    LOG.warn("Could not remove a docker container - trying again.", e);
                    if (++count == maxTries) throw e;
                }
            }
        } catch (DockerException | InterruptedException e) {
            LOG.error("Could not kill the container", e);
        }

        /* Free monitoring port previously used by the docker container */
        String containerPort = dc.getMonitoringPort();
        List<String> usedPorts = dh.getUsedPorts();
        usedPorts.remove(containerPort);
        dh.setUsedPorts(usedPorts);
        dhr.save(dh);

        dcr.delete(dc);

        LOG.info("VISP - The container: " + dc.getContainerid() + " for the operatorType: " + dc.getOperatorType() + " on the host: " + dc.getHost() + " was removed.");
    }

    public String executeCommand(DockerContainer dc, String cmd)  {
        LOG.info("in executeCommand for cmd: " + cmd);
        final String[] command = {"bash", "-c", cmd};
        DockerHost dh = dhr.findFirstByName(dc.getHost());
        if(dh == null) {
            throw new RuntimeException("Could not find dockerhost by name: " + dc.getHost());
        }

        try {
            final DockerClient docker = DefaultDockerClient.builder().uri("http://" + dh.getUrl() + ":2375").connectTimeoutMillis(60000).build();
            final ExecCreation execId = docker.execCreate(dc.getContainerid(), command, DockerClient.ExecCreateParam.attachStdout(), DockerClient.ExecCreateParam.attachStderr());
            final LogStream output = docker.execStart(execId.id());
            String result = output.readFully();
            LOG.info("VISP - the command " + cmd + " was executed on the container: " + dc.getContainerid() + " for the operatorType: " + dc.getOperatorType() + " on the host: " + dc.getHost() + "with the result: " + result);
            return result;
        } catch(Exception e) {
            // this exception is a bug in the spotify docker lib
            LOG.warn("Spotify lib bug");
            return "<could not fetch result from docker-host>";
        }
    }

    public void markContainerForRemoval(DockerContainer dc) {
        dc.setStatus("stopping");
        dc.setTerminationTime(new DateTime(DateTimeZone.UTC));
        dcr.saveAndFlush(dc);
        LOG.info("Marked container " + dc + " for removal");
    }


    private String getAvailablePortOnHost(DockerHost host) {

        String[] range = encodedHostNodeAvailablePorts.replaceAll("[a-zA-Z\']", "").split("-");
        int poolStart = Integer.valueOf(range[0]);
        int poolEnd = Integer.valueOf(range[1]);

        List<String> usedPorts = host.getUsedPorts();

        for (int port = poolStart; port < poolEnd; port++) {

            String portStr = Integer.toString(port);

            if (!usedPorts.contains(portStr)) {
                return portStr;
            }
        }
        return null;
    }


    public Boolean checkAvailabilityofDockerhost(String url) {
        final DockerClient docker = DefaultDockerClient.builder().uri("http://" + url + ":2375").connectTimeoutMillis(5000).build();
        try {
            return docker.ping().equals("OK");
        } catch (DockerException | InterruptedException e) {
            return false;
        }
    }

    public Boolean checkIfContainerIsRunning(DockerContainer dc) {
        DockerHost dh = dhr.findFirstByName(dc.getHost());
        final DockerClient docker = DefaultDockerClient.builder().uri("http://" + dh.getUrl() + ":2375").connectTimeoutMillis(5000).build();
        try {
            final ContainerInfo info = docker.inspectContainer(dc.getContainerid());
            return info.state().running();
        } catch (DockerException | InterruptedException e) {
            return true;
        }
    }

}
