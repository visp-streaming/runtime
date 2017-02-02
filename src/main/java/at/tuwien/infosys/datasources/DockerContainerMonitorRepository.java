package at.tuwien.infosys.datasources;


import at.tuwien.infosys.datasources.entities.DockerContainerMonitor;
import org.springframework.data.repository.CrudRepository;

public interface DockerContainerMonitorRepository extends CrudRepository<DockerContainerMonitor, Long> {

    DockerContainerMonitor findFirstByContaineridOrderByTimestampDesc(String containerid);

}
