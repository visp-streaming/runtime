package ac.at.tuwien.infosys.visp.runtime.restAPI;

import ac.at.tuwien.infosys.visp.runtime.topology.TopologyManagement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

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

    private static final Logger LOG = LoggerFactory.getLogger(PathfinderAPI.class);

    @RequestMapping("/getAllStatistics")
    @ResponseBody
    public Map<String, Object> getAllStatistics(HttpServletRequest request) {
        Map<String, Object> jsonData = new HashMap<>();

        for(String operator: topologyManagement.getProcessingOperatorsAsList()) {
            HashMap<String, Object> hm = new HashMap<>();

            hm.put("cpu_now", 0.8);
            hm.put("ram_now", 0.6);
            hm.put("network_out", 110);

            jsonData.put(operator, hm);
        }

        LOG.debug("/getAllStatistics request from IP " + request.getRemoteAddr());
        return jsonData;
    }
}
