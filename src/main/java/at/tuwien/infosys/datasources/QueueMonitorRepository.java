package at.tuwien.infosys.datasources;


import at.tuwien.infosys.entities.QueueMonitor;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface QueueMonitorRepository extends CrudRepository<QueueMonitor, Long> {

    List<QueueMonitor> findFirst5ByOperatorOrderByIdDesc(String operator);


}
