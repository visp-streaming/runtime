package at.tuwien.infosys.topology;


import at.tuwien.infosys.entities.operators.Operator;
import at.tuwien.infosys.entities.operators.ProcessingOperator;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

@Service
public class TopologyManagement {

    @Value("${spring.rabbitmq.username}")
    private String rabbitmqUsername;

    @Value("${spring.rabbitmq.password}")
    private String rabbitmqPassword;

    @Autowired
    private TopologyParser parser;

    private static final Logger LOG = LoggerFactory.getLogger(TopologyManagement.class);

    public void createMapping(String infrastructureHost) {
        try {

            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(infrastructureHost);
            factory.setUsername(rabbitmqUsername);
            factory.setPassword(rabbitmqPassword);
            Connection connection = null;
            connection = factory.newConnection();
            Channel channel = connection.createChannel();


            //declare error message channel
            channel.exchangeDeclare("error", "fanout", true);
            channel.queueDeclare("error", true, false, false, null);
            channel.queueBind("error", "error", "error");

            channel.exchangeDeclare("processingduration", "fanout", true);
            channel.queueDeclare("processingduration", true, false, false, null);
            channel.queueBind("processingduration", "processingduration", "processingduration");

            for (Operator n : parser.getTopology().values()) {
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

    public void reset(String infrastructureHost) {
        cleanup(infrastructureHost);
        createMapping(infrastructureHost);
    }

    public void cleanup(String infrastructureHost) {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(infrastructureHost);
            factory.setUsername(rabbitmqUsername);
            factory.setPassword(rabbitmqPassword);
            Connection connection = null;
            connection = factory.newConnection();
            Channel channel = connection.createChannel();

            for (Operator n : parser.getTopology().values()) {
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
        for (Operator op : parser.getTopology().values()) {
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
        for (Operator op : parser.getTopology().values()) {
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
        for (Operator op : parser.getTopology().values()) {
            operators.add(op.getName());
        }
        return operators;
    }

    public String getSpecificValueForProcessingOperator(String operator, String key) {
        for (Operator op : parser.getTopology().values()) {
            if (op instanceof ProcessingOperator) {
                if (op.getName().equals(operator)) {
                    ProcessingOperator pcOp = (ProcessingOperator) op;
                    switch (key) {
                        case "expectedDuration":
                            return pcOp.getExpectedDuration();
                        case "queueThreshold":
                            return pcOp.getQueueThreshold();
                        default:
                            throw new RuntimeException("value for key: " + key + " could not be found for operator: " + operator);
                    }
                }
            }
        }
        LOG.error("value for key: " + key + " could not be found for operator: " + operator);
        return "500";
    }

}
