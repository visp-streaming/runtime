package at.tuwien.infosys.datasources;


import at.tuwien.infosys.entities.DockerHost;
import org.springframework.data.repository.CrudRepository;

public interface DockerHostRepository extends CrudRepository<DockerHost, Long> {

    DockerHost findFirstByName(String name);
}
