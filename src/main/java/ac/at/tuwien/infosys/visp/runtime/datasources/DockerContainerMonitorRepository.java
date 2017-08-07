package ac.at.tuwien.infosys.visp.runtime.datasources;


import ac.at.tuwien.infosys.visp.runtime.datasources.entities.DockerContainerMonitor;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface DockerContainerMonitorRepository extends CrudRepository<DockerContainerMonitor, Long> {

    DockerContainerMonitor findFirstByContaineridOrderByTimestampDesc(String containerid);
    List<DockerContainerMonitor> findByOperatortype(String operator);
    List<DockerContainerMonitor> findByOperatorname(String operatorid);


    DockerContainerMonitor findFirstByOperatortypeOrderByTimestampDesc(String operator);
    DockerContainerMonitor findFirstByOperatornameOrderByTimestampDesc(String operator);



}
