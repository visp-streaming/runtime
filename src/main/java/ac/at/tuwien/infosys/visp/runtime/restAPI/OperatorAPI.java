package ac.at.tuwien.infosys.visp.runtime.restAPI;

import ac.at.tuwien.infosys.visp.common.resources.OperatorConfiguration;
import ac.at.tuwien.infosys.visp.runtime.configuration.OperatorConfigurationBootstrap;
import ac.at.tuwien.infosys.visp.runtime.monitoring.ResourceUsage;
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

    @RequestMapping(value = {"/getOperatorConfiguration/type/{operatorType}"}, method = RequestMethod.GET)
    public OperatorConfiguration getOperatorConfigurationForOperatorType(@PathVariable String operatorType) {

        OperatorConfiguration op =  new OperatorConfiguration(operatorType, 2400);
        op.setPlannedResources(new OperatorConfigurationBootstrap(operatorType).getExpected());
        op.setActualResources(resourceUsage.calculatelatestActualUsageForOperatorType(operatorType));
        return op;
    }

    @RequestMapping(value = {"/getOperatorConfiguration/name/{operatorName}"}, method = RequestMethod.GET)
    public OperatorConfiguration getOperatorConfiguration(@PathVariable String operatorName) {

        OperatorConfiguration op =  new OperatorConfiguration(operatorName, 2400);
        op.setPlannedResources(new OperatorConfigurationBootstrap(operatorName).getExpected());
        op.setActualResources(resourceUsage.calculatelatestActualUsageForOperatorid(operatorName));
        return op;
    }


}
