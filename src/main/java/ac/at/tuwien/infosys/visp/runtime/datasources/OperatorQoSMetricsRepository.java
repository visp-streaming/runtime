package ac.at.tuwien.infosys.visp.runtime.datasources;


import ac.at.tuwien.infosys.visp.runtime.datasources.entities.OperatorQoSMetrics;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface OperatorQoSMetricsRepository extends CrudRepository<OperatorQoSMetrics, Long> {

    List<OperatorQoSMetrics> findFirstByNameOrderByTimestampDesc(String name);

}
