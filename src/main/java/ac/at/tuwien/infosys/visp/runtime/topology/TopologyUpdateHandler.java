package ac.at.tuwien.infosys.visp.runtime.topology;


import ac.at.tuwien.infosys.visp.common.operators.Operator;
import ac.at.tuwien.infosys.visp.runtime.topology.operatorUpdates.SourcesUpdate;
import ac.at.tuwien.infosys.visp.runtime.topology.rabbitMq.RabbitMqManager;
import ac.at.tuwien.infosys.visp.topologyParser.TopologyParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

@Service
public class TopologyUpdateHandler {
    /**
     * this class is used to handle the process of updating the topology at runtime
     */

    private String incomingTopologyFilePath;

    private static final Logger LOG = LoggerFactory.getLogger(TopologyUpdateHandler.class);

    @Autowired
    TopologyParser topologyParser;

    @Autowired
    RabbitMqManager rabbitMqManager;


    public TopologyUpdateHandler() {
        incomingTopologyFilePath = null;
        topologyParser = new TopologyParser();
    }

    public File saveIncomingTopologyFile(String fileContent) {
        System.out.println("Inside TopologyUpdateHandler::saveIncomingTopologyFile");
        try {
            File temp = File.createTempFile("updatedTopology", ".txt");
            BufferedWriter bw = new BufferedWriter(new FileWriter(temp));
            bw.write(fileContent);
            bw.close();
            incomingTopologyFilePath = temp.getAbsolutePath();
            System.out.println("Saved new topology file to: " + incomingTopologyFilePath);
            return new File(incomingTopologyFilePath);
        } catch (IOException e) {
            throw new RuntimeException("Could not write topology to temporary file", e);
        }
    }

    public List<TopologyUpdate> computeUpdatesFromNewTopologyFile() {
        /**
         * this uses the antlr parser to actually process the new topology file and generate
         * a topology hashmap
         */
        TopologyParser parserForNewTopology = new TopologyParser();
        parserForNewTopology.loadTopologyFromFileSystem(incomingTopologyFilePath);
        Map<String, Operator> incomingTopology = parserForNewTopology.getTopology();
        LOG.info("Incoming topology contains the following entries:");
        for (Map.Entry<String, Operator> entry : incomingTopology.entrySet()) {
            String name = entry.getKey();
            Operator operator = entry.getValue();
            LOG.info(operator.toString());
        }

        List<TopologyUpdate> updates = updateTopology(topologyParser.getTopology(), parserForNewTopology.getTopology());
        LOG.info("Have to perform the following updates:");
        for (TopologyUpdate update : updates) {
            LOG.info(update.toString());
        }

        return updates;

    }

    public List<TopologyUpdate> updateTopology(Map<String, Operator> oldTopology, Map<String, Operator> newTopology) {
        /**
         * this function computes which changes need to be performed when updating from the old to the new topology on host location
         */
        List<TopologyUpdate> returnList = new ArrayList<TopologyUpdate>();

        // general assumption: operatorType names are unique throughout _both_ files
        // (if two operators have the same name in both files, it must be the same one)

        for (Map.Entry<String, Operator> entry : oldTopology.entrySet()) {
            String oldOperatorName = entry.getKey();
            Operator oldOperator = entry.getValue();
            if (!newTopology.containsKey(oldOperatorName)) {
                // operatorType no longer existing, remove it
                LOG.info("delete 1");
                returnList.add(new TopologyUpdate(oldOperator.getConcreteLocation().getIpAddress(), TopologyUpdate.Action.REMOVE_OPERATOR, oldOperator));
            } else {
                // operatorType is still here, check if we need to update
                updateOperator(returnList, oldOperator, newTopology.get(oldOperatorName));
            }
        }
        for (Map.Entry<String, Operator> entry : newTopology.entrySet()) {
            String newOperatorName = entry.getKey();
            Operator newOperator = entry.getValue();
            if (!oldTopology.containsKey(newOperatorName)) {
                // operatorType is new, create it
                LOG.info("add 1");
                returnList.add(new TopologyUpdate(newOperator.getConcreteLocation().getIpAddress(), TopologyUpdate.Action.ADD_OPERATOR, newOperator));
            } else {
                // this should already have been handled above...
            }
        }

        // sort updates in the order ADD - REMOVE - UPDATE
        // this prevents cases where operators are added as souce that do not exist yet
        returnList.sort(Comparator.comparingInt(t -> t.getAction().ordinal()));

        return returnList;
    }

    private void updateOperator(List<TopologyUpdate> updateList, Operator oldOperator, Operator newOperator) {
        /**
         * checks whether there are differences between the two operators and adds the according updates if there are
         */
        if (!oldOperator.getConcreteLocation().equals(newOperator.getConcreteLocation())) {
            // operatorType is migrated
            LOG.info("delete/add 2");
            updateList.add(new TopologyUpdate(oldOperator.getConcreteLocation().getIpAddress(), TopologyUpdate.Action.REMOVE_OPERATOR, oldOperator));
            updateList.add(new TopologyUpdate(newOperator.getConcreteLocation().getIpAddress(), TopologyUpdate.Action.ADD_OPERATOR, newOperator));
        }

        if (!sourcesAreEqual(oldOperator, newOperator)) {
            LOG.info("update");
            assert (newOperator.getConcreteLocation().equals(oldOperator.getConcreteLocation()));
            TopologyUpdate topologyUpdate = new TopologyUpdate(oldOperator.getConcreteLocation().getIpAddress(),
                    TopologyUpdate.Action.UPDATE_OPERATOR, TopologyUpdate.UpdateType.UPDATE_SOURCE,
                    newOperator);
            topologyUpdate.setChangeToBeExecuted(new SourcesUpdate(oldOperator.getSources(), newOperator.getSources()));
            updateList.add(topologyUpdate);

        }
    }

    private boolean sourcesAreEqual(Operator oldOperator, Operator newOperator) {
        List<String> oldSources = new ArrayList<>();
        for (Operator o : oldOperator.getSources()) {
            oldSources.add(o.getName());
        }

        List<String> newSources = new ArrayList<>();
        for (Operator o : newOperator.getSources()) {
            newSources.add(o.getName());
        }

        if (oldSources == null && newSources == null) {
            return true;
        }

        if ((oldSources == null && newSources != null)
                || oldSources != null && newSources == null
                || oldSources.size() != newSources.size()) {
            return false;
        }
        oldSources = new ArrayList<String>(oldSources);
        newSources = new ArrayList<String>(newSources);

        Collections.sort(oldSources);
        Collections.sort(newSources);
        return oldSources.equals(newSources);
    }

    public boolean testDeploymentByFile(String fileContent) {
        File topologyFile = saveIncomingTopologyFile(fileContent);
        List<TopologyUpdate> updates = computeUpdatesFromNewTopologyFile();
        List<String> involvedRuntimes = getInvolvedRuntimes(updates);

        return true; // TODO fix
    }

    public UpdateResult handleUpdateFromUser(String fileContent) {
        /**
         * this method is called by the user in the web ui
         * it must make sure that each involved VISP runtime is
         * properly informed about the changes through a multi-phase
         * commit mechanism
         */
        File topologyFile = saveIncomingTopologyFile(fileContent);
        List<TopologyUpdate> updates = computeUpdatesFromNewTopologyFile();
        List<String> involvedRuntimes = getInvolvedRuntimes(updates);

        for(String runtime : involvedRuntimes) {
            // TODO: rest call to instances
        }


        topologyParser.loadTopologyFromFileSystem(topologyFile.getAbsolutePath());
        rabbitMqManager.performUpdates(updates);

        return new UpdateResult(updates, topologyParser.getCurrentGraphvizPngFile());
    }

    private List<String> getInvolvedRuntimes(List<TopologyUpdate> updates) {
        /**
         * returns all runtimes involved in the updates
         */
        List<String> involvedRuntimes = new ArrayList<>();
        for(TopologyUpdate update : updates) {
            String runtime = update.getAffectedHost();
            if(!involvedRuntimes.contains(runtime)) {
                involvedRuntimes.add(runtime);
            }
        }
        return involvedRuntimes;
    }

    public class UpdateResult {
        public UpdateResult(List<TopologyUpdate> updatesPerformed, String pathToGraphviz) {
            this.updatesPerformed = updatesPerformed;
            this.pathToGraphviz = pathToGraphviz;
        }

        @Override
        public String toString() {
            return "UpdateResult{" +
                    "updatesPerformed=" + updatesPerformed +
                    ", pathToGraphviz='" + pathToGraphviz + '\'' +
                    '}';
        }

        public List<TopologyUpdate> updatesPerformed;
        public String pathToGraphviz;
    }
}