package ac.at.tuwien.infosys.visp.runtime.restAPI;

import java.util.List;

import ac.at.tuwien.infosys.visp.common.operators.ProcessingOperator;
import ac.at.tuwien.infosys.visp.common.resources.OperatorConfiguration;
import ac.at.tuwien.infosys.visp.runtime.configuration.OperatorConfigurationBootstrap;
import ac.at.tuwien.infosys.visp.runtime.datasources.DockerContainerMonitorRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.DockerContainerRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.ProcessingDurationRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.QueueMonitorRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.DockerContainer;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.DockerContainerMonitor;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.QueueMonitor;
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

    @Autowired
    private QueueMonitorRepository qmr;

    @Autowired
    private DockerContainerMonitorRepository dcmr;

    @Autowired
    private DockerContainerRepository dcr;

    @RequestMapping(value = {"/getOperatorConfiguration/type/{operatorType}"}, method = RequestMethod.GET)
    public OperatorConfiguration getOperatorConfigurationForOperatorType(@PathVariable String operatorType) {
        OperatorConfiguration opconfig =  new OperatorConfiguration(operatorType, 2400);

        topologymgmt.getTopology().values().stream()
                .filter(i -> i.getType().equals(operatorType))
                .filter(i -> i instanceof ProcessingOperator)
                .findFirst()
                .ifPresent(i -> opconfig.setExpectedDuration(((ProcessingOperator) i).getExpectedDuration()));

        List<DockerContainer> instances =  dcr.findByOperatorType(operatorType);
        if (instances != null) {
            opconfig.setInstances(instances.size());
        } else {
            opconfig.setInstances(0);
        }

        Integer counter = 0;
        Double duration = pcdr.findFirst5ByOperatortypeOrderByIdDesc(operatorType).
                stream().mapToDouble(i -> i.getDuration()).sum();

        if (counter == 0) {
            opconfig.setActualDuration(-1.0);
        } else {
            opconfig.setActualDuration(duration/counter);
        }

        opconfig.setPlannedResources(new OperatorConfigurationBootstrap().getExpected(operatorType));
        opconfig.setActualResources(resourceUsage.calculatelatestActualUsageForOperatorType(operatorType));

        QueueMonitor qm = qmr.findFirstByOperatorOrderByIdDesc(operatorType);

        if (qm.getIncomingRate() == null) {
            opconfig.setIncomingRate(qm.getIncomingRate());
        } else {
            opconfig.setIncomingRate(0.0);
        }


        if (qm.getAmount() == null) {
            opconfig.setItemsWaiting(qm.getAmount());
        } else {
            opconfig.setItemsWaiting(0);
        }


        if (qm.getDeliveryRate() == null) {
            opconfig.setDeliveryRate(qm.getDeliveryRate());
        } else {
            opconfig.setDeliveryRate(0.0);
        }


        DockerContainerMonitor dcm = dcmr.findFirstByOperatortypeOrderByTimestampDesc(operatorType);

        opconfig.setNetworkDownload(dcm.getNetworkDownload());
        opconfig.setNetworkUpload(dcm.getNetworkUpload());

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

        List<DockerContainer> instances =  dcr.findByOperatorName(operatorName);
        if (instances != null) {
            opconfig.setInstances(instances.size());
        } else {
            opconfig.setInstances(0);
        }


        Integer counter = 0;
        Double duration = pcdr.findFirst5ByOperatornameOrderByIdDesc(operatorName).
                stream().mapToDouble(i -> i.getDuration()).sum();
        
        if (counter == 0) {
            opconfig.setActualDuration(-1.0);
        } else {
            opconfig.setActualDuration(duration/counter);
        }

        opconfig.setPlannedResources(new OperatorConfigurationBootstrap().getExpected(operatorName));
        opconfig.setActualResources(resourceUsage.calculatelatestActualUsageForOperatorName(operatorName));

        QueueMonitor qm = qmr.findFirstByOperatorOrderByIdDesc(operatorName);

        opconfig.setIncomingRate(qm.getIncomingRate());
        opconfig.setItemsWaiting(qm.getAmount());

        DockerContainerMonitor dcm = dcmr.findFirstByOperatornameOrderByTimestampDesc(operatorName);

        opconfig.setNetworkDownload(dcm.getNetworkDownload());
        opconfig.setNetworkUpload(dcm.getNetworkUpload());

        return opconfig;
    }


}
