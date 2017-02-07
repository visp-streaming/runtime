package at.tuwien.infosys.restAPI;

import at.tuwien.infosys.entities.ResourcePoolUsage;
import at.tuwien.infosys.monitoring.ResourceUsage;
import at.tuwien.infosys.resourceManagement.ResourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RequestMapping("/resources")
@RestController
public class ResourceAPI {

    @Autowired
    private ResourceProvider resourceProvider;

    @Autowired
    private ResourceUsage resourceUsage;

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    @RequestMapping(value = {"/pools"}, method = RequestMethod.GET)
    @ResponseBody
    public Map<String, String> getResourcepools() {
        return resourceProvider.getResourceProviders();
    }

    @RequestMapping(value = {"/getLoad/{pool}"}, method = RequestMethod.GET)
    public ResourcePoolUsage getResourceAvailabilityForPool(@PathVariable String pool) {
        //CPUstats = usage in % of the assigned shares (from actual resources)
        return resourceUsage.calculateUsageForPool(pool);
    }
}
