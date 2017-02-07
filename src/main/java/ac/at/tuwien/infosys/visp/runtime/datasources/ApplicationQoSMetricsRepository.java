package ac.at.tuwien.infosys.visp.runtime.datasources;


import ac.at.tuwien.infosys.visp.runtime.datasources.entities.ApplicationQoSMetrics;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ApplicationQoSMetricsRepository extends CrudRepository<ApplicationQoSMetrics, Long> {

    List<ApplicationQoSMetrics> findFirstByApplicationNameOrderByTimestampDesc(String applicationName);

}
