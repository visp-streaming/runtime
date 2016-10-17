package at.tuwien.infosys.monitoring;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import at.tuwien.infosys.datasources.ApplicationQoSMetricsRepository;
import at.tuwien.infosys.entities.ApplicationQoSMetrics;
import entities.ApplicationQoSMetricsMessage;

/**
 * The ApplicationMonitor retrieves the application-related
 * metrics.
 * 
 * These metrics are published on a message queue service 
 * (RabbitMQ) by a topology operator (class: MonitorOperator)
 * using a push-approach. 
 * 
 * When a new sample is available, the Runtime-ApplicationMonitor
 * retrieves it and stores it in the ApplicationQoSMetricsRepository.
 */
@Service
@DependsOn("utilities")
public class ApplicationMonitor {

	@Autowired
	private ApplicationQoSMetricsRepository appMetricsRepository;

//    private static final Logger LOG = LoggerFactory.getLogger(ApplicationMonitor.class);

	@RabbitListener(queues = "applicationmetrics")
	public void assign(ApplicationQoSMetricsMessage message) throws InterruptedException {

//		LOG.info("Application Monitor: received " + message.getTimestamp() + 
//				" (a:" + message.getApplicationName() + ")" + 
//				"avgResp=" + message.getAverageResponseTime());
		
		if (!Double.isNaN(message.getAverageResponseTime())){

			ApplicationQoSMetrics data = new ApplicationQoSMetrics(message.getTimestamp(), message.getApplicationName(), message.getAverageResponseTime());
			appMetricsRepository.save(data);
			
		}
	}
}
