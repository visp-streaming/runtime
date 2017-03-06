package ac.at.tuwien.infosys.visp.runtime.topology;


import ac.at.tuwien.infosys.visp.common.operators.Operator;
import ac.at.tuwien.infosys.visp.runtime.restAPI.dto.TestDeploymentDTO;
import ac.at.tuwien.infosys.visp.runtime.topology.operatorUpdates.SourcesUpdate;
import ac.at.tuwien.infosys.visp.runtime.topology.rabbitMq.RabbitMqManager;
import ac.at.tuwien.infosys.visp.runtime.topology.rabbitMq.UpdateResult;
import ac.at.tuwien.infosys.visp.topologyParser.TopologyParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

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
    TopologyManagement topologyManagement;

    @Autowired
    RabbitMqManager rabbitMqManager;

    @Value("${visp.runtime.ip}")
    private String vispRuntimeOwnIp;

    private ReentrantLock lock = new ReentrantLock();


    public TopologyUpdateHandler() {
        incomingTopologyFilePath = null;
        topologyParser = new TopologyParser();
    }

    public File saveIncomingTopologyFile(String fileContent) {
        try {
            File temp = File.createTempFile("updatedTopology", ".txt");
            BufferedWriter bw = new BufferedWriter(new FileWriter(temp));
            bw.write(fileContent);
            bw.close();
            incomingTopologyFilePath = temp.getAbsolutePath();
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
        TopologyParser.ParseResult incomingTopology = topologyParser.parseTopologyFromFileSystem(incomingTopologyFilePath);
        LOG.info("Incoming topology contains the following entries:");
        for (Map.Entry<String, Operator> entry : incomingTopology.topology.entrySet()) {
            String name = entry.getKey();
            Operator operator = entry.getValue();
            LOG.info(operator.toString());
        }

        List<TopologyUpdate> updates = updateTopology(topologyManagement.getTopology(), incomingTopology.topology);
        LOG.info("Have to perform the following updates:");
        for (TopologyUpdate update : filterUpdatesByOwnRuntime(updates, vispRuntimeOwnIp)) {
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
        // this prevents cases where operators are added as source that do not exist yet
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
        this.lock.lock();
        try {
            File topologyFile = saveIncomingTopologyFile(fileContent);
            topologyManagement.saveTestDeploymentFile(topologyFile, fileContent.hashCode());
            List<TopologyUpdate> updates = computeUpdatesFromNewTopologyFile();
            List<String> involvedRuntimes = getInvolvedRuntimes(updates);
            // TODO: only check updates where own IP is affected
        } finally {
            this.lock.unlock();
        }
        LOG.info("testDeployment is possible!");
        return true; // TODO fix
    }

    public UpdateResult handleUpdateFromUser(String fileContent) throws UnsupportedEncodingException {
        /**
         * this method is called by the user in the web ui
         * it must make sure that each involved VISP runtime is
         * properly informed about the changes through a multi-phase
         * commit mechanism
         */
        int hash = fileContent.hashCode();
        File topologyFile = saveIncomingTopologyFile(fileContent);
        List<TopologyUpdate> updates = computeUpdatesFromNewTopologyFile();
        List<String> involvedRuntimes = getInvolvedRuntimes(updates);

        List<String> offlineRuntimes = getOfflineRuntimeInstances(involvedRuntimes);

        boolean allInvolvedRuntimesAreAvailable = offlineRuntimes.size() == 0;

        if (!allInvolvedRuntimesAreAvailable) {
            String errorMessage = "Critical error - one or more VISP runtime instances are not available: [" + String.join(", ", offlineRuntimes) + "]";
            LOG.error(errorMessage);
            UpdateResult result = new UpdateResult(null, null, UpdateResult.UpdateStatus.SUCCESSFUL);
            result.setStatus(UpdateResult.UpdateStatus.RUNTIMES_NOT_AVAILABLE);
            result.setErrorMessage(errorMessage);
            return result;
        }

        boolean allInvolvedRuntimesAgree = true;
        List<String> contactedRuntimes = new ArrayList<>();
        for (String runtime : involvedRuntimes) {
            // TODO: make for loop parallel
            TestDeploymentDTO result = sendRestRequest(fileContent, "http://" + runtime + ":8080/testDeploymentForTopologyFile");
            contactedRuntimes.add(runtime);
            if (!result.isDeploymentPossible()) {
                allInvolvedRuntimesAgree = false;
                break;
            }
        }

        String pngPath = null;

        UpdateResult updateResult = new UpdateResult(filterUpdatesByOwnRuntime(updates, vispRuntimeOwnIp), null, UpdateResult.UpdateStatus.SUCCESSFUL);

        if (allInvolvedRuntimesAgree) {
            if (contactedRuntimes.size() != involvedRuntimes.size()) {
                throw new RuntimeException("Exception: number of involved and contacted runtimes must agree in case of commit");
            }
            sendCommitToRuntimes(contactedRuntimes, hash);
            pngPath = executeUpdate(topologyFile, updates);
            updateResult.distributedUpdateSuccessful = true;
            updateResult.dotPath = pngPath;
        } else {
            updateResult.distributedUpdateSuccessful = false;
            sendAbortSignalToRuntimes(contactedRuntimes, hash);
            updateResult.setStatus(UpdateResult.UpdateStatus.DEPLOYMENT_NOT_POSSIBLE);
            updateResult.setDotPath(null);
        }
        updateResult.dotPath = pngPath;

        return updateResult;
    }

    private List<String> getOfflineRuntimeInstances(List<String> involvedRuntimes) {
        /**
         * quickly checks if each runtime instance is currently online
         */

        List<String> offlineInstances = new ArrayList<>();
        for (String runtime : involvedRuntimes) {
            boolean isOnline = false;
            try {
                RestTemplate restTemplate = new RestTemplate();
                String url = "http://" + runtime + ":8080/checkStatus";
                Map<String, Object> abortResult = restTemplate.getForObject(url, Map.class);
                String status = (String) abortResult.get("onlineStatus");
                if (status.equals("online")) {
                    isOnline = true;
                }
            } catch (Exception e) {
                isOnline = false;
            }
            if (!isOnline) {
                offlineInstances.add(runtime);
            }
        }

        return offlineInstances;
    }

    private void sendAbortSignalToRuntimes(List<String> contactedRuntimes, int hash) {
        LOG.info("Sending abort signal to " + contactedRuntimes.size() + " runtimes");
        for (String runtime : contactedRuntimes) {
            RestTemplate restTemplate = new RestTemplate();
            String url = "http://" + runtime + ":8080/abortTopologyUpdate?hash=" + hash;
            LOG.info("sending request to url: " + url);
            Map<String, Object> abortResult = restTemplate.getForObject(url, Map.class);
            LOG.info("runtime " + runtime + " replied " + abortResult.get("errorMessage"));
        }
    }

    private void sendCommitToRuntimes(List<String> contactedRuntimes, int hash) {
        LOG.info("Sending commit signal to " + contactedRuntimes.size() + " runtimes");
        for (String runtime : contactedRuntimes) {
            RestTemplate restTemplate = new RestTemplate();
            String url = "http://" + runtime + ":8080/commitTopologyUpdate?hash=" + hash;
            LOG.info("sending request to url: " + url);
            Map<String, Object> commitResult = restTemplate.getForObject(url, Map.class);
            LOG.info("runtime " + runtime + " replied " + commitResult.get("errorMessage"));
        }
    }

    private String executeUpdate(File topologyFile, List<TopologyUpdate> updates) {
        TopologyParser.ParseResult parseResult = topologyParser.parseTopologyFromFileSystem(topologyFile.getAbsolutePath());
        topologyManagement.setTopology(parseResult.topology);
        topologyManagement.setDotFile(parseResult.dotFile);
        LOG.info("set dotfile for future usage to " + parseResult.dotFile);

        String pngPath;
        try {
            rabbitMqManager.performUpdates(filterUpdatesByOwnRuntime(updates, vispRuntimeOwnIp));
            pngPath = topologyManagement.getDotfile();
        } catch (Exception e) {
            LOG.error("could not perform updates", e);
            pngPath = null;
        }

        return pngPath;
    }

    public List<TopologyUpdate> filterUpdatesByOwnRuntime(List<TopologyUpdate> updates, String vispRuntimeOwnIp) {
        List<TopologyUpdate> returnList = new ArrayList<>();
        for (TopologyUpdate update : updates) {
            if (update.getAffectedHost().equals(vispRuntimeOwnIp)) {
                returnList.add(update);
            } else {
                LOG.info("do not execute update on this host: " + update);
            }
        }

        return returnList;
    }

    private TestDeploymentDTO sendRestRequest(final String fileContent, String url) throws UnsupportedEncodingException {
        RestTemplate restTemplate = new RestTemplate();
        MultiValueMap<String, Object> map = new LinkedMultiValueMap<String, Object>();
        final String filename = "topology.txt";
        map.add("name", filename);
        map.add("filename", filename);
        ByteArrayResource contentsAsResource = new ByteArrayResource(fileContent.getBytes("UTF-8")) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
        map.add("file", contentsAsResource);
        TestDeploymentDTO testDeploymentDTO = restTemplate.postForObject(url, map, TestDeploymentDTO.class);
        LOG.info(testDeploymentDTO.toString());
        return testDeploymentDTO;
    }

    private List<String> getInvolvedRuntimes(List<TopologyUpdate> updates) {
        /**
         * returns all runtimes involved in the updates
         */
        List<String> involvedRuntimes = new ArrayList<>();
        for (TopologyUpdate update : updates) {
            String runtime = update.getAffectedHost();
            if (runtime.equals(vispRuntimeOwnIp)) {
                // the own runtime is not queried via REST
                LOG.info("Skipping own runtime with IP " + vispRuntimeOwnIp);
                continue;
            }
            if (!involvedRuntimes.contains(runtime)) {
                involvedRuntimes.add(runtime);
            }
        }
        return involvedRuntimes;
    }

    public void commitUpdate(int localHash) {
        LOG.info("Commiting update with localHash " + localHash);
        // TODO: check if hashes match
        executeUpdate(topologyManagement.getTestDeploymentFile(),
                updateTopology(topologyManagement.getTopology(),
                        topologyParser.parseTopologyFromFileSystem(
                                topologyManagement.getTestDeploymentFile().getAbsolutePath()).topology));
    }


}