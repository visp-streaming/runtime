package ac.at.tuwien.infosys.visp.runtime.topology.rabbitMq;

import ac.at.tuwien.infosys.visp.common.operators.Operator;
import ac.at.tuwien.infosys.visp.common.operators.Source;
import ac.at.tuwien.infosys.visp.runtime.configuration.Configurationprovider;
import ac.at.tuwien.infosys.visp.runtime.datasources.DockerContainerRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.DockerContainer;
import ac.at.tuwien.infosys.visp.runtime.exceptions.ResourceException;
import ac.at.tuwien.infosys.visp.runtime.exceptions.TopologyException;
import ac.at.tuwien.infosys.visp.runtime.reasoner.ReasonerUtility;
import ac.at.tuwien.infosys.visp.runtime.resourceManagement.DockerContainerManagement;
import ac.at.tuwien.infosys.visp.runtime.resourceManagement.ManualOperatorManagement;
import ac.at.tuwien.infosys.visp.runtime.resourceManagement.ProcessingNodeManagement;
import ac.at.tuwien.infosys.visp.runtime.topology.TopologyManagement;
import ac.at.tuwien.infosys.visp.runtime.topology.TopologyUpdate;
import ac.at.tuwien.infosys.visp.runtime.topology.operatorUpdates.SizeUpdate;
import ac.at.tuwien.infosys.visp.runtime.topology.operatorUpdates.SourcesUpdate;
import ac.at.tuwien.infosys.visp.runtime.topology.rabbitMq.entities.QueueResult;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.spotify.docker.client.exceptions.DockerException;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.util.Pair;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;


@Service
@DependsOn("configurationprovider")
public class RabbitMqManager {
    /**
     * This class is used to interact with a specific rabbitmq host
     */

    @Autowired
    private DockerContainerManagement dcm;

    @Autowired
    private DockerContainerRepository dcr;

    @Autowired
    private ManualOperatorManagement rpp;

    @Autowired
    private TopologyManagement topologyManagement;

    @Autowired
    private ReasonerUtility reasonerUtility;

    @Autowired
    private ProcessingNodeManagement pcm;

    @Autowired
    private Configurationprovider config;

    @Value("${spring.rabbitmq.username}")
    private String rabbitmqUsername;

    @Value("${spring.rabbitmq.password}")
    private String rabbitmqPassword;

    private static final Logger LOG = LoggerFactory.getLogger(RabbitMqManager.class);

    public static String getQueueName(String senderHost, String senderOperator, String consumerOperator) {
        // returns the name of the queue that is created for the communication between senderHost and consumerHost
        //return consumerHost + senderHost;
        if (senderOperator.contains("/") || consumerOperator.contains("/") || senderHost.contains("/")) {
            throw new TopologyException("Neither senderOperator, consumerOperator nor senderHost may contain slashes");
        }
        if (senderOperator.contains(">") || consumerOperator.contains(">") || senderHost.contains(">")) {
            throw new TopologyException("Neither senderOperator, consumerOperator nor senderHost may contain greater signs");
        }
        return senderHost + "/" + senderOperator + ">" + consumerOperator;

    }

    private Connection createConnection(String infrastructureHost) throws IOException, TimeoutException {
        LOG.debug("Creating connection to host " + infrastructureHost + " with user " + rabbitmqUsername + " and pw " + rabbitmqPassword);
        if (infrastructureHost.equals(config.getRuntimeIP())) {
            infrastructureHost = config.getInfrastructureIP();
        }
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(infrastructureHost);
        factory.setUsername(rabbitmqUsername);
        factory.setPassword(rabbitmqPassword);
        return factory.newConnection();
    }

    private void declareExchanges(List<TopologyUpdate> updates) throws IOException, TimeoutException {
        if (updates.size() == 0) {
            return;
        }

        Connection connection = createConnection(config.getRuntimeIP());
        Channel fromChannel = connection.createChannel();
        for (TopologyUpdate update : updates) {
            if (!update.getAffectedOperator().getConcreteLocation().getIpAddress().equals(config.getRuntimeIP())) {
                continue;
            }
            String exchangeName = update.getAffectedOperator().getName();
            try {
                fromChannel.exchangeDeclare(exchangeName, "fanout", true);
            } catch (IOException e) {
                LOG.error("Could not declare exchange " + exchangeName);
            }

            LOG.debug("Declaring exchange " + exchangeName + " on host " + config.getRuntimeIP());
        }
        try {
            fromChannel.close();
            connection.close();
        } catch (Exception e) {
            LOG.error("Could not close channel", e);
        }
    }


    private String addMessageFlow(String fromOperatorId, String toOperatorId,
                                 String fromInfrastructureHost) throws IOException, TimeoutException {
        /**
         * add a message flow from operatorType FROM to operatorType TO
         */

        Connection connection = createConnection(config.getRuntimeIP());
        Channel fromChannel = connection.createChannel();
        try {
            fromChannel.exchangeDeclare(fromOperatorId, "fanout", true);
            LOG.debug("Declaring exchange " + fromOperatorId + " on host " + fromInfrastructureHost);

            String queueName = getQueueName(fromInfrastructureHost, fromOperatorId, toOperatorId);

            LOG.debug("Declaring queue " + queueName + " on host " + fromInfrastructureHost);
            fromChannel.queueDeclare(queueName, true, false, false, null);

            // tell exchange to send msgs to queue:
            LOG.debug("Set exchange " + fromOperatorId + " to forward messages to queue " + queueName);
            fromChannel.queueBind(queueName, fromOperatorId, fromOperatorId); // third parameter is ignored in fanout mode

        } catch (Exception e) {
            LOG.error("Exception during exchange setup", e);
        } finally {
            try {
                fromChannel.close();
                connection.close();
            } catch (Exception e) {
                LOG.error("Could not close rabbitmq channel", e);
            }
        }
        return "ADD " + fromInfrastructureHost + "/" + fromOperatorId + ">" + toOperatorId;
    }

    private void sendDockerSignalForUpdate(Operator operator, String updateCommand)  {
        if (operator instanceof Source) {
            return; // source is no actual container
        }
        String toOperatorId = operator.getName();
        Operator.Location location = operator.getConcreteLocation();
        LOG.debug("Searching for containers with operator-name " + toOperatorId + " @ location: " + location);
        List<DockerContainer> dcs = dcr.findAllRunningByOperatorNameAndResourcepool(toOperatorId, location.getResourcePool());
        if (dcs.size() <= 0) {
            LOG.debug("Could not find the right one - but found those containers:");
            for (DockerContainer dc : dcr.findAll()) {
                LOG.debug(dc.toString());
            }
            throw new ResourceException("Could not find docker containers for operator " + toOperatorId);
        }
        LOG.debug("Found " + dcs.size() + " matching containers for operator-name " + toOperatorId);
        for (DockerContainer dc : dcs) {
            LOG.debug("Checking container " + dc + " (Status: " + dc.getStatus() + ")");

            String command = "echo \"" + updateCommand + "\" >> ~/topologyUpdate; touch ~/topologyUpdate";
            LOG.debug("Executing command on dockercontainer " + dc.getContainerid() + ": [" + command + "]");
            try {
                dcm.executeCommand(dc, command, false);
            } catch (DockerException | InterruptedException e) {
                LOG.error("Could not execute command on container " + dc.getContainerid() + ": [" + command + "]");
            }
        }
    }

    private String removeMessageFlow(String fromOperatorId, String toOperatorId,
                                    String fromInfrastructureHost) throws IOException, TimeoutException {

        Connection connection = createConnection(config.getRuntimeIP());
        Channel fromChannel = connection.createChannel();

        try {

            String queueName = getQueueName(fromInfrastructureHost, fromOperatorId, toOperatorId);
            // this stops the exchange sending messages to the queue
            fromChannel.queueUnbind(queueName, fromOperatorId, fromOperatorId);

            LOG.debug("Unbinding queue " + queueName + " on exchange " + fromOperatorId + " on host " + fromInfrastructureHost);

            try {
                fromChannel.queueDelete(queueName);
                LOG.debug("Deleted queue " + queueName + " on host " + fromInfrastructureHost);
            } catch (Exception e) {
                LOG.error("Could not delete queue " + queueName, e);
            }
            return "REMOVE " + fromInfrastructureHost + "/" + fromOperatorId + ">" + toOperatorId;
        } finally {
            try {
                fromChannel.close();
                connection.close();
            } catch (Exception e) {
                LOG.warn("Could not close rabbitmq channel(s)", e);
            }
        }
    }

    public void performUpdates(List<TopologyUpdate> updates) throws IOException, TimeoutException {
        declareExchanges(updates);

        // step 1: update the rabbitmq infrastructure (add and remove queues etc)

        List<Pair<Operator, String>> updateSignals = updateRabbitMqInfrastructure(updates);

        // step 2

        removeOldContainersFromUpdate(updates);
        addNewContainersFromUpdate(updates);

        // step 2.5: scale containers if needed

        rescaleContainers(updates);

        // step 3

        sendContainerUpdateSignals(updateSignals);

        LOG.info("" + updates.size() + " updates were applied");
    }

    @SuppressWarnings("Duplicates")
    private void rescaleContainers(List<TopologyUpdate> updates) {
        /**
         * this method is used to change the number of containers per operator
         * in case of size-updates (e.g. spawn second container when updating
         * from small to medium)
         */

        for (TopologyUpdate update : updates) {
            if (update.getAction().equals(TopologyUpdate.Action.UPDATE_OPERATOR) &&
                    update.getUpdateType().equals(TopologyUpdate.UpdateType.UPDATE_SIZE) &&
                    update.getAffectedOperator().getConcreteLocation().getIpAddress().equals(config.getRuntimeIP())) {

                Operator op = update.getAffectedOperator();
                Operator.Size oldSize = ((SizeUpdate) update.getChangeToBeExecuted()).getOldSize();
                Operator.Size newSize = ((SizeUpdate) update.getChangeToBeExecuted()).getNewSize();
                try {
                    int oldSizeInt = 0;
                    int newSizeInt = 0;
                    switch (oldSize) {
                        case SMALL: oldSizeInt = 1; break;
                        case MEDIUM: oldSizeInt = 2; break;
                        case LARGE: oldSizeInt = 4; break;
                        default: oldSizeInt = 1; break;
                    }
                    switch (newSize) {
                        case SMALL: newSizeInt = 1; break;
                        case MEDIUM: newSizeInt = 2; break;
                        case LARGE: newSizeInt = 4; break;
                        default: newSizeInt = 1; break;
                    }


                    int difference = newSizeInt - oldSizeInt;

                    if (difference > 0) {
                        // scale up:
                        LOG.debug("Scaling up operator " + op.getName() + " with " + difference + " more copies");
                        for (int i = 0; i < difference; i++) {
                            pcm.scaleup(reasonerUtility.selectSuitableDockerHost(op), op);
                        }
                    } else {
                        // scale down:
                        LOG.debug("Scaling down operator " + op.getName() + " with " + (difference * (-1)) + " fewer copies");
                        for (int i = 0; i < difference * (-1); i++) {
                            pcm.scaleDown(op.getName());
                        }
                    }

                } catch (ResourceException e) {
                    LOG.error("Could not rescale operator " + op, e);
                }
            }
        }
    }

    private void sendContainerUpdateSignals(List<Pair<Operator, String>> updateSignals) {
        // make list unique

        List<String> uniques = new ArrayList<>();

        for (Pair<Operator, String> updateSignal : updateSignals) {
            if (uniques.contains(updateSignal.getSecond())) {
                continue;
            }
            try {
                sendDockerSignalForUpdate(updateSignal.getFirst(), updateSignal.getSecond());
                uniques.add(updateSignal.getSecond());
            } catch (Exception e) {
                LOG.error("Exception during sending docker signal for update to operator " + updateSignal.getFirst(), e);
            }
        }
    }

    private void addNewContainersFromUpdate(List<TopologyUpdate> updates) {
        for (TopologyUpdate update : updates) {
            if (update.getAction().equals(TopologyUpdate.Action.ADD_OPERATOR) &&
                    update.getAffectedOperator().getConcreteLocation().getIpAddress().equals(config.getRuntimeIP())) {
                LOG.debug("Spawning operator " + update.getAffectedOperatorId());
                rpp.addOperator(update.getAffectedOperator());
            }
        }
    }

    private void removeOldContainersFromUpdate(List<TopologyUpdate> updates) {
        for (TopologyUpdate update : updates) {
            if (update.getAction().equals(TopologyUpdate.Action.REMOVE_OPERATOR) &&
                    update.getAffectedOperator().getConcreteLocation().getIpAddress().equals(config.getRuntimeIP())) {
                LOG.debug("Removing operator " + update.getAffectedOperatorId());
                rpp.removeOperators(update.getAffectedOperator());
            }
        }
    }

    private List<Pair<Operator, String>> updateRabbitMqInfrastructure(List<TopologyUpdate> updates) {
        List<Pair<Operator, String>> results = new ArrayList<>();
        for (TopologyUpdate update : updates) {
            List<Pair<Operator, String>> resultPairs = null;
            switch (update.getAction()) {
                case UPDATE_OPERATOR:
                    resultPairs = handleRabbitmqUpdateOperator(update);
                    break;
                case ADD_OPERATOR:
                    resultPairs = handleRabbitmqAddOperator(update);
                    break;
                case REMOVE_OPERATOR:
                    resultPairs = handleRabbitmqRemoveOperator(update);
                    break;
                default:
                    LOG.error("Unknown action: " + update.getAction() + " for update " + update.toString());
                    break;
            }
            if (resultPairs != null) {
                results.addAll(resultPairs);
            }
        }
        return results;
    }

    private List<Pair<Operator, String>> handleRabbitmqRemoveOperator(TopologyUpdate update) {
        List<Pair<Operator, String>> resultList = new ArrayList<>();
        Pair<Operator, String> pair;
        if (update.shouldChangeTopologyMessageFlow()) {
            for (Operator source : update.getAffectedOperator().getSources()) {
                if (source.getName().equals(update.getAffectedOperatorId())) {
                    continue;
                }
                if (!source.getConcreteLocation().getIpAddress().equals(config.getRuntimeIP())) {
                    continue;
                }
                LOG.debug("Removing message flow between operators " + source.getName() + " and " + update.getAffectedOperator().getName());
                try {
                    pair = Pair.of(update.getAffectedOperator(),
                            removeMessageFlow(source.getName(), update.getAffectedOperator().getName(),
                                    source.getConcreteLocation().getIpAddress()));
                    if (pair != null) {
                        resultList.add(pair);
                    }
                } catch (IOException | TimeoutException e) {
                    LOG.error(e.getLocalizedMessage());
                }
            }


            for (Operator downstreamOp : topologyManagement.getDownstreamOperatorsAsList(update.getAffectedOperator())) {
                if (downstreamOp.getName().equals(update.getAffectedOperatorId())) {
                    continue;
                }
                if (!update.getAffectedOperator().getConcreteLocation().getIpAddress().equals(config.getRuntimeIP())) {
                    continue;
                }
                LOG.debug("Removing message flow between operators " + update.getAffectedOperator().getName() + " and " + downstreamOp.getName());
                try {
                    pair = Pair.of(update.getAffectedOperator(),
                            removeMessageFlow(update.getAffectedOperator().getName(), downstreamOp.getName(),
                                    update.getAffectedOperator().getConcreteLocation().getIpAddress()));
                    if (pair != null) {
                        resultList.add(pair);
                    }
                } catch (IOException | TimeoutException e) {
                    LOG.error(e.getLocalizedMessage());
                }
            }
        }

        return resultList;
    }

    private List<Pair<Operator, String>> handleRabbitmqAddOperator(TopologyUpdate update) {
        List<Pair<Operator, String>> resultList = new ArrayList<>();
        Pair<Operator, String> pair;
        if (update.shouldChangeTopologyMessageFlow()) {
            for (Operator source : update.getAffectedOperator().getSources()) {
                if (source.getName().equals(update.getAffectedOperatorId())) {
                    continue;
                }
                if (!source.getConcreteLocation().getIpAddress().equals(config.getRuntimeIP())) {
                    continue;
                }
                try {
                    LOG.debug("Adding message flow between operators " + source.getName() + " and " + update.getAffectedOperatorId());
                    pair = Pair.of(update.getAffectedOperator(),
                            addMessageFlow(source.getName(), update.getAffectedOperatorId(), source.getConcreteLocation().getIpAddress()));
                    if (pair != null) {
                        resultList.add(pair);
                    }
                } catch (Exception e) {
                    LOG.error(e.getLocalizedMessage());
                }
            }
            for (Operator downstreamOp : topologyManagement.getDownstreamOperatorsAsList(update.getAffectedOperator())) {
                if (!update.getAffectedOperator().getConcreteLocation().getIpAddress().equals(config.getRuntimeIP())) {
                    continue;
                }
                if (downstreamOp.getName().equals(update.getAffectedOperatorId())) {
                    continue;
                }
                try {
                    LOG.debug("Adding message flow between operators " + update.getAffectedOperatorId() + " and " + downstreamOp.getName());
                    pair = Pair.of(downstreamOp,
                            addMessageFlow(update.getAffectedOperatorId(), downstreamOp.getName(), update.getAffectedOperator().getConcreteLocation().getIpAddress()));
                    if (pair != null) {
                        resultList.add(pair);
                    }
                } catch (Exception e) {
                    LOG.error(e.getLocalizedMessage());
                }
            }
        }

        return resultList;
    }

    private List<Pair<Operator, String>> handleRabbitmqUpdateOperator(TopologyUpdate update) {
        LOG.debug("Handling update operatorType update");
        switch (update.getUpdateType()) {
            case UPDATE_SOURCE:
                try {
                    return handleRabbitmqSourceUpdate(update);
                } catch (Exception e) {
                    LOG.error("Could not update rabbitmq for source update", e);
                }
            case UPDATE_SIZE:
                return null;
            default:
                throw new TopologyException("Update type " + update.getUpdateType().toString() + " not yet implemented");
        }

    }

    private List<Pair<Operator, String>> handleRabbitmqSourceUpdate(TopologyUpdate update) throws IOException, TimeoutException {
        List<Pair<Operator, String>> resultList = new ArrayList<>();
        Pair<Operator, String> pair;
        SourcesUpdate sourcesUpdate = (SourcesUpdate) update.getChangeToBeExecuted();
        // add message flow for new sources:
        List<Operator> newSources = sourcesUpdate.getNewSources();
        List<Operator> oldSources = sourcesUpdate.getOldSources();
        for (Operator newSourceEntry : newSources) {
            if (newSourceEntry.getName().equals(update.getAffectedOperatorId())) {
                continue;
            }
            if (!newSourceEntry.getConcreteLocation().getIpAddress().equals(config.getRuntimeIP())) {
                continue;
            }
            if (!oldSources.contains(newSourceEntry)) {
                // add new flow from the new source to our target operatorType

                LOG.debug("Adding message flow between operators " + newSourceEntry.getName() + " and " + update.getAffectedOperatorId());
                pair = Pair.of(newSourceEntry,
                        addMessageFlow(newSourceEntry.getName(), update.getAffectedOperatorId(),
                                newSourceEntry.getConcreteLocation().getIpAddress()));
                if (pair != null) {
                    resultList.add(pair);
                }
            }
        }

        for (Operator oldSourceEntry : oldSources) {
            if (!newSources.contains(oldSourceEntry)) {
                if (oldSourceEntry.getName().equals(update.getAffectedOperatorId())) {
                    continue;
                }
                if (!oldSourceEntry.getConcreteLocation().getIpAddress().equals(config.getRuntimeIP())) {
                    continue;
                }
                // remove message flow from old source
                try {
                    LOG.debug("Removing message flow between operators " + oldSourceEntry.getName() + " and " + update.getAffectedOperatorId());
                    pair = Pair.of(oldSourceEntry,
                            removeMessageFlow(oldSourceEntry.getName(), update.getAffectedOperatorId(),
                                    oldSourceEntry.getConcreteLocation().getIpAddress()));
                    if (pair != null) {
                        resultList.add(pair);
                    }
                } catch (IOException | TimeoutException e) {
                    LOG.error(e.getLocalizedMessage());
                }
            }
        }

        return resultList;
    }

    public void removeAllQueues() {
        List<String> queuesToIgnore = new ArrayList<>();
        queuesToIgnore.add("applicationmetrics");
        queuesToIgnore.add("error");
        queuesToIgnore.add("processingduration");

        // basic http auth:

        String plainCreds = rabbitmqUsername + ":" + rabbitmqPassword;
        byte[] plainCredsBytes = plainCreds.getBytes();
        byte[] base64CredsBytes = Base64.encodeBase64(plainCredsBytes);
        String base64Creds = new String(base64CredsBytes);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Basic " + base64Creds);

        Channel channel = null;
        Connection connection = null;
        try {
            String url = "http://" + config.getInfrastructureIP() + ":15672/api/queues";

            RestTemplate restTemplate = new RestTemplate();
            headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));

            HttpEntity<String> entity = new HttpEntity<>("", headers);

            ResponseEntity<QueueResult[]> response = restTemplate.exchange(url, HttpMethod.GET, entity, QueueResult[].class);

            connection = createConnection(config.getRuntimeIP());
            channel = connection.createChannel();

            for (QueueResult queueResult : response.getBody()) {
                if (queuesToIgnore.contains(queueResult.getName())) {
                    continue;
                }
                // remove the queue
                channel.queueDelete(queueResult.getName());
                LOG.debug("Deleted queue " + queueResult.getName());
            }
        } catch (IOException | TimeoutException e) {
            LOG.error("Could not delete all queues", e);
        } finally {
            if (channel != null) try {
                channel.close();
                connection.close();
            } catch (IOException | TimeoutException e) {
                LOG.error("Could not close channel.", e);
            }
        }
    }
}
