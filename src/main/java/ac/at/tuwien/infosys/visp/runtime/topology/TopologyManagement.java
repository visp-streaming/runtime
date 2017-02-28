package ac.at.tuwien.infosys.visp.runtime.topology;


import ac.at.tuwien.infosys.visp.common.operators.Operator;
import ac.at.tuwien.infosys.visp.runtime.topology.rabbitMq.RabbitMqManager;
import ac.at.tuwien.infosys.visp.topologyParser.TopologyParser;
import com.google.common.base.Joiner;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

            // TODO: all these exchanges and queues must be created at the appropriate rabbitmq hosts

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
            
            /* Declare Management Message Channels */
            channel.exchangeDeclare("applicationmetrics", "fanout", true);
            channel.queueDeclare("applicationmetrics", true, false, false, null);
            channel.queueBind("applicationmetrics", "applicationmetrics", "applicationmetrics");
            

            for (Operator n : parser.getTopology().values()) {
                String exchangeName = n.getName();
                channel.exchangeDeclare(exchangeName, "fanout", true);

                if (n.getSources()!=null) {
                    for (Operator source : n.getSources()) {
                        String queueName = RabbitMqManager.getQueueName(source.getConcreteLocation().getIpAddress(), source.getName(), exchangeName);
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

                if (n.getSources() != null) {
                    for (Operator source : n.getSources()) {
                        String queueName = RabbitMqManager.getQueueName(infrastructureHost, source.getName(), n.getName());
                        LOG.info("Deleting queue " + queueName);
                        channel.queueDelete(queueName);
                    }
                }
            }

            channel.close();
            connection.close();

        } catch (IOException e) {
            LOG.error("Could not create mapping.", e);
        } catch (TimeoutException e) {
            LOG.error("Could not create mapping.", e);
        } catch (Exception e) {
            LOG.error(e.getLocalizedMessage());
        }
    }

    public String getIncomingQueues(String operator) {
        StringBuilder incomingQueues = new StringBuilder();
        for (Operator op : parser.getTopology().values()) {
            if (op.getName().equals(operator)) {
                if (op.getSources()!=null) {
                    for (Operator source : op.getSources()) {
                        incomingQueues.append(RabbitMqManager.getQueueName(source.getConcreteLocation().getIpAddress(), source.getName(), op.getName())).append("_");
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
                        incomingQueues.add(RabbitMqManager.getQueueName(source.getConcreteLocation().getIpAddress(), source.getName(), op.getName()));
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

    public List<Operator> getOperators() {
        List<Operator> operators = new ArrayList<>();
        for (Operator op : parser.getTopology().values()) {
            operators.add(op);
        }
        return operators;
    }

    public List<Operator> getOperatorsForAConcreteLocation(String location) {
        List<Operator> operators = new ArrayList<>();
        for (Operator op : parser.getTopology().values()) {
            if (op.getConcreteLocation().getIpAddress().equals(location)) {
                operators.add(op);
            }
        }
        return operators;
    }

    public Operator getOperatorByIdentifier(String identifier) {
        return parser.getTopology().get(identifier);
    }
    
    public String getDownstreamOperators(String operator){
    
    	Set<String> ops = new HashSet<String>();
    	
        for (Operator n : parser.getTopology().values()) {

        	if (n.getSources() == null)
            	continue;
        	
            for (Operator source : n.getSources()) {
            	if (source.getName().equals(operator)){
            		ops.add(n.getName());
            	}
            }
        }

        return Joiner.on(',').join(ops);
   
    }
}
