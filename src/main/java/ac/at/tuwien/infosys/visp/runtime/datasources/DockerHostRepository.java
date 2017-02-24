package ac.at.tuwien.infosys.visp.runtime.datasources;


import ac.at.tuwien.infosys.visp.runtime.datasources.entities.DockerHost;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface DockerHostRepository extends CrudRepository<DockerHost, Long> {

    DockerHost findFirstByName(String name);

    List<DockerHost> findByResourcepool(String name);
}
