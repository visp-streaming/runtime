package ac.at.tuwien.infosys.visp.runtime.monitoring;

import ac.at.tuwien.infosys.visp.runtime.datasources.DockerContainerMonitorRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.DockerContainerRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.DockerHostRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.DockerContainer;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.DockerContainerMonitor;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.DockerHost;
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

        DockerContainerMonitor dcm = new DockerContainerMonitor(dc.getContainerid(), dc.getOperatorType());

        try {
            stats = docker.stats(dc.getContainerid());

            long cpuDelta = stats.cpuStats().cpuUsage().totalUsage() -  stats.precpuStats().cpuUsage().totalUsage();
            long systemDelta = stats.cpuStats().systemCpuUsage() - stats.precpuStats().systemCpuUsage();
            double allocatedCpuShares = dc.getCpuCores() / dh.getCores();

            double cpuUsage = ((double) cpuDelta / (double) systemDelta) / allocatedCpuShares * 100;

            dcm.setCpuUsage(cpuUsage);
            dcm.setMemoryUsage((stats.memoryStats().usage() / 1024 / 1024));

            dcmr.save(dcm);

        } catch (DockerException | InterruptedException e) {
            LOG.error(e.getMessage());
        }

        return dc;
    }
}
