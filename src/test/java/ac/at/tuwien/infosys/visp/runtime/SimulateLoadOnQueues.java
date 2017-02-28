package ac.at.tuwien.infosys.visp.runtime;


import ac.at.tuwien.infosys.visp.runtime.topology.TopologyManagement;
import ac.at.tuwien.infosys.visp.common.Message;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource(locations = "classpath:application.properties")
public class SimulateLoadOnQueues {

    @Autowired
    private TopologyManagement topologyMgmt;

    @Value("${visp.infrastructurehost}")
    private String infrastructureHost;

    @Test
    public void messageSender() {
        topologyMgmt.reset(infrastructureHost);

        for (String operator : topologyMgmt.getOperatorsAsList()) {

            //sendMessage(operatorType, 0);
            //sendMessage(operatorType, 1);
            //sendMessage(operatorType, 10);
            //sendMessage(operatorType, 100);
        }
    }


    public void sendMessage(String queue, Integer amount) {

        Message msg = new Message("dummyMessage");

        CachingConnectionFactory connectionFactory = new CachingConnectionFactory(infrastructureHost);

        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setRoutingKey(queue);
        template.setQueue(queue);

        for (int i = 0; i < amount; i++) {
            template.convertAndSend(queue, queue, msg);
        }

        connectionFactory.destroy();
    }
}
