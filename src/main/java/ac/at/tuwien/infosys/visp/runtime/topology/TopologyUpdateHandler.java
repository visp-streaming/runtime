package ac.at.tuwien.infosys.visp.runtime.topology;


import ac.at.tuwien.infosys.visp.common.operators.Operator;
import ac.at.tuwien.infosys.visp.runtime.configuration.Configurationprovider;
import ac.at.tuwien.infosys.visp.runtime.datasources.PooledVMRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.RuntimeConfigurationRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.VISPInstanceRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.RuntimeConfiguration;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.VISPInstance;
import ac.at.tuwien.infosys.visp.runtime.resourceManagement.ManualOperatorManagement;
import ac.at.tuwien.infosys.visp.runtime.restAPI.dto.TestDeploymentDTO;
import ac.at.tuwien.infosys.visp.runtime.topology.operatorUpdates.SizeUpdate;
import ac.at.tuwien.infosys.visp.runtime.topology.operatorUpdates.SourcesUpdate;
import ac.at.tuwien.infosys.visp.runtime.topology.rabbitMq.RabbitMqManager;
import ac.at.tuwien.infosys.visp.runtime.topology.rabbitMq.UpdateResult;
import ac.at.tuwien.infosys.visp.topologyParser.TopologyParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

@Service
@DependsOn("configurationprovider")
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

    @Autowired
    VISPInstanceRepository vir;

    @Autowired
    private PooledVMRepository pvmr;

    @Autowired
    private RuntimeConfigurationRepository rcr;

    @Autowired
    private Configurationprovider config;

    @Autowired
    private ManualOperatorManagement manualOperatorMgmt;

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
        LOG.debug("Incoming topology contains the following entries:");
        for (Map.Entry<String, Operator> entry : incomingTopology.topology.entrySet()) {
            String name = entry.getKey();
            Operator operator = entry.getValue();
            LOG.debug(operator.toString());
        }

        List<TopologyUpdate> updates = computeListOfUpdates(topologyManagement.getTopology(), incomingTopology.topology);
        LOG.debug("Have to perform the following updates:");
        for (TopologyUpdate update : updates) {
            LOG.debug(update.toString());
        }

        return updates;

    }

    public List<TopologyUpdate> computeListOfUpdates(Map<String, Operator> oldTopology, Map<String, Operator> newTopology) {
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
                returnList.add(new TopologyUpdate(oldOperator.getConcreteLocation().getIpAddress(), TopologyUpdate.Action.REMOVE_OPERATOR, oldOperator));
            } else {
                // operatorType is still here, check if we need to update
                updateSingleOperator(returnList, oldOperator, newTopology.get(oldOperatorName));
            }
        }
        for (Map.Entry<String, Operator> entry : newTopology.entrySet()) {
            String newOperatorName = entry.getKey();
            Operator newOperator = entry.getValue();
            if (!oldTopology.containsKey(newOperatorName)) {
                // operatorType is new, create it
                returnList.add(new TopologyUpdate(newOperator.getConcreteLocation().getIpAddress(), TopologyUpdate.Action.ADD_OPERATOR, newOperator));
            } else {
                // this should already have been handled above...
            }
        }
        return returnList;
    }

    private void updateSingleOperator(List<TopologyUpdate> updateList, Operator oldOperator, Operator newOperator) {
        /**
         * checks whether there are differences between the two operators and adds the according updates if there are
         */
        if (!oldOperator.getConcreteLocation().equals(newOperator.getConcreteLocation())) {
            // operator is migrated
            boolean sameIp = oldOperator.getConcreteLocation().getIpAddress().equals(newOperator.getConcreteLocation().getIpAddress());
            TopologyUpdate update1 = new TopologyUpdate(oldOperator.getConcreteLocation().getIpAddress(), TopologyUpdate.Action.REMOVE_OPERATOR, oldOperator);
            update1.setChangeTopologyMessageFlow(!sameIp); // do not actually change topology message flow since it will be redeployed on same instance
            updateList.add(update1);
            TopologyUpdate update2 = new TopologyUpdate(newOperator.getConcreteLocation().getIpAddress(), TopologyUpdate.Action.ADD_OPERATOR, newOperator);
            update2.setChangeTopologyMessageFlow(!sameIp);
            updateList.add(update2);
        }

        if (!sourcesAreEqual(oldOperator, newOperator)) {
            assert (newOperator.getConcreteLocation().equals(oldOperator.getConcreteLocation()));
            TopologyUpdate topologyUpdate = new TopologyUpdate(oldOperator.getConcreteLocation().getIpAddress(),
                    TopologyUpdate.Action.UPDATE_OPERATOR, TopologyUpdate.UpdateType.UPDATE_SOURCE,
                    newOperator);
            topologyUpdate.setChangeToBeExecuted(new SourcesUpdate(oldOperator.getSources(), newOperator.getSources()));
            updateList.add(topologyUpdate);
        }

        if(oldOperator.getSize() == null) {
            oldOperator.setSize(Operator.Size.UNKNOWN);
        }
        if(newOperator.getSize() == null) {
            newOperator.setSize(Operator.Size.UNKNOWN);
        }
        if(!oldOperator.getSize().equals(newOperator.getSize())) {
            TopologyUpdate topologyUpdate = new TopologyUpdate(oldOperator.getConcreteLocation().getIpAddress(),
                    TopologyUpdate.Action.UPDATE_OPERATOR, TopologyUpdate.UpdateType.UPDATE_SIZE,
                    newOperator);
            topologyUpdate.setChangeToBeExecuted(new SizeUpdate(oldOperator.getSize(), newOperator.getSize()));
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
        oldSources = new ArrayList<>(oldSources);
        newSources = new ArrayList<>(newSources);

        Collections.sort(oldSources);
        Collections.sort(newSources);
        return oldSources.equals(newSources);
    }

    public boolean testDeploymentByFile(String filePath) {
        this.lock.lock();
        try {
            //File topologyFile = saveIncomingTopologyFile(filePath);
            File topologyFile = new File(filePath);
            topologyManagement.saveTestDeploymentFile(topologyFile, filePath.hashCode());
            List<TopologyUpdate> updates = computeUpdatesFromNewTopologyFile();

            String ownDeploymentError = manualOperatorMgmt.testDeployment(extractOwnOperators(topologyFile, config.getRuntimeIP()));

            if(!ownDeploymentError.equals("ok")) {
                String errorMessage = "Critical error - Could not deploy topology on remote instance " +
                        config.getRuntimeIP() + "; error: [" + ownDeploymentError + "]";
                LOG.error(errorMessage);
                return false;
            }
        } finally {
            this.lock.unlock();
        }
        LOG.info("testDeployment is possible!");
        return true;
    }

    public UpdateResult handleUpdateFromUser(String fileContent) throws UnsupportedEncodingException {
        /**
         * this method is called by the user in the web ui
         * it must make sure that each involved VISP runtime is
         * properly informed about the changes through a multi-phase
         * commit mechanism
         */


        File topologyFile = saveIncomingTopologyFile(fileContent);
        List<TopologyUpdate> updates = computeUpdatesFromNewTopologyFile();

        // first check if local deployment is even possible - otherwise do not waste time to contact other instances
        String ownDeploymentError = manualOperatorMgmt.testDeployment(extractOwnOperators(topologyFile, config.getRuntimeIP()));

        if(!ownDeploymentError.equals("ok")) {
            String errorMessage = "Critical error - Could not deploy topology on own instance; error: [" + ownDeploymentError + "]";
            LOG.error(errorMessage);
            UpdateResult result = new UpdateResult(null, null, UpdateResult.UpdateStatus.LOCAL_DEPLOYMENT_NOT_POSSIBLE);
            result.setErrorMessage(errorMessage);
            return result;
        }

        List<String> involvedRuntimes = getInvolvedRuntimes(updates);

        List<String> offlineRuntimes = getOfflineRuntimeInstances(involvedRuntimes);

        boolean allInvolvedRuntimesAreAvailable = offlineRuntimes.size() == 0;

        if (!allInvolvedRuntimesAreAvailable) {
            String errorMessage = "Critical error - one or more VISP runtime instances are not available: [" + String.join(", ", offlineRuntimes) + "]";
            LOG.error(errorMessage);
            UpdateResult result = new UpdateResult(null, null, UpdateResult.UpdateStatus.RUNTIMES_NOT_AVAILABLE);
            result.setErrorMessage(errorMessage);
            return result;
        }

        try {
            fileContent = new String(Files.readAllBytes(
                    Paths.get(topologyParser.generateTopologyFile(
                            assignConcreteResourcePools(topologyParser.parseTopologyFromString(fileContent).topology)))));
            topologyFile = saveIncomingTopologyFile(fileContent);
            updates = computeUpdatesFromNewTopologyFile();
        } catch (IOException e) {
            LOG.error("Could not assign concrete resource pools for uploaded topology", e.getLocalizedMessage());
        }

        int hash = fileContent.hashCode();

        boolean allInvolvedRuntimesAgree = true;
        List<String> contactedRuntimes = new ArrayList<>();
        List<String> listOfFailedRuntimes = new ArrayList<>();
        for (String runtime : involvedRuntimes) {
            // TODO: make for loop parallel
            LOG.debug("Contacting VISP runtime for test deployment: " + runtime);
            TestDeploymentDTO result = sendRestRequest(fileContent, "http://" + runtime + ":8080/testDeploymentForTopologyFile");
            contactedRuntimes.add(runtime);
            if (!result.isDeploymentPossible()) {
                allInvolvedRuntimesAgree = false;
                listOfFailedRuntimes.add(runtime);
            }
        }

        String pngPath = null;

        UpdateResult updateResult = new UpdateResult(updates, null, UpdateResult.UpdateStatus.SUCCESSFUL);

        if (allInvolvedRuntimesAgree) {
            if (contactedRuntimes.size() != involvedRuntimes.size()) {
                throw new RuntimeException("Exception: number of involved and contacted runtimes must agree in case of commit");
            }
            sendCommitToRuntimes(contactedRuntimes, hash);
            pngPath = executeUpdate(topologyFile, updates);
            updateResult.distributedUpdateSuccessful = true;
            updateResult.dotPath = pngPath;
            updateResult.setUpdatesPerformed(updates);
        } else {
            LOG.warn("Deployment not possible for all runtimes - abort");
            updateResult.distributedUpdateSuccessful = false;
            sendAbortSignalToRuntimes(contactedRuntimes, hash);
            updateResult.setStatus(UpdateResult.UpdateStatus.DEPLOYMENT_NOT_POSSIBLE);
            updateResult.setDotPath(null);
            updateResult.setErrorMessage("Deployment not possible for the following runtimes: " + String.join(", ", listOfFailedRuntimes));
        }
        updateResult.dotPath = pngPath;

        return updateResult;
    }

    private List<Operator> extractOwnOperators(File topologyFile, String runtimeIP) {
        /**
         * this method extracts all operators from a topology that belong to a specific VISP runtime instance
         */
        Map<String, Operator> topology = topologyParser.parseTopologyFromFileSystem(topologyFile.getAbsolutePath()).topology;
        List<Operator> resultList = new ArrayList<>();

        for(Operator o : topology.values()) {
            if(o.getConcreteLocation().getIpAddress().equals(runtimeIP)) {
                resultList.add(o);
            }
        }

        return resultList;
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
        LOG.debug("Sending abort signal to " + contactedRuntimes.size() + " runtimes");
        for (String runtime : contactedRuntimes) {
            RestTemplate restTemplate = new RestTemplate();
            String url = "http://" + runtime + ":8080/abortTopologyUpdate?hash=" + hash;
            LOG.debug("sending request to url: " + url);
            Map<String, Object> abortResult = restTemplate.getForObject(url, Map.class);
            LOG.debug("runtime " + runtime + " replied " + abortResult.get("errorMessage"));
        }
    }

    private void sendCommitToRuntimes(List<String> contactedRuntimes, int hash) {
        LOG.debug("Sending commit signal to " + contactedRuntimes.size() + " runtimes");
        for (String runtime : contactedRuntimes) {
            RestTemplate restTemplate = new RestTemplate();
            String url = "http://" + runtime + ":8080/commitTopologyUpdate?hash=" + hash;
            LOG.debug("sending request to url: " + url);
            Map<String, Object> commitResult = restTemplate.getForObject(url, Map.class);
            LOG.debug("runtime " + runtime + " replied " + commitResult.get("errorMessage"));
        }
    }

    private String executeUpdate(File topologyFile, List<TopologyUpdate> updates) {
        TopologyParser.ParseResult parseResult = topologyParser.parseTopologyFromFileSystem(topologyFile.getAbsolutePath());
        topologyManagement.setTopology(parseResult.topology);
        topologyManagement.setDotFile(parseResult.dotFile);

        String pngPath;
        try {
            rabbitMqManager.performUpdates(updates);
            pngPath = topologyManagement.getDotFile();
        } catch (Exception e) {
            LOG.error("Could not perform updates", e);
            pngPath = null;
        }

        updateKnownVispInstances();

        // save new topology file to db

        try {
            String fileContent = new String(Files.readAllBytes(topologyFile.toPath()));
            RuntimeConfiguration rt = rcr.findFirstByKey("last_topology_file");
            if(rt == null) {
                rt = new RuntimeConfiguration("last_topology_file", fileContent);
            } else {
                rt.setValue(fileContent);
            }
            rcr.saveAndFlush(rt);
            LOG.debug("saved runtime configuration with key last_topology_file and value " + fileContent);
        } catch (IOException e) {
            LOG.error("Could not save topology file to DB", e);
        }


        return pngPath;
    }

    private Map<String, Operator> assignConcreteResourcePools(Map<String, Operator> topology) {
        /**
         * the user can use "*" for resource pools
         * if that has happened, replace with concrete resource pools
         */

        for (Operator op : topology.values()) {
            if (!op.getConcreteLocation().getResourcePool().equals("*")) {
                continue;
            }
            if (!op.getConcreteLocation().getIpAddress().equals(config.getRuntimeIP())) {
                // request list of resource pools from the instance
                try {
                    RestTemplate restTemplate = new RestTemplate();
                    String url = "http://" + op.getConcreteLocation().getIpAddress() + ":8080/vispconfiguration/listResourcePools";
                    List<Map<String, Object>> resourcePools = restTemplate.getForObject(url, List.class);
                    Map<String, Object> chosenPool = resourcePools.get(new Random().nextInt(resourcePools.size()));
                    Operator.Location loc = op.getConcreteLocation();
                    op.setConcreteLocation(new Operator.Location(op.getConcreteLocation().getIpAddress(), (String) chosenPool.get("name")));
                    LOG.debug("Assigning concrete resource pool " + (String) chosenPool.get("name") + " for operator " +
                            op.getName() + " on runtime " + op.getConcreteLocation());
                } catch (Exception e) {
                    LOG.error("Error while querying VISP instance for list of resource pools", e);
                    throw new RuntimeException("Could not query VISP instance for list of resource pools");
                }
            } else {
                List<String> allPools = pvmr.findDistinctPoolnames();
                if (allPools.size() == 0) {
                    throw new RuntimeException("No resource pools available for operator " + op.getName() + " on runtime " + config.getRuntimeIP());
                }
                String resourcePool = allPools.get(new Random().nextInt(allPools.size()));
                op.setConcreteLocation(new Operator.Location(op.getConcreteLocation().getIpAddress(), resourcePool));
                LOG.debug("Assigning concrete resource pool " + resourcePool + " for operator " + op.getName() + " on own runtime " + config.getRuntimeIP());
            }
        }

        return topology;
    }

    private void updateKnownVispInstances() {
        /**
         * curates a list of known runtime instances for various communication purposes
         */
        Map<String, VISPInstance> allInstances = new HashMap<>();

        for (Operator op : topologyManagement.getOperators()) {
            for (Operator.Location loc : op.getAllowedLocationsList()) {
                if (!allInstances.containsKey(loc.getIpAddress())) {
                    allInstances.put(loc.getIpAddress(), new VISPInstance(loc.getIpAddress()));
                }
            }
        }
        int newInstances = 0;
        for (VISPInstance instance : allInstances.values()) {
            VISPInstance vi = null;
            try {
                vi = vir.findFirstByUri(instance.getUri());
            } catch (Exception e) {

            }
            if (vi == null) {
                vir.save(instance);
                newInstances++;
            }
        }
        LOG.debug("Stored " + newInstances + " new VISP instances to local repository");
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
        LOG.debug(testDeploymentDTO.toString());
        return testDeploymentDTO;
    }

    private List<String> getInvolvedRuntimes(List<TopologyUpdate> updates) {
        /**
         * returns all runtimes involved in the updates
         */
        List<String> involvedRuntimes = new ArrayList<>();
        for (TopologyUpdate update : updates) {
            String runtime = update.getAffectedHost();
            if (runtime.equals(config.getRuntimeIP())) {
                // the own runtime is not queried via REST
                LOG.debug("Skipping own runtime with IP " + config.getRuntimeIP());
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
                computeListOfUpdates(topologyManagement.getTopology(),
                        topologyParser.parseTopologyFromFileSystem(
                                topologyManagement.getTestDeploymentFile().getAbsolutePath()).topology));
    }


}