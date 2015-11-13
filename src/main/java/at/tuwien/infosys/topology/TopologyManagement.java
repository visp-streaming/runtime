package at.tuwien.infosys.topology;


import at.tuwien.infosys.configuration.Topology;
import at.tuwien.infosys.entities.Operator;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

@Service
public class TopologyManagement {

    @Autowired
    private Topology topology;

    public void createMapping(String infrastructureHost) {
        try {

            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(infrastructureHost);
            Connection connection = null;
            connection = factory.newConnection();
            Channel channel = connection.createChannel();

            for (Operator n : topology.getTopologyAsList()) {
                String exchangeName = n.getName();
                channel.exchangeDeclare(exchangeName, "fanout", true);

                if (n.getSources()!=null) {
                    for (Operator source : n.getSources()) {
                        String queueName = exchangeName + source.getName();
                        channel.queueDeclare(queueName, true, false, false, null);
                        channel.queueBind(queueName, source.getName(), source.getName());
                    }
                }
            }

            channel.close();
            connection.close();

        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }

    public void cleanup(String infrastructureHost) {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(infrastructureHost);
            Connection connection = null;
            connection = factory.newConnection();
            Channel channel = connection.createChannel();

            for (Operator n : topology.getTopologyAsList()) {
                channel.exchangeDelete(n.getName());

                if (n.getSources()!=null) {
                    for (Operator source : n.getSources()) {
                        channel.queueDelete(n.getName() + source.getName());
                    }
                }
            }

            channel.close();
            connection.close();

        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }

    }

    public String getIncomingQueues(String operator) {
        StringBuilder incomingQueues = new StringBuilder();
        for (Operator op : topology.getTopologyAsList()) {
            if (op.getName().equals(operator)) {
                if (op.getSources()!=null) {
                    for (Operator source : op.getSources()) {
                        incomingQueues.append(op.getName() + source.getName()).append("_");
                    }
                }
            }
        }
        return incomingQueues.toString();
    }

}
