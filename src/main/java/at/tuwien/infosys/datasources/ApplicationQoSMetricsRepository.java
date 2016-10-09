package at.tuwien.infosys.datasources;


import java.util.List;

import org.springframework.data.repository.CrudRepository;

import at.tuwien.infosys.entities.ApplicationQoSMetrics;

public interface ApplicationQoSMetricsRepository extends CrudRepository<ApplicationQoSMetrics, Long> {

    List<ApplicationQoSMetrics> findFirstByApplicationNameOrderByTimestampDesc(String applicationName);

}
