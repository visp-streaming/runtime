package ac.at.tuwien.infosys.visp.runtime.datasources;


import ac.at.tuwien.infosys.visp.runtime.datasources.entities.RuntimeConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RuntimeConfigurationRepository extends JpaRepository<RuntimeConfiguration, Long> {

    RuntimeConfiguration findFirstByKey(String key);

}
