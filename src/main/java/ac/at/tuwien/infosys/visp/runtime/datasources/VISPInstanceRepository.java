package ac.at.tuwien.infosys.visp.runtime.datasources;


import ac.at.tuwien.infosys.visp.runtime.datasources.entities.VISPInstance;
import org.springframework.data.repository.CrudRepository;

public interface VISPInstanceRepository extends CrudRepository<VISPInstance, Long> {

    VISPInstance findFirstByIp(String ip);


}
