package ac.at.tuwien.infosys.visp.runtime.reasoner.rl.internal;

import ac.at.tuwien.infosys.visp.runtime.datasources.DockerContainerMonitorRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.DockerContainer;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.DockerContainerMonitor;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.OperatorQoSMetrics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reled.model.Operator;
import reled.model.Resource;
import reled.model.ResourcePool;

import java.util.List;

/**
 * OperatorModelBuilder create an operatorType model (instance of
 * Operator) that is managed by the reinforcement learning 
 * reasoner
 */

@Service
public class OperatorModelBuilder {

	@Autowired
	private DockerContainerMonitorRepository dcmr;

	public Operator create(String operatorName, OperatorQoSMetrics qosMetrics, List<DockerContainer> containers){

		Operator operator = new Operator(operatorName);
		
		/* Retrieve Containers where the operatorType replica are instantiated */
		ResourcePool resourcePool = new ResourcePool();
		if (!(containers == null || containers.isEmpty())){
			for (DockerContainer container : containers){
				DockerContainerMonitor dcm = dcmr.findFirstByContaineridOrderByTimestampDesc(container.getContainerid());
				Resource resource;
				if (dcm!=null) {
					resource = new Resource(container.getContainerid(), dcm.getCpuUsage());
				} else {
					resource = new Resource(container.getContainerid(), 0.0);
				}

				resourcePool.addResource(resource);
			}
		}
		operator.setDeployment(resourcePool);
		
		/* Retrieve QoS Metrics related to the operatorType */
		operator.setProcessedMessagesPerUnitTime(qosMetrics.getProcessedMessages());
		operator.setReceivedMessagesPerUnitTime(qosMetrics.getReceivedMessages());
	    	
		return operator;
	}
	
}
