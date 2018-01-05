package ac.at.tuwien.infosys.visp.runtime.restAPI;

import ac.at.tuwien.infosys.visp.common.resources.OperatorConfiguration;
import ac.at.tuwien.infosys.visp.runtime.topology.TopologyManagement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import ac.at.tuwien.infosys.visp.runtime.resourceManagement.ManualOperatorManagement;


import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is the interface to the Pathfinder Fault Tolerance Framework
 */

@RequestMapping("/pathfinder")
@RestController
public class PathfinderAPI {

    @Autowired
    TopologyManagement topologyManagement;

    @Autowired
    OperatorAPI operatorAPI;

    @Autowired
    private ManualOperatorManagement mom;


    private static final Logger LOG = LoggerFactory.getLogger(PathfinderAPI.class);

    @RequestMapping("/getAllStatistics")
    @ResponseBody
    public Map<String, Object> getAllStatistics(HttpServletRequest request) {
        Map<String, Object> jsonData = new HashMap<>();

        for(String operator: topologyManagement.getProcessingOperatorsAsList()) {
            HashMap<String, Object> hm = new HashMap<>();

            OperatorConfiguration operatorConfig = operatorAPI.getOperatorConfiguration(operator);

            hm.put("cpu_now", operatorConfig.getActualResources().getCores());
            hm.put("ram_now", operatorConfig.getActualResources().getMemory());
            hm.put("network_out", operatorConfig.getNetworkUpload());
            hm.put("network_in", operatorConfig.getNetworkDownload());
            hm.put("delivery_rate", operatorConfig.getDeliveryRate());
            hm.put("items_waiting", operatorConfig.getItemsWaiting());

            jsonData.put(operator, hm);
        }

        LOG.debug("/getAllStatistics request from IP " + request.getRemoteAddr());
        return jsonData;
    }

    /**
     * Commands VISP to change the active fallback path for a split operator
     * @param splitId the target split operator
     * @param activeId the id of the first operator of the new active alternative path
     * @return returns "ok" if the switch was successful
     */
    @RequestMapping("/switchAlternative")
    @ResponseBody
    public Map<String, Object> switchAlternative(@RequestParam(value="splitId") String splitId, @RequestParam(value="activeId") String activeId, HttpServletRequest request) {
        LOG.debug("/switchAlternative request for split id " + splitId + " change fallback to " + activeId + " from IP " + request.getRemoteAddr());
        Map<String, Object> jsonData = new HashMap<>();

        if(topologyManagement.isActiveAlternativePath(splitId, activeId)) {
            jsonData.put("result", "no_change");
            return jsonData;
        }

        // contact local statistics about the event
        // remove for production
        try {
            if(activeId.equals("position_to_keywords_external")) {
                RestTemplate restTemplate = new RestTemplate();
                String ipMockApi = "127.0.0.1:20299";
                String url = "http://" + ipMockApi + "/pathSwitchTo/" + "PEX";
                restTemplate.getForObject(url, Object.class);
            }
        } catch(Exception e) {
            LOG.error("Exception during mock-api connection", e);
        }

        try {
            if(activeId.equals("position_to_keywords_user_tags")) {
                RestTemplate restTemplate = new RestTemplate();
                String ipMockApi = "127.0.0.1:20299";
                String url = "http://" + ipMockApi + "/pathSwitchTo/" + "PUT";
                restTemplate.getForObject(url, Object.class);
            }
        } catch(Exception e) {
            LOG.error("Exception during mock-api connection", e);
        }

        topologyManagement.setActiveAlternativePath(splitId, activeId);
        jsonData.put("result", "ok");

        return jsonData;
    }

    @RequestMapping("/redeployOperator")
    @ResponseBody
    public Map<String, Object> redeployOperator(@RequestParam(value="operatorId") String operatorId, HttpServletRequest request) {
        LOG.info("Redeploying operator " + operatorId);
        Map<String, Object> jsonData = new HashMap<>();
        jsonData.put("result", "ok");

        try {
            if(operatorId.equals("position_to_keywords_external")) {
                RestTemplate restTemplate = new RestTemplate();
                String ipMockApi = "127.0.0.1:20299";
                String url = "http://" + ipMockApi + "/redeployOperator/" + "PEX";
                restTemplate.getForObject(url, Object.class);
            }
        } catch(Exception e) {
            LOG.error("Exception during mock-api connection", e);
        }

        mom.removeOperators(topologyManagement.getOperatorByIdentifier(operatorId));
        LOG.info("Done deleting operator");

        mom.addOperator(topologyManagement.getOperatorByIdentifier(operatorId));
        LOG.info("Done deploying new operator");


        return jsonData;
    }

    @RequestMapping("/probe")
    @ResponseBody
    public Map<String, Object> probe(@RequestParam(value="splitId") String splitId, @RequestParam(value="pathId") String pathId, HttpServletRequest request) {
        LOG.debug("/probe request for split id " + splitId + " change fallback to " + pathId + " from IP " + request.getRemoteAddr());
        Map<String, Object> jsonData = new HashMap<>();
        jsonData.put("result", "ok");

        try {
            if(pathId.equals("position_to_keywords_external")) {
                RestTemplate restTemplate = new RestTemplate();
                String ipMockApi = "127.0.0.1:20299";
                String url = "http://" + ipMockApi + "/startProbing/" + "PEX";
                restTemplate.getForObject(url, Object.class);
            }
        } catch(Exception e) {
            LOG.error("Exception during mock-api connection", e);
        }

        topologyManagement.probe(splitId, pathId);

        try {
            if(pathId.equals("position_to_keywords_external")) {
                RestTemplate restTemplate = new RestTemplate();
                String ipMockApi = "127.0.0.1:20299";
                String url = "http://" + ipMockApi + "/stopProbing/" + "PEX";
                restTemplate.getForObject(url, Object.class);
            }
        } catch(Exception e) {
            LOG.error("Exception during mock-api connection", e);
        }

        jsonData.put("status", "working");
        return jsonData;
    }

}
