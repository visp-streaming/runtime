package ac.at.tuwien.infosys.visp.runtime.datasources;


import ac.at.tuwien.infosys.visp.runtime.datasources.entities.VISPConnection;
import org.springframework.data.repository.CrudRepository;

public interface VISPConnectionRepository extends CrudRepository<VISPConnection, Long> {


}
