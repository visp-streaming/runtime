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
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * ResourceMonitor monitors the computing resources,
 * which are used to execute the processing nodes.
 * <p>
 * In this implementation, ResourceMonitor retrieves
 * the CPU utilization of each DockerContainer
 * and stores this information in the Runtime repository.
 */
@Service
@DependsOn("configurationprovider")
public class ResourceMonitor {

    @Autowired
    private DockerContainerRepository dcr;

    @Autowired
    private DockerContainerMonitorRepository dcmr;

    @Autowired
    private DockerHostRepository dhr;

    private static final Logger LOG = LoggerFactory.getLogger(ResourceMonitor.class);

    @Scheduled(fixedRateString = "#{@configurationprovider.monitoringperiod}")
    public void updateAllHostsCpuUtilization() {
        for (DockerHost dh : dhr.findAll()) {
            for (DockerContainer container : dcr.findByHost(dh.getName())) {
                retrieveCpuUtilization(dh, container);
            }
        }
    }

    private void retrieveCpuUtilization(DockerHost dh, DockerContainer dc) {
        String connectionUri = "http://" + dh.getUrl() + ":2375";
        final DockerClient docker = DefaultDockerClient.builder().uri(connectionUri).connectTimeoutMillis(60000).build();
        ContainerStats stats;



        DockerContainerMonitor dcm = new DockerContainerMonitor(dc.getOperatorType(), dc.getOperatorName(), dc.getContainerid());

        try {
            stats = docker.stats(dc.getContainerid());

            long cpuDelta;
            try {
                cpuDelta = stats.cpuStats().cpuUsage().totalUsage() - stats.precpuStats().cpuUsage().totalUsage();
            } catch(NullPointerException e) {
                cpuDelta = 0;
            }            long systemDelta = stats.cpuStats().systemCpuUsage() - stats.precpuStats().systemCpuUsage();
            double allocatedCpuShares = dc.getCpuCores() / dh.getCores();
            double cpuUsage = ((double) cpuDelta / (double) systemDelta) / allocatedCpuShares * 100;

            dcm.setCpuUsage(cpuUsage);
            dcm.setMemoryUsage((stats.memoryStats().usage() / 1024 / 1024));

            if (stats.networks()!=null) {
                dcm.setNetworkDownload((double) stats.networks().get("eth0").rxBytes());
                dcm.setNetworkUpload((double) stats.networks().get("eth0").txBytes());
            } else {
                dcm.setNetworkDownload(-1);
                dcm.setNetworkUpload(-1);
            }

            dcmr.save(dcm);

        } catch (DockerException | InterruptedException e) {
            LOG.error(e.getMessage());
        }
    }
}
