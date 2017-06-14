package ac.at.tuwien.infosys.visp.runtime.restAPI;

import ac.at.tuwien.infosys.visp.common.operators.ProcessingOperator;
import ac.at.tuwien.infosys.visp.common.resources.OperatorConfiguration;
import ac.at.tuwien.infosys.visp.runtime.configuration.OperatorConfigurationBootstrap;
import ac.at.tuwien.infosys.visp.runtime.datasources.ProcessingDurationRepository;
import ac.at.tuwien.infosys.visp.runtime.monitoring.ResourceUsage;
import ac.at.tuwien.infosys.visp.runtime.topology.TopologyManagement;
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
    private TopologyManagement topologymgmt;

    @Autowired
    private ProcessingDurationRepository pcdr;

    @RequestMapping(value = {"/getOperatorConfiguration/type/{operatorType}"}, method = RequestMethod.GET)
    public OperatorConfiguration getOperatorConfigurationForOperatorType(@PathVariable String operatorType) {
        OperatorConfiguration opconfig =  new OperatorConfiguration(operatorType, 2400);

        topologymgmt.getTopology().values().stream()
                .filter(i -> i.getType().equals(operatorType))
                .filter(i -> i instanceof ProcessingOperator)
                .findFirst()
                .ifPresent(i -> opconfig.setExpectedDuration(((ProcessingOperator) i).getExpectedDuration()));

        Integer counter = 0;
        Double duration = pcdr.findFirst5ByOperatorOrderByIdDesc(operatorType).
                stream().mapToDouble(i -> i.getDuration()).sum();

        if (counter == 0) {
            opconfig.setActualDuration(opconfig.getExpectedDuration());
        } else {
            opconfig.setActualDuration(duration/counter);
        }

        opconfig.setPlannedResources(new OperatorConfigurationBootstrap().getExpected(operatorType));
        opconfig.setActualResources(resourceUsage.calculatelatestActualUsageForOperatorType(operatorType));
        return opconfig;
    }

    @RequestMapping(value = {"/getOperatorConfiguration/name/{operatorName}"}, method = RequestMethod.GET)
    public OperatorConfiguration getOperatorConfiguration(@PathVariable String operatorName) {

        OperatorConfiguration opconfig =  new OperatorConfiguration(operatorName, 2400);

        topologymgmt.getTopology().values().stream()
                .filter(i -> i.getType().equals(operatorName))
                .filter(i -> i instanceof ProcessingOperator)
                .findFirst()
                .ifPresent(i -> opconfig.setExpectedDuration(((ProcessingOperator) i).getExpectedDuration()));

        Integer counter = 0;
        Double duration = pcdr.findFirst5ByOperatorOrderByIdDesc(operatorName).
                stream().mapToDouble(i -> i.getDuration()).sum();
        
        if (counter == 0) {
            opconfig.setActualDuration(opconfig.getExpectedDuration());
        } else {
            opconfig.setActualDuration(duration/counter);
        }

        opconfig.setPlannedResources(new OperatorConfigurationBootstrap().getExpected(operatorName));
        opconfig.setActualResources(resourceUsage.calculatelatestActualUsageForOperatorid(operatorName));
        return opconfig;
    }


}
