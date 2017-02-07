package ac.at.tuwien.infosys.visp.runtime.datasources;


import ac.at.tuwien.infosys.visp.runtime.datasources.entities.OperatorReplicationReport;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface OperatorReplicationReportRepository extends CrudRepository<OperatorReplicationReport, Long> {

    List<OperatorReplicationReport> findFirstByNameOrderByTimestampDesc(String name);

}
