package at.tuwien.infosys.processingNodeDeployment;


import at.tuwien.infosys.configuration.OperatorConfiguration;
import at.tuwien.infosys.datasources.DockerContainerRepository;
import at.tuwien.infosys.datasources.ScalingActivityRepository;
import at.tuwien.infosys.entities.DockerContainer;
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

    @Value("${visp.operator.cpu}")
    private Double operatorCPU;

    @Value("${visp.operator.ram}")
    private Integer operatorRAM;

    @Value("${visp.operator.storage}")
    private Integer operatorStorage;


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

    public void startContainer(String dockerHost, String operator, String infrastructureHost) throws DockerException, InterruptedException {
        final DockerClient docker = DefaultDockerClient.builder().uri(URI.create(dockerHost)).connectTimeoutMillis(60000).build();

        docker.pull(operatorConfiguration.getImage(operator));

        List<String> environmentVariables = new ArrayList<>();
        environmentVariables.add("SPRING_RABBITMQ_HOST=" + infrastructureHost);
        environmentVariables.add("SPRING_RABBITMQ_OUTGOING_HOST=" + infrastructureHost);
        environmentVariables.add("SPRING_REDIS_HOST=" + infrastructureHost);
        environmentVariables.add("OUTGOINGEXCHANGE=" + operator);
        environmentVariables.add("INCOMINGQUEUES=" + topologyManagement.getIncomingQueues(operator));
        environmentVariables.add("ROLE=" + operator);


        //TODO make this flexible in terms of core and memory - checkout viepep
       /*
        Double vmCores = 4.0;
        Double containerCores = 1.0;
        Double containerRam = 100.0;

        long cpuShares = 1024 / (long) Math.ceil(vmCores / containerCores);
        long memory = (long) (containerRam * 1024 * 1024);
        */

        final ContainerConfig containerConfig = ContainerConfig.builder()
                .image(operatorConfiguration.getImage(operator))
                //.cpuShares(cpuShares)
                //.memory(memory)
                .env(environmentVariables)
                .cmd("sh", "-c", "java -jar vispProcessingNode-0.0.1.jar -Djava.security.egd=file:/dev/./urandom")
                .build();

        final ContainerCreation creation = docker.createContainer(containerConfig);
        final String id = creation.id();

        docker.startContainer(id);

        DockerContainer dc = new DockerContainer();
        dc.setContainerid(id);
        dc.setImage(operatorConfiguration.getImage(operator));
        dc.setHost(dockerHost);
        dc.setOperator(operator);

        dc.setStorage(operatorStorage);
        dc.setRam(operatorRAM);
        dc.setCpuCores(operatorCPU);

        dcr.save(dc);

        sar.save(new ScalingActivity(new DateTime(DateTimeZone.UTC).toString(), operator, "scaleup", dockerHost));

        LOG.info("VISP - A new container with the ID: " + id + " for the operator: " + operator + " on the host: " + dockerHost);
    }

    public void removeContainer(DockerContainer dc) throws DockerException, InterruptedException {
        final DockerClient docker = DefaultDockerClient.builder().uri(URI.create(dc.getHost())).build();
        docker.killContainer(dc.getContainerid());
        docker.removeContainer(dc.getContainerid());
        dcr.delete(dc);
        sar.save(new ScalingActivity(new DateTime(DateTimeZone.UTC).toString(), dc.getOperator(), "scaledown", dc.getHost()));

        LOG.info("VISP - The container: " + dc.getContainerid() + " for the operator: " + dc.getOperator() + " on the host: " + dc.getHost() + " was removed.");
    }

    public String executeCommand(DockerContainer dc, String cmd) throws DockerException, InterruptedException {
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
