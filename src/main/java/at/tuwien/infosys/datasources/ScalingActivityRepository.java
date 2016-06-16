package at.tuwien.infosys.datasources;


import at.tuwien.infosys.entities.ScalingActivity;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ScalingActivityRepository extends CrudRepository<ScalingActivity, Long> {

    List<ScalingActivity> findByOperator(String operator);
    Long countByOperator(String operator);


}
