package at.tuwien.infosys.datasources;


import at.tuwien.infosys.datasources.entities.ApplicationQoSMetrics;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ApplicationQoSMetricsRepository extends CrudRepository<ApplicationQoSMetrics, Long> {

    List<ApplicationQoSMetrics> findFirstByApplicationNameOrderByTimestampDesc(String applicationName);

}
