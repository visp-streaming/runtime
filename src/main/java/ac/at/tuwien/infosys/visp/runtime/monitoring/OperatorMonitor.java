package ac.at.tuwien.infosys.visp.runtime.monitoring;

import ac.at.tuwien.infosys.visp.common.ProcessingNodeMetricsMessage;
import ac.at.tuwien.infosys.visp.runtime.datasources.DockerContainerRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.DockerHostRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.OperatorQoSMetricsRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.DockerContainer;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.DockerHost;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.OperatorQoSMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * The OperatorMonitor retrieves the statistics related
 * to each operatorType of the topology.
 * <p>
 * It needs to collect the sum of emitted events and the
 * amount of processed event by each operatorType instance.
 * Afterwards, aggregates the statistics per operatorType.
 * <p>
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
    private OperatorQoSMetricsRepository operatorQosRepository;

    @Value("${visp.dataprovider.host}")
    private String dataProviderHost;

    @Value("${visp.dataprovider.port}")
    private String dataProviderPort;


    private static final String CONNECTION_PROTOCOL = "http://";
    private static final String MONITOR_ENTRYPOINT = "/metrics";

    private static final Logger LOG = LoggerFactory.getLogger(OperatorMonitor.class);

    private long lastUpdate = 0;

    @Scheduled(fixedRateString = "${visp.monitor.period}")
    public synchronized void retrieveOperatorsMetricsFromAllContainers() {

        if (lastUpdate == 0) {
            lastUpdate = System.currentTimeMillis();
            return;
        }

        Iterable<DockerContainer> hostedContainers = dcr.findAll();
        Map<String, Long> receivedPerOperator = new HashMap<>();
        Map<String, Long> processedPerOperator = new HashMap<>();
        List<ProcessingNodeMetricsMessage> stats = new ArrayList<>();

    	/* Collect stats from all docker containers */
        long now = System.currentTimeMillis();
        for (DockerContainer container : hostedContainers) {

            ProcessingNodeMetricsMessage message = null;

                DockerHost host = dhr.findFirstByName(container.getHost());
                if (host == null)
                    continue;
                String hostUrl = host.getUrl();

                String url = CONNECTION_PROTOCOL + hostUrl + ":" + container.getMonitoringPort() + MONITOR_ENTRYPOINT;
                RestTemplate restTemplate = new RestTemplate();
                try {
                    message = restTemplate.getForObject(url, ProcessingNodeMetricsMessage.class);
                } catch (ResourceAccessException e) {
                    LOG.error("Could not access metrics endpoint " + e.getLocalizedMessage());
                }

            if (message != null) {
                stats.add(message);
            }
        }

    	/* Add also data source container */
    	/* TODO: Decouple data source from VISP-Runtime parameters */
        /*ProcessingNodeMetricsMessage srcStats = getStatsFromDataSource();
        if (srcStats != null)
            stats.add(srcStats);
    	*/
    	/* Process collected stats to obtain: 
    	 *  - processed messages per operatorType
    	 *  - received messages per operatorType
    	 *  
    	 *  Note that a container runs an operatorType instance
    	 */
        for (ProcessingNodeMetricsMessage msg : stats) {

            Map<String, Long> emittedByOpInstance = msg.getEmittedMessages();
            Map<String, Long> processedByOpInstance = msg.getProcessedMessages();
    		
    		/* Update Processed */
            for (String operatorName : processedByOpInstance.keySet()) {
                Long processedCount = processedPerOperator.get(operatorName);
                if (processedCount == null) {
                    processedCount = processedByOpInstance.get(operatorName);
                } else {
                    processedCount = processedCount + processedByOpInstance.get(operatorName);
                }
                processedPerOperator.put(operatorName, processedCount);
            }

    		/* Update Received */
            for (String destination : emittedByOpInstance.keySet()) {
                Long receivedCount = receivedPerOperator.get(destination);
                if (receivedCount == null) {
                    receivedCount = emittedByOpInstance.get(destination);
                } else {
                    receivedCount = receivedCount + emittedByOpInstance.get(destination);
                }
                receivedPerOperator.put(destination, receivedCount);
            }

        }

    	/* Save (append) information on the repository */
        Set<String> allOperators = new HashSet<>();
        allOperators.addAll(processedPerOperator.keySet());
        allOperators.addAll(receivedPerOperator.keySet());
        long delta = (now - lastUpdate);
        // delta is divided by 1000 to obtain msg/s (instead of msg/ms)
        for (String operatorName : processedPerOperator.keySet()) {

            OperatorQoSMetrics operator = new OperatorQoSMetrics();
            operator.setName(operatorName);
            operator.setTimestamp(Long.toString(now));

            double msgProcPerUnitTime = 0;
            if (processedPerOperator.get(operatorName) != null)
                msgProcPerUnitTime = (double) processedPerOperator.get(operatorName) * 1000 / (double) delta;
            operator.setProcessedMessages(msgProcPerUnitTime);

            double msgRecvPerUnitTime = 0;
            if (receivedPerOperator.get(operatorName) != null)
                msgRecvPerUnitTime = (double) receivedPerOperator.get(operatorName) * 1000 / (double) delta;
            operator.setReceivedMessages(msgRecvPerUnitTime);

            operatorQosRepository.save(operator);

            allOperators.remove(operatorName);
        }
        for (String operatorName : allOperators) {

            OperatorQoSMetrics operator = new OperatorQoSMetrics();
            operator.setName(operatorName);
            operator.setTimestamp(Long.toString(now));

            double msgProcPerUnitTime = 0;
            operator.setProcessedMessages(msgProcPerUnitTime);

            double msgRecvPerUnitTime = 0;
            if (receivedPerOperator.get(operatorName) != null)
                msgRecvPerUnitTime = (double) receivedPerOperator.get(operatorName) * 1000 / (double) delta;
            operator.setReceivedMessages(msgRecvPerUnitTime);

            operatorQosRepository.save(operator);

        }
        lastUpdate = now;

    }

    private ProcessingNodeMetricsMessage getStatsFromDataSource() {

        ProcessingNodeMetricsMessage message = null;
        try {
            String url = CONNECTION_PROTOCOL + dataProviderHost + ":" + dataProviderPort + MONITOR_ENTRYPOINT;
            RestTemplate restTemplate = new RestTemplate();
            message = restTemplate.getForObject(url, ProcessingNodeMetricsMessage.class);
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }

        return message;

    }
}
