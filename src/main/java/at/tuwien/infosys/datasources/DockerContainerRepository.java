package at.tuwien.infosys.datasources;


import at.tuwien.infosys.datasources.entities.DockerContainer;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface DockerContainerRepository extends CrudRepository<DockerContainer, Long> {

    List<DockerContainer> findByStatus(String status);
    List<DockerContainer> findByOperator(String operator);
    List<DockerContainer> findByHost(String host);

}
