package ac.at.tuwien.infosys.visp.runtime.restAPI;

import ac.at.tuwien.infosys.visp.runtime.configuration.OperatorConfigurationBootstrap;
import ac.at.tuwien.infosys.visp.runtime.entities.OperatorConfiguration;
import ac.at.tuwien.infosys.visp.runtime.monitoring.ResourceUsage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/operators")
@RestController
public class OperatorAPI {

    @Autowired
    private ResourceUsage resourceUsage;

    @Autowired
    private OperatorConfigurationBootstrap opConfig;

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    @RequestMapping(value = {"/getOperatorConfiguration/{operator}"}, method = RequestMethod.GET)
    public OperatorConfiguration getOperatorConfiguration(@PathVariable String operator) {

        OperatorConfiguration op =  new OperatorConfiguration(operator);
        op.setPlannedResources(new OperatorConfigurationBootstrap(operator).getExpected());
        op.setActualResources(resourceUsage.calculatelatestActualUsageForOperator(operator));

        //TODO add additional information from the topology

        return op;

    }


}
