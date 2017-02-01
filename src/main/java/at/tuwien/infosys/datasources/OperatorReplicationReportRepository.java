package at.tuwien.infosys.datasources;


import java.util.List;

import org.springframework.data.repository.CrudRepository;

import at.tuwien.infosys.datasources.entities.OperatorReplicationReport;

public interface OperatorReplicationReportRepository extends CrudRepository<OperatorReplicationReport, Long> {

    List<OperatorReplicationReport> findFirstByNameOrderByTimestampDesc(String name);

}
