package ac.at.tuwien.infosys.visp.runtime.restAPI;


import ac.at.tuwien.infosys.visp.runtime.topology.TopologyManagement;
import ac.at.tuwien.infosys.visp.runtime.topology.TopologyUpdateHandler;
import ac.at.tuwien.infosys.visp.runtime.topology.rabbitMq.RabbitMqManager;
import ac.at.tuwien.infosys.visp.topologyParser.TopologyParser;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.http.HttpStatus.OK;

@RestController
public class TopologyAPI {

    private static final Logger LOG = LoggerFactory.getLogger(TopologyAPI.class);

    @Autowired
    TopologyUpdateHandler topologyUpdateHandler;

    @Autowired
    RabbitMqManager rabbitMqManager;

    @Autowired
    private TopologyParser parser;

    @Autowired
    private TopologyManagement topologyManagement;

//    @RequestMapping("/addMessageFlow")
//    public String addMessageFlow() {
//        /**
//         * this method changes some aspect about the rabbitmq configuration at runtime
//         * it is used to test whether the newly built adaption features actually work
//         */
//
//        try {
//            rabbitMqManager.addMessageFlow("step1", "step4", "128.130.172.226", "128.130.172.226");
//            return "Done!";
//
//        } catch (Exception e) {
//            return e.getLocalizedMessage();
//        }
//
//    }
//
//    @RequestMapping("/removeMessageFlow")
//    public String removeMessageFlow() {
//        try {
//            rabbitMqManager.removeMessageFlow("step1", "step4", "128.130.172.226", "128.130.172.226");
//            return "Done!";
//
//        } catch (Exception e) {
//            return e.getLocalizedMessage();
//        }
//    }

    @RequestMapping("/getTopology")
    public String getTopology() throws IOException {
        /**
         * returns the currently active topology as a VISP topology description language file
         */
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

        LOG.info("IN TEST DEPLOYMENT for the following file:");

        Map<String, Object> jsonData = new HashMap<>();
        String errorMessage = "none";
        boolean deploymentPossible = false;

        try {
            ByteArrayInputStream stream = new ByteArrayInputStream(file.getBytes());
            String fileContent = IOUtils.toString(stream, "UTF-8");
            LOG.info(fileContent);
            deploymentPossible = topologyUpdateHandler.testDeploymentByFile(fileContent);
        }
        catch (Exception e) {
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
        Map<String, Object> jsonData = new HashMap<>();
        String errorMessage = "none";
        LOG.info("aborting topology update");
        int localHash = topologyManagement.getTestDeploymentHash();
        if(localHash != Integer.parseInt(hash)) {
            LOG.warn("Warning - transmitted hash does not fit locally stored hash - will NOT abort commit");
            errorMessage = "invalid hash";
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
        Map<String, Object> jsonData = new HashMap<>();
        String errorMessage = "none";
        LOG.info("commiting topology update for hash " + hash);
        int localHash = topologyManagement.getTestDeploymentHash();
        if(localHash != Integer.parseInt(hash)) {
            LOG.warn("Warning - transmitted hash does not fit locally stored hash - will NOT commit");
            errorMessage = "invalid hash";
        } else {
            topologyUpdateHandler.commitUpdate(localHash);
        }

        jsonData.put("action", "commit");
        jsonData.put("hash", hash);
        jsonData.put("errorMessage", errorMessage);
        return jsonData;
    }

}

