package at.tuwien.infosys.processingNodeDeployment;


import at.tuwien.infosys.configuration.OperatorConfiguration;
import at.tuwien.infosys.entities.DockerContainer;
import at.tuwien.infosys.topology.TopologyManagement;
import com.spotify.docker.client.*;
import com.spotify.docker.client.messages.Container;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Service
public class DockerContainerManagement {

    @Autowired
    private TopologyManagement topologyManagement;

    @Autowired
    private OperatorConfiguration operatorConfiguration;

    private static final Logger LOG = LoggerFactory.getLogger(DockerContainerManagement.class);

    private HashMap<String, DockerContainer> deployedContainer;

    public DockerContainerManagement() {
        if (deployedContainer==null) {
            deployedContainer = new HashMap<>();
        }
    }

    public void updateDeployedContainer(List<String> hosts) throws DockerCertificateException, DockerException, InterruptedException {
        deployedContainer = new HashMap<>();

        for (String host : hosts) {
            final DockerClient docker = DefaultDockerClient.builder().uri(URI.create(host)).build();

            DockerClient.ListContainersParam params = DockerClient.ListContainersParam.allContainers(false);
            List<Container> containers = docker.listContainers(params);

            if (containers == null) {
                continue;
            }

            for (Container container : containers) {
                DockerContainer dc = new DockerContainer();
                dc.setId(container.id());
                dc.setImage(container.image());
                dc.setHost(host);
                deployedContainer.put(container.id(), dc);
            }
        }
    }

    public void startContainer(String dockerHost, String operator, String infrastructureHost) throws DockerException, InterruptedException {
        //TODO add configuration to manage multiple hosts
        final DockerClient docker = DefaultDockerClient.builder().uri(URI.create(dockerHost)).build();

        docker.pull(operatorConfiguration.getImage(operator));

        List<String> environmentVariables = new ArrayList<>();
        environmentVariables.add("SPRING_RABBITMQ_HOST=" + infrastructureHost);
        environmentVariables.add("SPRING_RABBITMQ_OUTGOING_HOST=" + infrastructureHost);
        environmentVariables.add("SPRING_REDIS_HOST=" + infrastructureHost);
        environmentVariables.add("OUTGOINGEXCHANGE=" + operator);
        environmentVariables.add("INCOMINGQUEUES=" + topologyManagement.getIncomingQueues(operator));


        //TODO make this flexible in terms of core and memory
        Double vmCores = 4.0;
        Double containerCores = 1.0;
        Double containerRam = 100.0;

        long cpuShares = 1024 / (long) Math.ceil(vmCores / containerCores);
        long memory = (long) (containerRam * 1024 * 1024);


        final ContainerConfig containerConfig = ContainerConfig.builder()
                .image(operatorConfiguration.getImage(operator))
                .cpuShares(cpuShares)
                .memory(memory)
                .env(environmentVariables)
                .cmd("sh", "-c", "java -jar vispProcessingNode-0.0.1.jar")
                .build();

        final ContainerCreation creation = docker.createContainer(containerConfig);
        final String id = creation.id();

        docker.startContainer(id);

        DockerContainer dc = new DockerContainer();
        dc.setId(id);
        dc.setImage(operatorConfiguration.getImage(operator));
        dc.setHost(dockerHost);
        dc.setOperator(operator);

        deployedContainer.put(dc.getId(), dc);
    }

    public void removeContainer(DockerContainer dc) throws DockerException, InterruptedException {
        final DockerClient docker = DefaultDockerClient.builder().uri(URI.create(dc.getHost())).build();
        docker.killContainer(dc.getId());
        docker.removeContainer(dc.getId());
    }

    public String executeCommand(DockerContainer dc, String cmd) throws DockerException, InterruptedException {
        final String[] command = {"bash", "-c", cmd};
        final DockerClient docker = DefaultDockerClient.builder().uri(URI.create(dc.getHost())).build();

        final String execId = docker.execCreate(dc.getId(), command, DockerClient.ExecParameter.STDOUT, DockerClient.ExecParameter.STDERR);
        final LogStream output = docker.execStart(execId);
        String result = output.readFully();
        LOG.info(result);
        return result;
    }

    public HashMap<String, DockerContainer> getDeployedContainer() {
        return deployedContainer;
    }

    public void removeDeployedContainerFromList(String id) {
        deployedContainer.remove(id);
    }
}
