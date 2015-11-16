package at.tuwien.infosys.monitoring;

import at.tuwien.infosys.entities.ScalingAction;
import at.tuwien.infosys.processingNodeDeployment.OpenstackVmManagement;
import at.tuwien.infosys.topology.TopologyManagement;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
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

    private static final Logger LOG = LoggerFactory.getLogger(OpenstackVmManagement.class);

    @Autowired
    private TopologyManagement topologyMgmt;

    public ScalingAction analyze(String operator) {
        List<String> queues = topologyMgmt.getIncomingQueuesAsList(operator);

        Integer max = 0;
        Integer min = 0;

        for (String queue : queues) {
            Integer queueCount = getQueueCount(queue);
            if (queueCount<min) {
                min = queueCount;
            }

            if (queueCount>max) {
                max = queueCount;
            }
        }

        //TODO log data to a mysql database on the host

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

    private Integer getQueueCount(final String name) {

        CachingConnectionFactory connectionFactory = new CachingConnectionFactory("localhost");
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);

        AMQP.Queue.DeclareOk declareOk = admin.getRabbitTemplate().execute(new ChannelCallback<AMQP.Queue.DeclareOk>() {
            public AMQP.Queue.DeclareOk doInRabbit(Channel channel) throws Exception {
                return channel.queueDeclarePassive(name);
            }
        });
        Integer queueLoad = declareOk.getMessageCount();
        LOG.info("Current load for queue: " + name + " is " + queueLoad);

        connectionFactory.destroy();

        return queueLoad;
    }
}
