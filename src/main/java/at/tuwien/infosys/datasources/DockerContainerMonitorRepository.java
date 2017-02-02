package at.tuwien.infosys.datasources;


import at.tuwien.infosys.datasources.entities.DockerContainerMonitor;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface DockerContainerMonitorRepository extends CrudRepository<DockerContainerMonitor, Long> {

    DockerContainerMonitor findFirstByContaineridOrderByTimestampDesc(String containerid);
    List<DockerContainerMonitor> findByOperator(String operator);


}
