package ac.at.tuwien.infosys.visp.runtime.datasources;


import ac.at.tuwien.infosys.visp.runtime.datasources.entities.ProcessingDuration;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ProcessingDurationRepository extends CrudRepository<ProcessingDuration, Long> {

    List<ProcessingDuration> findFirst5ByOperatorOrderByIdDesc(String operator);

}
