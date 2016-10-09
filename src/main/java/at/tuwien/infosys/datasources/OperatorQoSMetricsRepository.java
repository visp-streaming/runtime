package at.tuwien.infosys.datasources;


import java.util.List;

import org.springframework.data.repository.CrudRepository;

import at.tuwien.infosys.entities.ApplicationQoSMetrics;
import at.tuwien.infosys.entities.OperatorQoSMetrics;

public interface OperatorQoSMetricsRepository extends CrudRepository<OperatorQoSMetrics, Long> {

    List<ApplicationQoSMetrics> findFirstByNameOrderByTimestampDesc(String name);

}
