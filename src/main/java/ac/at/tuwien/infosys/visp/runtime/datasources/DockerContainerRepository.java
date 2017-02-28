package ac.at.tuwien.infosys.visp.runtime.datasources;


import ac.at.tuwien.infosys.visp.runtime.datasources.entities.DockerContainer;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface DockerContainerRepository extends CrudRepository<DockerContainer, Long> {

    List<DockerContainer> findByStatus(String status);

    List<DockerContainer> findByOperatorName(String operatorName);

    List<DockerContainer> findByOperatorType(String operatorType);

    List<DockerContainer> findByHost(String host);

}
