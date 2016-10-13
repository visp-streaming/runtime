package at.tuwien.infosys.reasoner.rl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import reled.RLController;
import reled.ReLEDParameters;
import reled.learning.entity.Action;
import reled.learning.entity.ActionAvailable;
import reled.model.Application;
import reled.model.ApplicationSLA;
import reled.model.Operator;
import at.tuwien.infosys.configuration.OperatorConfiguration;
import at.tuwien.infosys.datasources.ApplicationQoSMetricsRepository;
import at.tuwien.infosys.datasources.DockerContainerRepository;
import at.tuwien.infosys.datasources.DockerHostRepository;
import at.tuwien.infosys.datasources.OperatorQoSMetricsRepository;
import at.tuwien.infosys.datasources.ScalingActivityRepository;
import at.tuwien.infosys.entities.ApplicationQoSMetrics;
import at.tuwien.infosys.entities.DockerContainer;
import at.tuwien.infosys.entities.DockerHost;
import at.tuwien.infosys.entities.OperatorQoSMetrics;
import at.tuwien.infosys.entities.ResourceAvailability;
import at.tuwien.infosys.entities.ScalingActivity;
import at.tuwien.infosys.monitoring.AvailabilityWatchdog;
import at.tuwien.infosys.reasoner.ReasonerUtility;
import at.tuwien.infosys.reasoner.rl.internal.ApplicationModelBuilder;
import at.tuwien.infosys.reasoner.rl.internal.LeastLoadedHostFirstComparator;
import at.tuwien.infosys.reasoner.rl.internal.LoadBalancingPlacementStrategy;
import at.tuwien.infosys.reasoner.rl.internal.OperatorModelBuilder;
import at.tuwien.infosys.reasoner.rl.internal.PlacementStrategy;
import at.tuwien.infosys.reasoner.rl.internal.RLParameterManager;
import at.tuwien.infosys.reasoner.rl.internal.SortedList;
import at.tuwien.infosys.resourceManagement.ProcessingNodeManagement;
import at.tuwien.infosys.resourceManagement.ResourceProvider;
import at.tuwien.infosys.topology.TopologyManagement;

@Service
public class CentralizedRLReasoner {
    
	/* *** Constants 				*** */
	/* Currently, VISP-Runtime manages a single application */
	public static final String APPNAME = "default";
	public static final int FIRST = 0;

	/* *** Auto-wired Dependencies 	*** */
	@Autowired
	private TopologyManagement topologyManager;
	
    @Autowired
    private OperatorConfiguration operatorConfig;
    
    @Autowired
    private ProcessingNodeManagement procNodeManager;
    
    @Autowired
    private DockerContainerRepository dockerRepository;
    
    @Autowired
    private DockerHostRepository dockerHostRepository;
    
    @Autowired
    private ApplicationQoSMetricsRepository applicationMetricsRepository;
    
    @Autowired
    private OperatorQoSMetricsRepository operatorMetricsRepository;
    
    @Autowired
    private RLParameterManager paramManager;

    @Autowired
    private ResourceProvider resourceProvider;
    
    @Autowired
    private AvailabilityWatchdog availabilityWatchdog;

    @Autowired
    private ReasonerUtility reasonerUtility;

    @Autowired
    private ScalingActivityRepository scalingActivityRepository;
    
    @Value("${visp.infrastructurehost}")
    private String infrastructureHost;

    private static final Logger LOG = LoggerFactory.getLogger(CentralizedRLReasoner.class);

    /* *** Internal attributes 		*** */
    private Application application; 
    private ApplicationSLA applicationSla; 
	private Map<String, RLController> controller;
	private Map<String, Operator> operatorNameToOperator;

	public CentralizedRLReasoner() {
	}
	
	public void initialize(){

		/* Centralized Version of ReLED Controller */
		controller = new HashMap<String, RLController>();
		operatorNameToOperator = new HashMap<String, Operator>();
				
		/* Generate a new RL Controller for each operator */
		createRLController();
	
		/* Retrieve the application SLA */
		createApplicationSla();
		
	}
	

	private void createRLController(){
		
		/* Create Parameters for RL Controllers */
		ReLEDParameters params = paramManager.getReLEDParameters();
		
		/* Create RL Controllers */
		for (String operator : topologyManager.getOperatorsAsList()){
			controller.put(operator, new RLController(params));
		}
		
	}
	private void createApplicationSla(){
		
		/* Collect Application QoS Requirements */
		applicationSla = new ApplicationSLA(APPNAME);
		applicationSla.setMaxResponseTime(paramManager.getMaxResponseTime());
		applicationSla.setMaxUtilization(paramManager.getMaxUtilization());
		applicationSla.setMinThroughtput(paramManager.getMinThroughput());
		
	}
	
	public void runAdaptationCycle(){
				
		/* Terminate container and hosts that have been flagged 
		 * to shutdown in the previous adaptation cycles */
		terminateFlaggedContainersAndHosts();
		
		consolidateContainers();
		
		/* Execute MAPE control cycle */
		try {

			monitor();

			/* Run -APE steps for each operator (in a concurrent way) */
			for (String operatorName : topologyManager.getOperatorsAsList()) {
				try{
					
					Action action = analyzeAndPlan(operatorName);
					execute(operatorName, action);
			
				}catch(RLException apeExc){
					LOG.error(apeExc.getMessage());
					apeExc.printStackTrace();
				}
			}
			
		} catch (RLException rle) {
			LOG.error(rle.getMessage());
			rle.printStackTrace();
		}

	}
	
	
	private void terminateFlaggedContainersAndHosts(){
		availabilityWatchdog.checkAvailablitiyOfContainer();
		procNodeManager.removeContainerWhichAreFlaggedToShutdown();
		resourceProvider.get().removeHostsWhichAreFlaggedToShutdown();
	}
	
	/**
	 * Retrieve all metrics related to the operators
	 * @throws Exception 
	 */
	private void monitor() throws RLException{

		/* Create Application from monitored data:
		 *  - (end-to-end) average response time */
		application = createApplicationModel();
		if (application == null)
			throw new RLException("Application Model not available");
		
		
		/* Create Operator from monitored data: 
		 * 	- Operator: receivedMsg/s; processedMsg/s 
		 *  - Resources: averageCpuUsage
		 *  	- Resource: containerId, cpuUsage
		 */
		List<String> operatorNames = topologyManager.getOperatorsAsList();
		if (operatorNames == null || operatorNames.isEmpty())
			throw new RLException("Operator Model not available");
		
		for (String operatorName : topologyManager.getOperatorsAsList()) {
			Operator operator = createOperatorModel(operatorName);
			operatorNameToOperator.put(operatorName, operator);
		}

	}
	
	/**
	 * Analyze monitored metrics and plan next action for the operator node
	 * @param operatorName
	 * @return
	 * @throws Exception
	 */
	private Action analyzeAndPlan(String operatorName) throws RLException{
		
		RLController operatorController = controller.get(operatorName);

		Operator operator = operatorNameToOperator.get(operatorName);
		
		if (operatorController == null || application == null || operator == null)
			throw new RLException("Invalid operator information (controller, application, operator)");
		
		Action action = operatorController.nextAction(application, operator, applicationSla);

		return action; 
		
	}
	
	/**
	 * Enact the planned action to the execution environment.
	 * 
	 * If a new docker container has to be instantiated, first try to 
	 * use available resources (hosts) by balancing the number of containers
	 * per host; if no available resource is found, a new one is launched. 
	 * 
	 * @param operatorName
	 * @param action
	 * @throws Exception
	 */
	private void execute(String operatorName, Action action){
		
		ActionAvailable decodedAction = action.getAction();
		
		switch(decodedAction){
			case SCALE_IN:
				/* Scale-in the number of operator replicas by stopping a 
				 * (randomly chosen) processing node (Docker container) */
				procNodeManager.scaleDown(operatorName);
				
				/* Track scaling activity */
				/* Action already tracked in procNodeManager.scaleDown() */
				
				/* In this step containers are flagged as "stopping";
				 * the consolidation of containers on available hosts is
				 * postponed to next execution of the adaptation cycle */
				break;
				
			case NO_ACTIONS:
				break;
			
			case SCALE_OUT:
				/* Scale-out the number of operator replicas by executing the steps: 
				 * - create a new docker container
				 * - determine the container placement (existing nodes, new node) 
				 * - launch the container */
				DockerContainer container = operatorConfig.createDockerContainerConfiguration(operatorName);
				DockerHost host = determineContainerPlacement(container);
				procNodeManager.scaleup(container, host, infrastructureHost);

				/* Track scaling activity */
				/* Action already tracked in procNodeManager.scaleUp() */
				break;
		}
		

	}
	
	/**
	 * Determine the container placement (existing nodes or a new one) 
	 * @param DockerContainer
	 * @return DockerHost
	 */
    private synchronized DockerHost determineContainerPlacement(DockerContainer container) {
    	
    	DockerHost candidateHost = null;
    	
    	/* Compute resource availability on each running host */
        List<ResourceAvailability> availableResources = reasonerUtility.calculateFreeResourcesforHosts(candidateHost);

    	/* Try to use an already running host */
        candidateHost = computePlacementWithLoadBalancing(container, availableResources);

    	/* Start a new VM */
        if (candidateHost == null) {
        	candidateHost = resourceProvider.createContainerSkeleton(); 
            return resourceProvider.get().startVM(candidateHost);
        } 

        return candidateHost;
        
    }
    
    
    /**
     * If possible, consolidate running containers 
     * on fewer computing resources. 
     */
    private void consolidateContainers(){
    	
    	/* Avoid turning off the only available host */
    	if (dockerHostRepository.count() <= 1)
    		return;
    	
    	/* Compute resource availability on each running host */
        List<ResourceAvailability> availableResources = reasonerUtility.calculateFreeResourcesforHosts(null);

    	/* Sort hosts in decreasing order w.r.t. the number of hosted containers */
    	SortedList<ResourceAvailability> sortedResources = new SortedList<ResourceAvailability>(new LeastLoadedHostFirstComparator());
    	sortedResources.addAll(availableResources);

		/* Determine host to be turned off and its container relocation */
    	DockerHost hostToTurnOff = null;
    	Map<DockerContainer, DockerHost> relocationMap = null;
    	for(ResourceAvailability resource : sortedResources){

    		/* Check if host (resource) can be turned off and its container safely relocated */
    		relocationMap = reasonerUtility.canRelocateHostedContainers(resource, sortedResources);
    		if (relocationMap != null){
    			hostToTurnOff = resource.getHost();
    			
    			/* if at least a host can be released, stop consolidation */
    	    	break;
    		}
    	}
    	
    	/* Execute relocation, if suitable */
    	if (relocationMap == null)
    		return;

    	for(DockerContainer container : relocationMap.keySet()){
    		
    		/* Relocate Container */
    		DockerHost destinationHost = relocationMap.get(container);
    		procNodeManager.triggerShutdown(container);
    		procNodeManager.scaleup(container, destinationHost, infrastructureHost);
    		
    		/* Track consolidation activity */
    		scalingActivityRepository.save(new ScalingActivity("container", 
    				new DateTime(DateTimeZone.UTC).toString(), container.getOperator(), 
    							"consolidation", container.getHost()));

    	}
    	
    	/* Release resource */
    	resourceProvider.get().markHostForRemoval(hostToTurnOff);
                	
    }

    private Application createApplicationModel(){
    	
    	List<ApplicationQoSMetrics> metrics = applicationMetricsRepository.findFirstByApplicationNameOrderByTimestampDesc(APPNAME);
    	if (metrics == null || metrics.isEmpty()){
    		return null;
    	}
    	ApplicationQoSMetrics qosMetrics = metrics.get(FIRST);
    	
    	return ApplicationModelBuilder.create(qosMetrics);
    	
    }
    
 	private Operator createOperatorModel(String operatorName){

		List<OperatorQoSMetrics> operatorsMetrics = operatorMetricsRepository.findFirstByNameOrderByTimestampDesc(operatorName);
		List<DockerContainer> containers = dockerRepository.findByOperator(operatorName);

	 	if (operatorsMetrics == null || operatorsMetrics.isEmpty()){
	 		return null;
	 	}
	 	OperatorQoSMetrics qosMetrics = operatorsMetrics.get(FIRST);
	 	
	 	return OperatorModelBuilder.create(operatorName, qosMetrics, containers);
	 	
	 }
 	

    private DockerHost computePlacementWithLoadBalancing(DockerContainer container, List<ResourceAvailability> availableResources) {
    	
    	PlacementStrategy ps = new LoadBalancingPlacementStrategy();
    	return ps.computePlacement(container, availableResources);
    	
    }
	 
 
}
