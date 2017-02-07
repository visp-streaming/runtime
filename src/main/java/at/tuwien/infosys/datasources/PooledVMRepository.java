package at.tuwien.infosys.datasources;


import at.tuwien.infosys.datasources.entities.PooledVM;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface PooledVMRepository extends CrudRepository<PooledVM, Long> {

    @Query("select distinct poolname from PooledVM")
    List<String> findDistinctPoolnames();

    PooledVM findFirstByName(String name);

    PooledVM findFirstByPoolnameAndLinkedhostIsNull(String poolname);

    List<PooledVM> findByPoolname(String poolname);

}