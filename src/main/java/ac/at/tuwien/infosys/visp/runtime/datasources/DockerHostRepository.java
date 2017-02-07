package ac.at.tuwien.infosys.visp.runtime.datasources;


import ac.at.tuwien.infosys.visp.runtime.datasources.entities.DockerHost;
import org.springframework.data.repository.CrudRepository;

public interface DockerHostRepository extends CrudRepository<DockerHost, Long> {

    DockerHost findFirstByName(String name);
}
