package at.tuwien.infosys;


import at.tuwien.infosys.topology.TopologyManagement;
import entities.Message;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = VISPRuntime.class)
@TestPropertySource(locations="classpath:application.properties")
public class SimulateLoadOnQueues {

    @Autowired
    private TopologyManagement topologyMgmt;

    @Value("${visp.infrastructurehost}")
    private String infrastructureHost;

    @Test
    public void messageSender() {
        topologyMgmt.reset(infrastructureHost);

        for (String operator : topologyMgmt.getOperatorsAsList()) {

            sendMessage(operator, 0);
            //sendMessage(operator, 1);
            //sendMessage(operator, 10);
            //sendMessage(operator, 100);
        }
    }


    public void sendMessage(String queue, Integer amount) {

        Message msg = new Message("dummyMessage");

        CachingConnectionFactory connectionFactory = new CachingConnectionFactory(infrastructureHost);

        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setRoutingKey(queue);
        template.setQueue(queue);

        for (int i = 0; i<amount; i++) {
            template.convertAndSend(queue, queue, msg);
        }

        connectionFactory.destroy();
    }
}
