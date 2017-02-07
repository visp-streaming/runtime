package at.tuwien.infosys.datasources;


import at.tuwien.infosys.datasources.entities.VISPInstance;
import org.springframework.data.repository.CrudRepository;

public interface VISPInstanceRepository extends CrudRepository<VISPInstance, Long> {

    VISPInstance findFirstByUri(String uri);


}
