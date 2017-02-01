package at.tuwien.infosys.datasources;


import at.tuwien.infosys.entities.PooledVM;
import org.springframework.data.repository.CrudRepository;

public interface PooledVMRepository extends CrudRepository<PooledVM, Long> {

    PooledVM findFirstByName(String name);
    PooledVM findFirstByPoolnameAndLinkedhostIsNull(String poolname);


}