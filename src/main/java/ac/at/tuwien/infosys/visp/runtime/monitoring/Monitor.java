package ac.at.tuwien.infosys.visp.runtime.monitoring;

import ac.at.tuwien.infosys.visp.common.operators.ProcessingOperator;
import ac.at.tuwien.infosys.visp.runtime.datasources.QueueMonitorRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.ProcessingDuration;
import ac.at.tuwien.infosys.visp.runtime.entities.ScalingAction;
import ac.at.tuwien.infosys.visp.runtime.resourceManagement.OpenstackConnector;
import ac.at.tuwien.infosys.visp.runtime.topology.TopologyManagement;
import ac.at.tuwien.infosys.visp.runtime.datasources.ProcessingDurationRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.QueueMonitor;
import com.rabbitmq.client.AMQP;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class Monitor {

    private static final Logger LOG = LoggerFactory.getLogger(OpenstackConnector.class);

    @Value("${spring.rabbitmq.username}")
    private String rabbitmqUsername;

    @Value("${spring.rabbitmq.password}")
    private String rabbitmqPassword;

    @Value("${visp.relaxationfactor}")
    private Double relaxationfactor;

    @Value("${visp.minimalqueueloadforupscaling}")
    private Integer queueUpscalingThreshold;

    @Autowired
    private ProcessingDurationRepository pcr;

    @Autowired
    private QueueMonitorRepository qmr;

    @Autowired
    private TopologyManagement topologyMgmt;

    public ScalingAction analyze(String operator, String infrastructureHost) {
        List<String> queues = topologyMgmt.getIncomingQueuesAsList(operator);

        Integer max = 0;
        Integer min = 0;

        for (String queue : queues) {
            Integer queueCount = getQueueCount(queue, infrastructureHost);
            if (queueCount < min) {
                min = queueCount;
            }

            if (queueCount > max) {
                max = queueCount;
            }

            qmr.save(new QueueMonitor(new DateTime(DateTimeZone.UTC), operator, queue, queueCount));
        }

        return upscalingDuration(operator, max);
    }
    
    
    public void saveQueueCount(String operator, String infrastructureHost) {

    	List<String> queues = topologyMgmt.getIncomingQueuesAsList(operator);

        for (String queue : queues) {
        
        	Integer queueCount = getQueueCount(queue, infrastructureHost);
            qmr.save(new QueueMonitor(new DateTime(DateTimeZone.UTC), operator, queue, queueCount));
        
        }

    }


    private ScalingAction upscalingDuration(String operator, Integer maxQueue) {
        List<ProcessingDuration> pds = pcr.findFirst5ByOperatorOrderByIdDesc(operator);

        if (pds.isEmpty()) {
            if (operator.contains("source")) {
                return ScalingAction.DONOTHING;
            }

            return ScalingAction.DONOTHING;
        }


        if (pds.get(0).getDuration() * relaxationfactor > ((ProcessingOperator) topologyMgmt.getOperatorByIdentifier(operator)).getExpectedDuration()) {
            if (maxQueue > queueUpscalingThreshold) {
                return ScalingAction.SCALEUP;
            }

        }

        Integer count = 4;
        double[][] data = new double[5][2];
        for (ProcessingDuration pd : pds) {
            data[count][0] = count;
            data[count][1] = pd.getDuration();
            count--;
        }

        SimpleRegression regression = new SimpleRegression(false);
        regression.addData(data);

        Double expectedDurationValue = regression.predict(6);

        if (expectedDurationValue * relaxationfactor > ((ProcessingOperator) topologyMgmt.getOperatorByIdentifier(operator)).getExpectedDuration()) {
            if (maxQueue > queueUpscalingThreshold) {
                return ScalingAction.SCALEUP;
            }
        }

        return ScalingAction.DONOTHING;
    }

    private ScalingAction upscalingQueue(String operator, Integer max) {
        if (max > ((ProcessingOperator) topologyMgmt.getOperatorByIdentifier(operator)).getQueueThreshold()) {
            return ScalingAction.SCALEUP;
        }

        List<QueueMonitor> qms = qmr.findFirst5ByOperatorOrderByIdDesc(operator);

        Integer count = 4;
        double[][] data = new double[5][2];
        for (QueueMonitor qm : qms) {
            data[count][0] = count;
            data[count][1] = qm.getAmount();
            count--;
        }

        SimpleRegression regression = new SimpleRegression(false);
        regression.addData(data);

        Double expectedDurationValue = regression.predict(6);

        if (expectedDurationValue > ((ProcessingOperator) topologyMgmt.getOperatorByIdentifier(operator)).getQueueThreshold()) {
            return ScalingAction.SCALEUP;
        }

        return ScalingAction.DONOTHING;
    }

    
    
    private Integer getQueueCount(final String name, String infrastructureHost) {

        CachingConnectionFactory connectionFactory = new CachingConnectionFactory(infrastructureHost);
        connectionFactory.setUsername(rabbitmqUsername);
        connectionFactory.setPassword(rabbitmqPassword);

        RabbitAdmin admin = new RabbitAdmin(connectionFactory);

        Integer queueLoad = 0;
        try {
            AMQP.Queue.DeclareOk declareOk = admin.getRabbitTemplate().execute(channel -> channel.queueDeclarePassive(name));
            queueLoad = declareOk.getMessageCount();
            LOG.info("Current load for queue: " + name + " is " + queueLoad);
        } catch (Exception e) {
            LOG.warn("Queue \"" + name + "\" is not yet available.");
        }

        connectionFactory.destroy();

        return queueLoad;
    }
}
