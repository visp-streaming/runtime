package ac.at.tuwien.infosys.visp.runtime.resourceManagement;


import ac.at.tuwien.infosys.visp.common.operators.Operator;
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
import com.spotify.docker.client.messages.*;
import jersey.repackaged.com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
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

    @Value("${visp.node.processing.port}")
    private String processingNodeServerPort;

    @Value("${visp.node.port.available}'")
    private String encodedHostNodeAvailablePorts;

    @Value("${spring.redis.host}'")
    private String redisHost;

    @Value("${spring.rabbitmq.host}")
    private String rabbitMqHost;

    @Value("${visp.runtime.ip}")
    private String ownIp;

    private static final Logger LOG = LoggerFactory.getLogger(DockerContainerManagement.class);


    public void startContainer(DockerHost dh, Operator op) throws DockerException, InterruptedException {
        /* Connect to docker server of the host */
        final DockerClient docker = DefaultDockerClient.builder().uri("http://" + dh.getUrl() + ":2375").connectTimeoutMillis(60000).build();

        DockerContainer dc = opConfig.createDockerContainerConfiguration(op);

        /* Update the list of available docker images */
        if (!dh.getAvailableImages().contains(operatorConfigurationBootstrap.getImage(dc.getOperatorType()))) {
            docker.pull(operatorConfigurationBootstrap.getImage(dc.getOperatorType()));
            List<String> availableImages = dh.getAvailableImages();
            availableImages.add(operatorConfigurationBootstrap.getImage(dc.getOperatorType()));
            dh.setAvailableImages(availableImages);
            dhr.save(dh);
        }

        /* Configure environment variables */
        List<String> environmentVariables = new ArrayList<>();
        String outgoingHost = op.getConcreteLocation().getIpAddress().equals(rabbitMqHost) ? ownIp : op.getConcreteLocation().getIpAddress(); // generalized deployment
        environmentVariables.add("SPRING_RABBITMQ_OUTGOING_HOST=" + outgoingHost); // TODO: check if this is always the right host
        environmentVariables.add("SPRING_REDIS_HOST=" + redisHost);
        environmentVariables.add("OUTGOINGEXCHANGE=" + op.getName());
        environmentVariables.add("INCOMINGQUEUES=" + topologyManagement.getIncomingQueues(op.getName()));
        environmentVariables.add("ROLE=" + op.getType());
        environmentVariables.add("OPERATOR_SUBSCRIBED_OPERATORS=" + topologyManagement.getDownstreamOperators(op.getName()));

        LOG.info("Printing env variables");
        //TODO set correct environment variables - especially for hosts
        for(String ev : environmentVariables) {
            LOG.info(ev);
        }

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
                .image(operatorConfigurationBootstrap.getImage(dc.getOperatorType()))
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
        dc.setImage(operatorConfigurationBootstrap.getImage(dc.getOperatorType()));
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

    //TODO get actual infrastructure host from the topology information to realize distributed topology deployments
    @Deprecated
    public void startContainer(DockerHost dh, DockerContainer container, String infrastructureHost) throws DockerException, InterruptedException {
        LOG.error("!! WARNING !! - this method is deprecated and the wrong outgoing host is set since no operator information is available");
        /* Connect to docker server of the host */
        final DockerClient docker = DefaultDockerClient.builder().uri("http://" + dh.getUrl() + ":2375").connectTimeoutMillis(60000).build();

        /* Update the list of available docker images */
        if (!dh.getAvailableImages().contains(operatorConfigurationBootstrap.getImage(container.getOperatorType()))) {
            docker.pull(operatorConfigurationBootstrap.getImage(container.getOperatorType()));
            List<String> availableImages = dh.getAvailableImages();
            availableImages.add(operatorConfigurationBootstrap.getImage(container.getOperatorType()));
            dh.setAvailableImages(availableImages);
            dhr.save(dh);
        }

        /* Configure environment variables */
        List<String> environmentVariables = new ArrayList<>();
        environmentVariables.add("SPRING_RABBITMQ_HOST=" + infrastructureHost);
        environmentVariables.add("SPRING_RABBITMQ_OUTGOING_HOST=" + infrastructureHost);
        environmentVariables.add("SPRING_REDIS_HOST=" + infrastructureHost);
        environmentVariables.add("OUTGOINGEXCHANGE=" + container.getOperatorType());
        environmentVariables.add("INCOMINGQUEUES=" + topologyManagement.getIncomingQueues(container.getOperatorType()));
        environmentVariables.add("ROLE=" + container.getOperatorType());
        environmentVariables.add("OPERATOR_SUBSCRIBED_OPERATORS=" + topologyManagement.getDownstreamOperators(container.getOperatorType()));

        /* Configure docker container */
        Double vmCores = dh.getCores();
        Double containerCores = container.getCpuCores();

        long containerMemory = (long) container.getMemory().doubleValue() * 1024 * 1024;
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
                .image(operatorConfigurationBootstrap.getImage(container.getOperatorType()))
                .exposedPorts(processingNodeServerPort)
                .cmd("sh", "-c", "java -jar vispProcessingNode-0.0.1.jar -Djava.security.egd=file:/dev/./urandom")
                .env(environmentVariables)
                .build();

        /* Start docker container */
        final ContainerCreation creation = docker.createContainer(containerConfig);
        final String id = creation.id();
        docker.startContainer(id);
        
        /* Save docker container information on repository */
        container.setContainerid(id);
        container.setImage(operatorConfigurationBootstrap.getImage(container.getOperatorType()));
        container.setHost(dh.getName());
        container.setMonitoringPort(hostPort);
        container.setStatus("running");
        container.setTerminationTime(null);
        dcr.save(container);

        /* Update the set of used port on docker host */
        List<String> usedPorts = dh.getUsedPorts();
        usedPorts.add(hostPort);
        dh.setUsedPorts(usedPorts);
        dhr.save(dh);

        LOG.info("VISP - A new container with the ID: " + id + " for the operatorType: " + container.getOperatorType() + " on the host: " + dh.getName() + " has been started.");
    }

    public void removeContainer(DockerContainer dc) {
        DockerHost dh = dhr.findFirstByName(dc.getHost());
        final DockerClient docker = DefaultDockerClient.builder().uri("http://" + dh.getUrl() + ":2375").connectTimeoutMillis(60000).build();


        try {
            int count = 0;
            int maxTries = 5;
            while(true) {
                try {
                    docker.killContainer(dc.getContainerid());
                    break;
                } catch (InterruptedException | DockerException e) {
                    LOG.warn("Could not kill a docker container - trying again.", e);
                    if (++count == maxTries) throw e;
                }
            }        } catch (DockerException | InterruptedException e) {
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

    public String executeCommand(DockerContainer dc, String cmd) throws DockerException, InterruptedException {
        LOG.info("in executeCommand for cmd: " + cmd);
        final String[] command = {"bash", "-c", cmd};
        DockerHost dh = dhr.findFirstByName(dc.getHost());
        if(dh == null) {
            throw new RuntimeException("Could not find dockerhost by name: " + dc.getHost());
        }
        final DockerClient docker = DefaultDockerClient.builder().uri("http://" + dh.getUrl() + ":2375").connectTimeoutMillis(60000).build();

        final ExecCreation execId = docker.execCreate(dc.getContainerid(), command, DockerClient.ExecCreateParam.attachStdout(), DockerClient.ExecCreateParam.attachStderr());
        final LogStream output = docker.execStart(execId.id());
        String result = output.readFully();
        LOG.info("VISP - the command " + cmd + " was executed on the container: " + dc.getContainerid() + " for the operatorType: " + dc.getOperatorType() + " on the host: " + dc.getHost() + "with the result: " + result);
        return result;
    }

    public void markContainerForRemoval(DockerContainer dc) {
        dc.setStatus("stopping");
        dc.setTerminationTime(new DateTime(DateTimeZone.UTC));
        dcr.save(dc);
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
}
