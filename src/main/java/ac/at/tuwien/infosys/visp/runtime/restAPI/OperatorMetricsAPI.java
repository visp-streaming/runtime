package ac.at.tuwien.infosys.visp.runtime.restAPI;

import ac.at.tuwien.infosys.visp.common.resources.OperatorQoSMetricsDTO;
import ac.at.tuwien.infosys.visp.runtime.datasources.OperatorQoSMetricsRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.OperatorQoSMetrics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RequestMapping("/operatorMetrics")
@RestController
public class OperatorMetricsAPI {

    @Autowired
    private OperatorQoSMetricsRepository operatorQosRepository;

    @RequestMapping(value = {"/getLastMetrics/name/{operatorName}"}, method = RequestMethod.GET)
    public List<OperatorQoSMetricsDTO> getLast10OperatorQosMetricsByName(@PathVariable String operatorName) {
       List<OperatorQoSMetrics> metricsList = operatorQosRepository.findFirst10ByNameOrderByTimestampDesc(operatorName);
        List<OperatorQoSMetricsDTO> metricsDTOList =  metricsList.stream()
                .map(m ->
                        new OperatorQoSMetricsDTO(m.getName(), m.getTimestamp(), m.getProcessedMessages(),
                                m.getReceivedMessages(), m.getDeltaSeconds())).collect(Collectors.toList());
        return metricsDTOList;
    }
}
