package at.tuwien.infosys.datasources;


import at.tuwien.infosys.entities.PooledVM;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface PooledVMRepository extends CrudRepository<PooledVM, Long> {

    List<PooledVM> findByName(String name);
    List<PooledVM> findByLinkedhostIsNull();


}
