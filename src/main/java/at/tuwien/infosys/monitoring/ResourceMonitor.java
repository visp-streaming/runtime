package at.tuwien.infosys.monitoring;

import at.tuwien.infosys.datasources.DockerContainerRepository;
import at.tuwien.infosys.datasources.DockerHostRepository;
import at.tuwien.infosys.entities.DockerContainer;
import at.tuwien.infosys.entities.DockerHost;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ContainerStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.List;

/**
 * ResourceMonitor monitors the computing resources, 
 * which are used to execute the processing nodes. 
 * 
 * In this implementation, ResourceMonitor retrieves 
 * the CPU utilization of each DockerContainer 
 * and stores this information in the Runtime repository. 
 */
@Service
public class ResourceMonitor {

    @Autowired
    private DockerContainerRepository dcr;

    @Autowired
    private DockerHostRepository dhr;

    private static final String CONNECTION_PROTOCOL = "http://";
    private static final String CONNECTION_PORT = ":2375";
    
    private static final Logger LOG = LoggerFactory.getLogger(ResourceMonitor.class);

    @Scheduled(fixedRateString = "${visp.monitor.period}")
    public void updateAllHostsCpuUtilization(){
		Iterator<DockerHost> hosts = dhr.findAll().iterator();
    	
    	while(hosts.hasNext()){
    		DockerHost dockerHost = hosts.next();
    		updateCpuUtilization(dockerHost);
    	}
    }
    
    public void updateCpuUtilization(DockerHost dh){
    	List<DockerContainer> hostedContainers = dcr.findByHost(dh.getName());
    	
    	for (DockerContainer container : hostedContainers){
    		retrieveCpuUtilization(dh, container);
    	}
    	dcr.save(hostedContainers);
    }
    
    
    /**
     * This function changes the information within the docker container
     * passed as argument.
     * 
     * @param dockerHost
     * @param container
     * @return
     */
    private DockerContainer retrieveCpuUtilization(DockerHost dockerHost, DockerContainer container){
		String connectionUri = CONNECTION_PROTOCOL + dockerHost.getUrl() + CONNECTION_PORT;
        final DockerClient docker = DefaultDockerClient.builder().uri(connectionUri).connectTimeoutMillis(60000).build();
        ContainerStats stats;

        try {
			stats = docker.stats(container.getContainerid());
		
	        if (container.getPreviousSystemUsage() == 0 && container.getPreviousCpuUsage() == 0){
	        	
	        	LOG.debug("Container " + container.getContainerid() + " first computation of CPU Utilization");
	        	
	        	container.setPreviousCpuUsage(stats.cpuStats().cpuUsage().totalUsage());
	        	container.setPreviousSystemUsage(stats.cpuStats().systemCpuUsage());
	        	container.setCpuUsage(0);
				container.setPreviousMemoryUsage(stats.memoryStats().usage());
				container.setMemoryUsage(0);

	        } else {

	            double cpuUsage = 0.0;
	            long cpuDelta = 0;
	            long systemDelta = 0;
	            
	            /* Calculate the change of container's usage in between readings */
	            long currentCpuUsage = stats.cpuStats().cpuUsage().totalUsage();
	            long currentSystemUsage = stats.cpuStats().systemCpuUsage();
	            cpuDelta = currentCpuUsage - container.getPreviousCpuUsage();
	        	systemDelta = currentSystemUsage - container.getPreviousSystemUsage();


	        	if (systemDelta > 0 && cpuDelta > 0) {
	            	/* This information should be scaled with respect to the CPU share */
	                double allocatedCpuShares = container.getCpuCores() / dockerHost.getCores();

	                cpuUsage = ((double) cpuDelta / (double) systemDelta) / allocatedCpuShares; // * 100.0;

		        	LOG.debug("Container " + container.getContainerid() + " CPU Utilization: " 
		        			+ cpuUsage + " (allocatedShares: " + allocatedCpuShares + ")");
		        	
	                container.setCpuUsage(cpuUsage);
	                container.setPreviousCpuUsage(currentCpuUsage);
	                container.setPreviousSystemUsage(currentSystemUsage);

					container.setPreviousMemoryUsage(container.getPreviousMemoryUsage());
					container.setMemoryUsage(stats.memoryStats().usage());

				}

	        }
		
		} catch (DockerException | InterruptedException e) {
			LOG.error(e.getMessage());
		}      

        return container;
    }
}
