package ac.at.tuwien.infosys.visp.runtime.monitoring;

import ac.at.tuwien.infosys.visp.runtime.datasources.ApplicationQoSMetricsRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.ApplicationQoSMetrics;
import ac.at.tuwien.infosys.visp.common.ApplicationQoSMetricsMessage;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

/**
 * The ApplicationMonitor retrieves the application-related
 * metrics.
 * <p>
 * These metrics are published on a message queue service
 * (RabbitMQ) by a topology operatorType (class: MonitorOperator)
 * using a push-approach.
 * <p>
 * When a new sample is available, the Runtime-ApplicationMonitor
 * retrieves it and stores it in the ApplicationQoSMetricsRepository.
 */
@Service
@DependsOn("utilities")
public class ApplicationMonitor {

    @Autowired
    private ApplicationQoSMetricsRepository appMetricsRepository;

    @RabbitListener(queues = "applicationmetrics")
    public void assign(ApplicationQoSMetricsMessage message) throws InterruptedException {

        if (!Double.isNaN(message.getAverageResponseTime())) {

            ApplicationQoSMetrics data = new ApplicationQoSMetrics(message.getTimestamp(), message.getApplicationName(), message.getAverageResponseTime());
            appMetricsRepository.save(data);

        }
    }
}
