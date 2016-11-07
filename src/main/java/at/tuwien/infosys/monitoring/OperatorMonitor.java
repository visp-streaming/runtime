package at.tuwien.infosys.monitoring;

import at.tuwien.infosys.datasources.DockerContainerRepository;
import at.tuwien.infosys.datasources.DockerHostRepository;
import at.tuwien.infosys.datasources.OperatorQoSMetricsRepository;
import at.tuwien.infosys.entities.DockerContainer;
import at.tuwien.infosys.entities.DockerHost;
import at.tuwien.infosys.entities.OperatorQoSMetrics;
import entities.ProcessingNodeMetricsMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * The OperatorMonitor retrieves the statistics related
 * to each operator of the topology. 
 * 
 * It needs to collect the sum of emitted events and the 
 * amount of processed event by each operator instance. 
 * Afterwards, aggregates the statistics per operator. 
 * 
 * Note: to avoid collecting data from multiple or not consistent
 * window, this components adopts a PULL-approach.
 */
@Service
public class OperatorMonitor {

	@Autowired
    private DockerContainerRepository dcr;

    @Autowired
    private DockerHostRepository dhr;
    
    @Autowired
    private OperatorQoSMetricsRepository operatorRepository;
	
    @Value("${visp.dataprovider.host}")
    private String dataProviderHost; 
    
    @Value("${visp.dataprovider.port}")
    private String dataProviderPort; 
    
    
    private static final String CONNECTION_PROTOCOL = "http://";
    private static final String MONITOR_ENTRYPOINT 	= "/metrics";
    
//    private static final Logger LOG = LoggerFactory.getLogger(ResourceMonitor.class);

    private long lastUpdate = 0;
    
	@Scheduled(fixedRateString = "${visp.monitor.period}")
	public synchronized void retrieveOperatorsMetricsFromAllContainers(){
	
		if (lastUpdate == 0){
			lastUpdate = System.currentTimeMillis();
			return;
		}
		
    	Iterable<DockerContainer> hostedContainers = dcr.findAll();    	
    	Map<String, Long> receivedPerOperator = new HashMap<String, Long>();
    	Map<String, Long> processedPerOperator = new HashMap<String, Long>();
    	List<ProcessingNodeMetricsMessage> stats = new ArrayList<ProcessingNodeMetricsMessage>();

    	/* Collect stats from all docker containers */
		long now = System.currentTimeMillis();		
    	for (DockerContainer container : hostedContainers){
    	
    		ProcessingNodeMetricsMessage message = null;
    		
    		try{
    			
    			DockerHost host = dhr.findFirstByName(container.getHost());
    			if (host == null)
    				continue;
    			String hostUrl = host.getUrl();

    			String url = CONNECTION_PROTOCOL + hostUrl + ":" + container.getMonitoringPort() + MONITOR_ENTRYPOINT;
        		RestTemplate restTemplate = new RestTemplate();
        		message = restTemplate.getForObject(url, ProcessingNodeMetricsMessage.class);
    		
    		} catch (Exception e){

			}

    		if (message != null){
    			stats.add(message);
    		}
    	}
    	
    	/* Add also data source container */
    	/* TODO: Decouple data source from VISP-Runtime parameters */
    	ProcessingNodeMetricsMessage srcStats = getStatsFromDataSource(); 
    	if (srcStats != null)
    		stats.add(srcStats);
    	
    	/* Process collected stats to obtain: 
    	 *  - processed messages per operator
    	 *  - received messages per operator
    	 *  
    	 *  Note that a container runs an operator instance
    	 */
    	for(ProcessingNodeMetricsMessage msg : stats){

    		Map<String, Long> emittedByOpInstance = msg.getEmittedMessages();
    		Map<String, Long> processedByOpInstance  = msg.getProcessedMessages();
    		
    		/* Update Processed */
    		for(String operatorName : processedByOpInstance.keySet()){
        		Long processedCount = processedPerOperator.get(operatorName);
        		if (processedCount == null)
        			processedCount = new Long(processedByOpInstance.get(operatorName));
        		else
        			processedCount = new Long(processedCount.longValue() + processedByOpInstance.get(operatorName));
        		processedPerOperator.put(operatorName, processedCount);
    		}

    		/* Update Received */
    		for(String destination : emittedByOpInstance.keySet()){
    			Long receivedCount = receivedPerOperator.get(destination);
        		if (receivedCount == null)
        			receivedCount = new Long(emittedByOpInstance.get(destination));
        		else
        			receivedCount = new Long(receivedCount.longValue() + emittedByOpInstance.get(destination));
        		receivedPerOperator.put(destination, receivedCount);
    		}

    	}

    	/* Save (append) information on the repository */
    	Set<String> allOperators = new HashSet<String>();
    	allOperators.addAll(processedPerOperator.keySet());
    	allOperators.addAll(receivedPerOperator.keySet());
    	long delta = (now - lastUpdate);
    	// delta is divided by 1000 to obtain msg/s (instead of msg/ms)
    	for(String operatorName : processedPerOperator.keySet()){

    		OperatorQoSMetrics operator = new OperatorQoSMetrics();
    		operator.setName(operatorName);
    		operator.setTimestamp(Long.toString(now));

    		double msgProcPerUnitTime = 0;
    		if(processedPerOperator.get(operatorName) != null)
    			msgProcPerUnitTime = (double) processedPerOperator.get(operatorName) * 1000 / (double) delta;
    		operator.setProcessedMessages(msgProcPerUnitTime);
        	
    		double msgRecvPerUnitTime = 0;
    		if(receivedPerOperator.get(operatorName) != null)
    			msgRecvPerUnitTime = (double) receivedPerOperator.get(operatorName)  * 1000 / (double) delta;
        	operator.setReceivedMessages(msgRecvPerUnitTime);

        	operatorRepository.save(operator);
        	
        	allOperators.remove(operatorName);
    	}
    	for(String operatorName : allOperators){

    		OperatorQoSMetrics operator = new OperatorQoSMetrics();
    		operator.setName(operatorName);
    		operator.setTimestamp(Long.toString(now));

    		double msgProcPerUnitTime = 0;
    		operator.setProcessedMessages(msgProcPerUnitTime);
        	
    		double msgRecvPerUnitTime = 0;
    		if(receivedPerOperator.get(operatorName) != null)
    			msgRecvPerUnitTime = (double) receivedPerOperator.get(operatorName)  * 1000 / (double) delta;
        	operator.setReceivedMessages(msgRecvPerUnitTime);
        	
        	operatorRepository.save(operator);

    	}
    	lastUpdate = now;

    }
    
	private ProcessingNodeMetricsMessage getStatsFromDataSource(){
		
		ProcessingNodeMetricsMessage message = null;
		try{
			String url = CONNECTION_PROTOCOL + dataProviderHost + ":" + dataProviderPort + MONITOR_ENTRYPOINT;
			RestTemplate restTemplate = new RestTemplate();
			message = restTemplate.getForObject(url, ProcessingNodeMetricsMessage.class);
		}catch(Exception e){}
		
		return message; 
	
	}
}
