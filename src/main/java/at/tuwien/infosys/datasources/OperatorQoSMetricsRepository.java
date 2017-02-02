package at.tuwien.infosys.datasources;


import at.tuwien.infosys.datasources.entities.OperatorQoSMetrics;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface OperatorQoSMetricsRepository extends CrudRepository<OperatorQoSMetrics, Long> {

    List<OperatorQoSMetrics> findFirstByNameOrderByTimestampDesc(String name);

}
