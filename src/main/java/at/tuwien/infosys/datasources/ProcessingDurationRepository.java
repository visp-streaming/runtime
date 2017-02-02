package at.tuwien.infosys.datasources;


import at.tuwien.infosys.datasources.entities.ProcessingDuration;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ProcessingDurationRepository extends CrudRepository<ProcessingDuration, Long> {

    List<ProcessingDuration> findFirst5ByOperatorOrderByIdDesc(String operator);

}
