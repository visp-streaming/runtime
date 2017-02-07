package ac.at.tuwien.infosys.visp.runtime.reasoner.rl.internal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import reled.ReLEDParamConstants;
import reled.ReLEDParameters;
import reled.ReLEDParametersFactory;

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

    @Value("${reasoner.r-learning.eligibilitytraces}")
    private boolean rlEligibility;

    @Value("${reasoner.r-learning.eligibilitytraces.gamma}")
    private double rlGamma;

    private ReLEDParameters params = null;
    
//    @Value("${reasoner.r-learning.tao}")
//    private double rlTao;
    
    
    public RLParameterManager() {
	}
    
    public ReLEDParameters getReLEDParameters(){

    	if (params != null)
    		return params;
    	
    	params = ReLEDParametersFactory.createParameters();

    	params.set(ReLEDParamConstants.ALPHA, 	new Double(rlAlpha));
		params.set(ReLEDParamConstants.LAMBDA, 	new Double(rlLambda));
		params.set(ReLEDParamConstants.EPSILON, new Double(rlEpsilon));
		
		params.set(ReLEDParamConstants.USE_ELIGIBILITY_TRACES, new Boolean(rlEligibility));
		params.set(ReLEDParamConstants.GAMMA, new Double(rlGamma));
    	
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
