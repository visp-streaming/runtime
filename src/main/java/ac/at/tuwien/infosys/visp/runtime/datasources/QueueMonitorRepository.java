package ac.at.tuwien.infosys.visp.runtime.datasources;


import ac.at.tuwien.infosys.visp.runtime.datasources.entities.QueueMonitor;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface QueueMonitorRepository extends CrudRepository<QueueMonitor, Long> {

    List<QueueMonitor> findFirst5ByOperatorOrderByIdDesc(String operator);

    QueueMonitor findFirstByOperatorOrderByIdDesc(String operator);


}
