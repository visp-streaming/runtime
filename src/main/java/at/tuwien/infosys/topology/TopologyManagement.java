package at.tuwien.infosys.topology;


import at.tuwien.infosys.configuration.Topology;
import at.tuwien.infosys.entities.Operator;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

@Service
public class TopologyManagement {

    @Autowired
    private Topology topology;

    private static final Logger LOG = LoggerFactory.getLogger(TopologyManagement.class);

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
            LOG.error("Could not cleanup topology.", e);
        } catch (TimeoutException e) {
            LOG.error("Could not cleanup topology.", e);
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
            LOG.error("Could not create mapping.", e);
        } catch (TimeoutException e) {
            LOG.error("Could not create mapping.", e);
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

    public List<String> getIncomingQueuesAsList(String operator) {
        List<String> incomingQueues = new ArrayList<>();
        for (Operator op : topology.getTopologyAsList()) {
            if (op.getName().equals(operator)) {
                if (op.getSources()!=null) {
                    for (Operator source : op.getSources()) {
                        incomingQueues.add(op.getName() + source.getName());
                    }
                }
            }
        }
        return incomingQueues;
    }

    public List<String> getOperatorsAsList() {
        List<String> operators = new ArrayList<>();
        for (Operator op : topology.getTopologyAsList()) {
            operators.add(op.getName());
        }
        return operators;
    }

}
