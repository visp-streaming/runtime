package at.tuwien.infosys.reasoner.rl.internal;

import java.util.List;

import reled.model.Operator;
import reled.model.Resource;
import reled.model.ResourcePool;
import at.tuwien.infosys.entities.DockerContainer;
import at.tuwien.infosys.entities.OperatorQoSMetrics;

/**
 * OperatorModelBuilder create an operator model (instance of 
 * Operator) that is managed by the reinforcement learning 
 * reasoner
 */
public class OperatorModelBuilder {

	public static Operator create(String operatorName, OperatorQoSMetrics qosMetrics, List<DockerContainer> containers){

		Operator operator = new Operator(operatorName);
		
		/* Retrieve Containers where the operator replica are instantiated */
		ResourcePool resourcePool = new ResourcePool();
		if (!(containers == null || containers.isEmpty())){
			for (DockerContainer container : containers){
				Resource resource = new Resource(container.getContainerid(), container.getCpuUsage());
				resourcePool.addResource(resource);
			}
		}
		operator.setDeployment(resourcePool);
		
		/* Retrieve QoS Metrics related to the operator */
		operator.setProcessedMessagesPerUnitTime(qosMetrics.getProcessedMessages());
		operator.setReceivedMessagesPerUnitTime(qosMetrics.getReceivedMessages());
	    	
		return operator;
	}
	
}
