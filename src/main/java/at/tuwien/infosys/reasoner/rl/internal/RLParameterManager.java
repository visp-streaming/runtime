package at.tuwien.infosys.reasoner.rl.internal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import reled.ReLEDParamConstants;
import reled.ReLEDParameters;

@Service
public class RLParameterManager {

    @Value("${application.sla.utilization.max}")
    private double maxUtilization;

    @Value("${application.sla.responsetime.max}")
    private double maxResponseTime;

    @Value("${application.sla.throughput.min}")
    private double minThroughput;

    @Value("${reasoner.r-learning.alpha}")
    private double rlAlpha;

    @Value("${reasoner.r-learning.lambda}")
    private double rlLambda;

    @Value("${reasoner.r-learning.epsilon}")
    private double rlEpsilon;

    private ReLEDParameters params = null;
    
//    @Value("${reasoner.r-learning.tao}")
//    private double rlTao;
    
    
    public RLParameterManager() {
	}
    
    public ReLEDParameters getReLEDParameters(){

    	if (params != null)
    		return params;
    	
    	params = new ReLEDParameters();

    	params.set(ReLEDParamConstants.ALPHA, 	new Double(rlAlpha));
		params.set(ReLEDParamConstants.LAMBDA, 	new Double(rlLambda));
		params.set(ReLEDParamConstants.EPSILON, new Double(rlEpsilon));
    	
//		params.set(ReLEDParamConstants.ALPHA, 	new Double(0.1));
//		params.set(ReLEDParamConstants.LAMBDA, 	new Double(0.1));
//		params.set(ReLEDParamConstants.EPSILON, new Double(0.1));
//		params.set(ReLEDParamConstants.TAO, 	new Double(0.5));

		return params;
		
    }

	public double getMaxUtilization() {
		return maxUtilization;
	}

	public double getMaxResponseTime() {
		return maxResponseTime;
	}

	public double getMinThroughput() {
		return minThroughput;
	}

}
