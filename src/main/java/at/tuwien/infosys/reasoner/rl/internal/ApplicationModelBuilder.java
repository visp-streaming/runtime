package at.tuwien.infosys.reasoner.rl.internal;

import reled.model.Application;
import at.tuwien.infosys.entities.ApplicationQoSMetrics;

/**
 * ApplicatioModelBuilder translates the application in a model
 * (instance of Application) that is managed by the reinforcement
 * learning reasoner
 */
public class ApplicationModelBuilder {

	public static Application create(ApplicationQoSMetrics qosMetrics){
			
    	Application application = new Application(qosMetrics.getApplicationName());
    	application.setResponseTime(qosMetrics.getAverageResponseTime());

    	return application;
	    	
	}
	
}
