package at.tuwien.infosys.monitoring;

import at.tuwien.infosys.datasources.ProcessingDurationRepository;
import at.tuwien.infosys.datasources.QueueMonitorRepository;
import at.tuwien.infosys.entities.ProcessingDuration;
import at.tuwien.infosys.entities.QueueMonitor;
import at.tuwien.infosys.entities.ScalingAction;
import at.tuwien.infosys.resourceManagement.OpenstackConnector;
import at.tuwien.infosys.topology.TopologyManagement;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.ChannelCallback;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class Monitor {

    private static final Logger LOG = LoggerFactory.getLogger(OpenstackConnector.class);

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
            if (queueCount<min) {
                min = queueCount;
            }

            if (queueCount>max) {
                max = queueCount;
            }

            qmr.save(new QueueMonitor(new DateTime(DateTimeZone.UTC).toString(), operator, queue, queueCount));
        }


        List <ProcessingDuration> pds = pcr.findFirst5ByOperatorOrderByIdDesc("aaa");

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
        //TODO implement logic to handle this




        if (max<1) {
            return ScalingAction.SCALEDOWN;
        } else {
            if (max<100) {
                return ScalingAction.DONOTHING;
            } else {
                if (min>250) {
                    return ScalingAction.SCALEUPDOUBLE;
                } else {
                    return ScalingAction.SCALEUP;
                }
            }
        }
    }

    private Integer getQueueCount(final String name, String infrastructureHost) {

        CachingConnectionFactory connectionFactory = new CachingConnectionFactory(infrastructureHost);
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);

        Integer queueLoad = 0;
        try {
            AMQP.Queue.DeclareOk declareOk = admin.getRabbitTemplate().execute(new ChannelCallback<AMQP.Queue.DeclareOk>() {
                public AMQP.Queue.DeclareOk doInRabbit(Channel channel) throws Exception {
                    return channel.queueDeclarePassive(name);
                }
            });
            queueLoad = declareOk.getMessageCount();
            LOG.info("Current load for queue: " + name + " is " + queueLoad);
        } catch (Exception e) {
            LOG.warn("Queue \"" + name + "\" is not yet available.");
        }

        connectionFactory.destroy();

        return queueLoad;
    }
}
