package ac.at.tuwien.infosys.visp.runtime.restAPI;


import ac.at.tuwien.infosys.visp.runtime.topology.TopologyManagement;
import ac.at.tuwien.infosys.visp.runtime.topology.TopologyUpdateHandler;
import ac.at.tuwien.infosys.visp.runtime.utility.Utilities;
import ac.at.tuwien.infosys.visp.topologyParser.TopologyParser;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@RestController
public class TopologyAPI {

    private static final Logger LOG = LoggerFactory.getLogger(TopologyAPI.class);

    @Autowired
    private TopologyUpdateHandler topologyUpdateHandler;

    @Autowired
    private TopologyParser parser;

    @Autowired
    private TopologyManagement topologyManagement;

    @Autowired
    private Utilities utilities;

    @RequestMapping("/checkStatus")
    @ResponseBody
    public Map<String, Object> checkOnlineStatus(HttpServletRequest request) {
        Map<String, Object> jsonData = new HashMap<>();
        jsonData.put("onlineStatus", "online");
        LOG.debug("/checkStatus request from IP " + request.getRemoteAddr());
        return jsonData;
    }

    @RequestMapping("/getTopology")
    public String getTopology() throws IOException {
        /**
         * returns the currently active topology as a VISP topology description language file
         */
        LOG.debug("/getTopology called");
        String topologyFile = parser.generateTopologyFile(topologyManagement.getTopology());
        byte[] encoded = Files.readAllBytes(Paths.get(topologyFile));
        return new String(encoded, Charset.defaultCharset());
    }

    @RequestMapping(value = "/testDeploymentForTopologyFile", method = RequestMethod.POST, produces = "application/json")
    @ResponseBody
    public Map<String, Object> testDeploymentForTopologyFile(
            @RequestParam("file") MultipartFile file) {
        /**
         * this method is called from another VISP runtime instance
         * the current instance checks whether it can perform the suggested
         * updates or not
         */

        LOG.debug("/testDeploymentForTopologyFile called");

        Map<String, Object> jsonData = new HashMap<>();
        String errorMessage = "none";
        boolean deploymentPossible;

        try {
            ByteArrayInputStream stream = new ByteArrayInputStream(file.getBytes());
            String fileContent = IOUtils.toString(stream, "UTF-8");
            String deploymentResult = topologyUpdateHandler.testDeploymentByFile(fileContent);
            deploymentPossible = "ok".equals(deploymentResult);
            if(!deploymentPossible) {
                errorMessage = deploymentResult;
            }
        }
        catch (Exception e) {
            LOG.error("Deployment not possible: " + e.getLocalizedMessage());
            errorMessage = e.getLocalizedMessage();
            deploymentPossible = false;
        }

        jsonData.put("errorMessage", errorMessage);
        jsonData.put("deploymentPossible", deploymentPossible);

        return jsonData;
    }

    @RequestMapping(value = "/abortTopologyUpdate", produces = "application/json")
    @ResponseBody
    public Map<String, Object> abortTopologyUpdate(@RequestParam(value="hash") String hash) throws IOException {
        /**
         * called from another VISP instance - leads to abort of the topology update process
         */
        LOG.debug("/abortTopologyUpdate called");
        Map<String, Object> jsonData = new HashMap<>();
        String errorMessage = "none";
        int localHash = topologyManagement.getTestDeploymentHash();
        if(localHash != Integer.parseInt(hash)) {
            LOG.warn("Warning - transmitted hash does not fit locally stored hash - will NOT abort commit");
            errorMessage = "invalid hash";
        } else {
            LOG.debug("Topology hash is valid");
        }

        jsonData.put("action", "abort");
        jsonData.put("hash", hash);
        jsonData.put("errorMessage", errorMessage);

        return jsonData;
    }

    @RequestMapping(value = "/commitTopologyUpdate", produces = "application/json")
    @ResponseBody
    public Map<String, Object> commitTopologyUpdate(@RequestParam(value="hash") String hash) throws IOException {
        /**
         * called from another VISP instance - leads to commit of the topology update process
         */
        LOG.debug("/commitTopologyUpdate called");
        Map<String, Object> jsonData = new HashMap<>();
        String errorMessage = "none";
        int localHash = topologyManagement.getTestDeploymentHash();
        if(localHash != Integer.parseInt(hash)) {
            LOG.warn("Warning - transmitted hash does not fit locally stored hash - will NOT commit");
            errorMessage = "invalid hash";
        } else {
            LOG.info("Hash was correct");
            //if(topologyUpdateHandler.testDeploymentByFile(topologyManagement.getTestDeploymentFile().getAbsolutePath())) {
            topologyUpdateHandler.commitUpdate(localHash);
            //} else {
            //    errorMessage = "Could not actually deploy topology file";
            //    LOG.error(errorMessage);
            //}
        }

        jsonData.put("action", "commit");
        jsonData.put("hash", hash);
        jsonData.put("errorMessage", errorMessage);
        return jsonData;
    }

    @RequestMapping(value = "/clear", produces = "application/json")
    @ResponseBody
    public Map<String, Object> clear() throws IOException {
        /**
         * called from another VISP instance - cleans the current topology
         */
        LOG.debug("/clear called");
        Map<String, Object> jsonData = new HashMap<>();
        String errorMessage = "none";
        utilities.clearAll();
        jsonData.put("action", "clear");
        jsonData.put("errorMessage", errorMessage);
        return jsonData;
    }

}

