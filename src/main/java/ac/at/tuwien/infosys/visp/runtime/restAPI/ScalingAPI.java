package ac.at.tuwien.infosys.visp.runtime.restAPI;

import java.util.List;

import ac.at.tuwien.infosys.visp.common.operators.Operator;
import ac.at.tuwien.infosys.visp.runtime.configuration.Configurationprovider;
import ac.at.tuwien.infosys.visp.runtime.reasoner.ReasonerUtility;
import ac.at.tuwien.infosys.visp.runtime.resourceManagement.ProcessingNodeManagement;
import ac.at.tuwien.infosys.visp.runtime.topology.TopologyManagement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/control")
@RestController
@DependsOn("configurationprovider")
public class ScalingAPI {

    @Autowired
    private TopologyManagement topologyMgmt;

    @Autowired
    private Configurationprovider config;

    @Autowired
    private ProcessingNodeManagement pcm;

    @Autowired
    private ReasonerUtility reasonerUtility;

    @RequestMapping(value = {"/scaleup/{operatorName}"}, method = RequestMethod.GET)
    public String scaleup(@PathVariable String operatorName) {

        List<Operator> operators = topologyMgmt.getOperatorsForAConcreteLocation(config.getRuntimeIP());

        Operator selectedOperator = null;

        for (Operator op : operators) {
            if (operatorName.equals(op.getName())) {
                selectedOperator = op;
                break;
            }
        }

        if (selectedOperator == null) {
            return "no operator with that name exists.";
        }


        pcm.scaleup(reasonerUtility.selectSuitableDockerHost(selectedOperator), selectedOperator);

        return "ok";
    }

}


