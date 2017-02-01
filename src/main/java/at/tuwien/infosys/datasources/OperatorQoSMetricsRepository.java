package at.tuwien.infosys.datasources;


import java.util.List;

import org.springframework.data.repository.CrudRepository;

import at.tuwien.infosys.datasources.entities.OperatorQoSMetrics;

public interface OperatorQoSMetricsRepository extends CrudRepository<OperatorQoSMetrics, Long> {

    List<OperatorQoSMetrics> findFirstByNameOrderByTimestampDesc(String name);

}
