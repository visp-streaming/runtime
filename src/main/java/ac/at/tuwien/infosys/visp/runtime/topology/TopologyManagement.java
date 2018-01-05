package ac.at.tuwien.infosys.visp.runtime.topology;


import ac.at.tuwien.infosys.visp.common.operators.*;
import ac.at.tuwien.infosys.visp.runtime.configuration.Configurationprovider;
import ac.at.tuwien.infosys.visp.runtime.datasources.RuntimeConfigurationRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.VISPInstanceRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.RuntimeConfiguration;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.VISPInstance;
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
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Service
@DependsOn("configurationprovider")
public class TopologyManagement {
    /**
     * this class is used to manage the topology of a specific VISP runtime
     */

    @Value("${spring.rabbitmq.username}")
    private String rabbitmqUsername;

    @Value("${spring.rabbitmq.password}")
    private String rabbitmqPassword;

    @Autowired
    private RuntimeConfigurationRepository rcr;

    @Autowired
    private TopologyParser topologyParser;

    @Autowired
    private TopologyUpdateHandler topologyUpdateHandler;

    @Autowired
    private VISPInstanceRepository vir;

    @Autowired
    private Configurationprovider config;

    @Autowired
    private RabbitMqManager rabbitMqManager;

    private String dotFile;

    private Map<String, Operator> topology = new LinkedHashMap<>();

    private Map<String, Boolean> operatorDeployments = new HashMap<>();

    private static final Logger LOG = LoggerFactory.getLogger(TopologyManagement.class);

    private Map<String, String> activePaths = new HashMap<>();

    private Map<String, Long> lastProbing = new HashMap<>();


    /**
     * path to the deploymentfile that will be executed after the deployment has been tested
     */
    private File testDeploymentFile;
    private int testDeploymentHash;

    public Map<String, Operator> getTopology() {
        return topology;
    }

    public void setTopology(Map<String, Operator> topology) {
        this.topology = topology;
    }

    public void createMapping(String infrastructureHost) {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(infrastructureHost);
            factory.setUsername(rabbitmqUsername);
            factory.setPassword(rabbitmqPassword);
            Connection connection;
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

            channel.close();
            connection.close();

        } catch (IOException | TimeoutException e) {
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
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();

            for (Operator n : topology.values()) {
                channel.exchangeDelete(n.getName());

                if (n.getSources() != null) {
                    for (Operator source : n.getSources()) {
                        String queueName = RabbitMqManager.getQueueName(infrastructureHost, source.getName(), n.getName());
                        LOG.debug("Deleting queue " + queueName);
                        channel.queueDelete(queueName);
                    }
                }
            }

            channel.close();
            connection.close();

        } catch (IOException | TimeoutException e) {
            LOG.error("Could not create mapping.", e);
        }
    }

    public String getIncomingQueues(String operator) {
        return Joiner.on('_').join(getIncomingQueuesAsList(operator));
    }

    public List<String> getIncomingQueuesAsList(String operator) {
        // TODO: differentiate between processing operators and join/split

        List<String> incomingQueues = new ArrayList<>();
        Operator op = topology.get(operator);
        if(op.getSources() != null) {
            for(Operator source: op.getSources()) {
                if(source instanceof Split || source instanceof Join) {
                    // take "grandchildren" as sources
                    for(Operator grandchild : source.getSources()) {
                        incomingQueues.add(RabbitMqManager.getQueueName(grandchild.getConcreteLocation().getIpAddress(), grandchild.getName(), op.getName()));
                    }
                } else {
                    incomingQueues.add(RabbitMqManager.getQueueName(source.getConcreteLocation().getIpAddress(), source.getName(), op.getName()));
                }
            }
        }
        return incomingQueues;
    }

    public List<String> getOperatorsAsList() {
        List<String> operators = new ArrayList<>();
        for (Operator op : topology.values()) {
            if (op instanceof ProcessingOperator || op instanceof Source || op instanceof Sink) {
                operators.add(op.getName());
            }
        }
        return operators;
    }

    public List<String> getProcessingOperatorsAsList() {
        List<String> operators = new ArrayList<>();
        for (Operator op : topology.values()) {
            if (op instanceof ProcessingOperator) {
                operators.add(op.getName());
            }
        }
        return operators;
    }

    public List<Operator> getOperators() {
        List<Operator> operators = new ArrayList<>();
        for (Operator op : topology.values()) {
            if (op instanceof ProcessingOperator || op instanceof Source || op instanceof Sink) {
                operators.add(op);
            }
        }
        return operators;
    }

    public List<Operator> getOperatorsForAConcreteLocation(String location) {
        List<Operator> operators = new ArrayList<>();
        for (Operator op : topology.values()) {
            if (!(op instanceof ProcessingOperator || op instanceof Source || op instanceof Sink)) {
                continue;
            }
            if (op.getConcreteLocation().getIpAddress().equals(location)) {
                operators.add(op);
            }
        }
        return operators;
    }

    public Operator getOperatorByIdentifier(String identifier) {
        return topology.get(identifier);
    }

    public String getDownstreamOperators(String operator) {
        return Joiner.on(',').join(getDownstreamOperatorsAsList(topology.get(operator)));

    }

    public List<Operator> getDownstreamOperatorsAsList(Operator operator, boolean ignoreSplitJoin) {
        List<Operator> ops = new ArrayList<>();

        for (Operator n : topology.values()) {
            if (n.getSources() == null)
                continue;

            for (Operator source : n.getSources()) {
                if (source.getName().equals(operator.getName())) {
                    /*   Must treat split differently - each outgoing path's first operator is added
                     */
                    if(ignoreSplitJoin) {
                        if (n instanceof Split) {
                            for (String splitChild : ((Split) n).getPathOrder()) {
                                ops.add(topology.get(splitChild));
                            }
                        } else if (n instanceof Join) {
                            ops.addAll(getDownstreamOperatorsAsList(n));
                        } else {
                            ops.add(n);
                        }
                    } else {
                        ops.add(n);
                    }
                }
            }
        }

        return ops;
    }

    public List<Operator> getDownstreamOperatorsAsList(Operator operator) {
        return getDownstreamOperatorsAsList(operator, true);
    }

    public String getDotFile() {
        return dotFile;
    }


    public void setDotFile(String dotFile) {
        this.dotFile = dotFile;
    }

    public void saveTestDeploymentFile(File topologyFile, int hashcode) {
        /**
         * this method is called by the handling of a REST-call coming from another VISP instance.
         * It stores the topology-file for later deployment
         */

        LOG.info("Saved test deployment file: ");
        String result;
        try {
            if (topologyFile.exists()) {
                result = new String(Files.readAllBytes(topologyFile.toPath()));
                LOG.info(result);
            }
        } catch (IOException e) {
            LOG.error(e.getLocalizedMessage());
        }

        this.testDeploymentFile = topologyFile;
        this.testDeploymentHash = hashcode;
    }

    public File getTestDeploymentFile() {
        return testDeploymentFile;
    }

    public int getTestDeploymentHash() {
        return testDeploymentHash;
    }

    public boolean restoreTopologyFromPeers() {

        List<VISPInstance> allVispInstances = (List<VISPInstance>) vir.findAll();

        if (allVispInstances.size() == 0) {
            LOG.warn("Could not restore topology from peers because no peers known");
            return false;
        }

        LOG.debug("Restoring topology from peers - still knowing of " + allVispInstances.size() + " VISP instances...");


        for (VISPInstance instance : allVispInstances) {
            if (instance.getIp().equals(config.getRuntimeIP())) {
                continue;
            }
            try {

                if (!InetAddress.getByName(instance.getIp()).isReachable(5000)) {
                    LOG.warn("VISP Instance " + instance.getIp() + " could not be used to get topology (probably offline)");
                    continue;
                }

                RestTemplate restTemplate = new RestTemplate();
                String url = "http://" + instance.getIp() + ":8080/getTopology";
                LOG.debug("Trying to retrieve topology from VISP instance " + instance.getIp() + "...");
                String topologyContent = restTemplate.getForObject(url, String.class);
                if (topologyContent == null || topologyContent.equals("")) {
                    continue;
                }

                TopologyParser.ParseResult pr = topologyParser.parseTopologyFromString(topologyContent);
                this.topology = pr.topology;
                this.setDotFile(pr.dotFile);
                LOG.info("Successfully retrieved topology from VISP instance " + instance.getIp());
                return true;
            } catch (Exception e) {
                LOG.warn("VISP Instance " + instance.getIp() + " could not be used to get topology (probably offline)");
            }
        }

        LOG.warn("Could not restore topology from peers");
        return false;
    }

    public boolean restoreTopologyFromDatabase() {
        RuntimeConfiguration rc = rcr.findFirstByKey("last_topology_file");

        if (rc == null) {
            return false;
        }

        String topologyContent = rc.getValue();

        if (topologyContent == null || topologyContent.equals("")) {
            LOG.warn("Retreived empty topology file from database");
            return false;
        }

        TopologyParser.ParseResult pr = topologyParser.parseTopologyFromString(topologyContent);
        this.topology = pr.topology;
        this.setDotFile(pr.dotFile);

        return true;
    }

    /**
     * Find the split operator that manages operator's fallback path
     * @param operator
     * @return Returns the parent split operator of operator if operator is part of a fallback path and null otherwise
     */
    public Split getParentSplitOperator(Operator operator) {
        Queue<String> queue = new LinkedList<>();
        queue.add(operator.getName());
        while (!queue.isEmpty()) {
            String currentOpId = queue.poll();
            Operator currentOp = this.getTopology().get(currentOpId);
            if(currentOp instanceof Join) {
                // since no nested split/joins are allowed, this means the operator is not part of split/join
                return null;
            }
            if (currentOp instanceof Split) {
                LOG.info("Returning parent split " + currentOp.getName());
                return (Split) currentOp;
            }
            for (Operator s : currentOp.getSources()) {
                queue.add(s.getName());
            }
        }
        return null;
    }

    public void setOperatorDeploymentStatus(String operatorId, boolean isPhysicallyDeployed) {
        if(this.operatorDeployments.containsKey(operatorId)) {
            this.operatorDeployments.replace(operatorId, isPhysicallyDeployed);
        } else {
            this.operatorDeployments.put(operatorId, isPhysicallyDeployed);
        }
    }

    public boolean getOperatorDeploymentStatus(String operatorId) {
        if(this.operatorDeployments.containsKey(operatorId)) {
            return this.operatorDeployments.get(operatorId);
        } else {
            return false;
        }
    }

    /**
     * Updates the currently active fallback path for split operator splitId
     * @param splitId
     * @param activeId
     */
    public void setActiveAlternativePath(String splitId, String activeId) {
        for(Operator splitSource : ((Split) getTopology().get(splitId)).getSources()) {
            // for each source of the split operator, update the message flow
            try {
                rabbitMqManager.activateAlternativePath(splitSource.getName(), activeId, splitSource.getConcreteLocation().getIpAddress());
                if(!activePaths.containsKey(splitId)) {
                    activePaths.put(splitId, activeId);
                } else {
                    activePaths.replace(splitId, activeId);
                }
            } catch (Exception e) {
                LOG.error("Could not modify split message flow", e);
            }

            // and immediately afterwards, disable the forwarding to the previously active paths
            for(String fallbackAlternative : ((Split) getTopology().get(splitId)).getPathOrder()) {
                if(fallbackAlternative.equals(activeId)) {
                    continue;
                }
                try {
                    rabbitMqManager.deactivateAlternativePath(splitSource.getName(), fallbackAlternative, splitSource.getConcreteLocation().getIpAddress());
                } catch (Exception e) {
                    LOG.error("Could not modify split message flow", e);
                }
            }


        }

    }

    /**
     * Temporarily activate the message flow to an operator
     * @param splitId
     * @param pathId
     */
    public void probe(String splitId, String pathId) {
        final int sleepTime = 15000; // millis
        final long timeBetweenProbings = 15000; // millis

        if(!lastProbing.containsKey(pathId)) {
            lastProbing.put(pathId, System.currentTimeMillis());
        } else {
            Long lastProbingAttempt = lastProbing.get(pathId);
            if(System.currentTimeMillis() - lastProbingAttempt < timeBetweenProbings) {
                LOG.warn("Will not attempt probing again - cooloff period not reached");
                return;
            } else {
                lastProbing.replace(pathId, System.currentTimeMillis());
            }
        }


        LOG.info("Start probing for operator " + pathId);

        for(Operator splitSource : ((Split) getTopology().get(splitId)).getSources()) {
            // for each source of the split operator, update the message flow
            try {
                rabbitMqManager.activateAlternativePath(splitSource.getName(), pathId, splitSource.getConcreteLocation().getIpAddress());
            } catch (Exception e) {
                LOG.error("Could not modify split message flow", e);
            }
        }

        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            LOG.error("Timer error", e);
        }

        for(Operator splitSource : ((Split) getTopology().get(splitId)).getSources()) {
            // for each source of the split operator, update the message flow
            try {
                rabbitMqManager.deactivateAlternativePath(splitSource.getName(), pathId, splitSource.getConcreteLocation().getIpAddress());
            } catch (Exception e) {
                LOG.error("Could not modify split message flow", e);
            }
        }

        LOG.info("End probing for operator " + pathId);

    }

    public int getQueueSize(String queueName) throws IOException, TimeoutException {
        return rabbitMqManager.getQueueSize(queueName);
    }

    public String getLastQueueOfPath(String pathId) {
        Operator firstInPath = topology.get(pathId);
        // assume path is linear

        Operator currentOp, nextOp;
        currentOp = firstInPath;
        while(true) {
            nextOp = getDownstreamOperatorsAsList(currentOp, false).get(0);
            if(nextOp instanceof Join) {
                break;
            }
            currentOp = nextOp;
        }
        Operator afterJoin = getDownstreamOperatorsAsList(nextOp).get(0);
        if(afterJoin instanceof Split) {
            throw new RuntimeException("Unimplemented case where split immediately follows join");
        }
        return RabbitMqManager.getQueueName(currentOp.getConcreteLocation().getIpAddress().toString(), currentOp.getName(), afterJoin.getName());
    }

    public boolean isActiveAlternativePath(String splitId, String activeId) {
        if(activePaths.containsKey(splitId)) {
            return activePaths.get(splitId).equals(activeId);
        } else {
            return (((Split) getTopology().get(splitId)).getPathOrder().get(0)).equals(activeId);
        }
    }
}
