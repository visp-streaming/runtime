package at.tuwien.infosys.monitoring;

import at.tuwien.infosys.datasources.DockerContainerMonitorRepository;
import at.tuwien.infosys.datasources.DockerContainerRepository;
import at.tuwien.infosys.datasources.DockerHostRepository;
import at.tuwien.infosys.datasources.entities.DockerContainer;
import at.tuwien.infosys.datasources.entities.DockerContainerMonitor;
import at.tuwien.infosys.datasources.entities.DockerHost;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ContainerStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * ResourceMonitor monitors the computing resources,
 * which are used to execute the processing nodes.
 * <p>
 * In this implementation, ResourceMonitor retrieves
 * the CPU utilization of each DockerContainer
 * and stores this information in the Runtime repository.
 */
@Service
public class ResourceMonitor {

    @Autowired
    private DockerContainerRepository dcr;

    @Autowired
    private DockerContainerMonitorRepository dcmr;

    @Autowired
    private DockerHostRepository dhr;

    private static final String CONNECTION_PROTOCOL = "http://";
    private static final String CONNECTION_PORT = ":2375";

    private static final Logger LOG = LoggerFactory.getLogger(ResourceMonitor.class);

    @Scheduled(fixedRateString = "${visp.monitor.period}")
    public void updateAllHostsCpuUtilization() {

        for (DockerHost dockerHost : dhr.findAll()) {
            updateCpuUtilization(dockerHost);
        }
    }

    public void updateCpuUtilization(DockerHost dh) {
        List<DockerContainer> hostedContainers = dcr.findByHost(dh.getName());

        for (DockerContainer container : hostedContainers) {
            retrieveCpuUtilization(dh, container);
        }
        dcr.save(hostedContainers);
    }


    private DockerContainer retrieveCpuUtilization(DockerHost dh, DockerContainer dc) {
        String connectionUri = CONNECTION_PROTOCOL + dh.getUrl() + CONNECTION_PORT;
        final DockerClient docker = DefaultDockerClient.builder().uri(connectionUri).connectTimeoutMillis(60000).build();
        ContainerStats stats;

        DockerContainerMonitor oldDcm = dcmr.findFirstByContaineridOrderByTimestampDesc(dc.getContainerid());
        DockerContainerMonitor dcm = new DockerContainerMonitor(dc.getContainerid(), dc.getOperator());

        try {
            stats = docker.stats(dc.getContainerid());

            if (oldDcm == null) {

                LOG.debug("Container " + dc.getContainerid() + " first computation of CPU Utilization");

                dcm.setCpuUsage(stats.cpuStats().cpuUsage().totalUsage());
                dcm.setSystemUsage(stats.cpuStats().systemCpuUsage());
                dcm.setDerivedCpuUsage(0);
                dcm.setMemoryUsage(stats.memoryStats().usage());

            } else {

	            /* Calculate the change of container's usage in between readings */
                long currentCpuUsage = stats.cpuStats().cpuUsage().totalUsage();
                long currentSystemUsage = stats.cpuStats().systemCpuUsage();
                long cpuDelta = currentCpuUsage - oldDcm.getCpuUsage();
                long systemDelta = currentSystemUsage - oldDcm.getSystemUsage();


                if (systemDelta > 0 && cpuDelta > 0) {
                    /* This information should be scaled with respect to the CPU share */
                    double allocatedCpuShares = dc.getCpuCores() / dh.getCores();

                    double cpuUsage = ((double) cpuDelta / (double) systemDelta) / allocatedCpuShares; // * 100.0;

                    LOG.debug("Container " + dc.getContainerid() + " CPU Utilization: "
                            + cpuUsage + " (allocatedShares: " + allocatedCpuShares + ")");

                    dcm.setDerivedCpuUsage(cpuUsage);
                    dcm.setCpuUsage(currentCpuUsage);
                    dcm.setSystemUsage(currentSystemUsage);
                    dcm.setMemoryUsage(stats.memoryStats().usage());
                }
            }
            dcmr.save(dcm);

        } catch (DockerException | InterruptedException e) {
            LOG.error(e.getMessage());
        }

        return dc;
    }
}
