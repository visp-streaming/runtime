package at.tuwien.infosys.datasources;


import at.tuwien.infosys.datasources.entities.OperatorReplicationReport;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface OperatorReplicationReportRepository extends CrudRepository<OperatorReplicationReport, Long> {

    List<OperatorReplicationReport> findFirstByNameOrderByTimestampDesc(String name);

}
