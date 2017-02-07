package ac.at.tuwien.infosys.visp.runtime.datasources;


import ac.at.tuwien.infosys.visp.runtime.datasources.entities.ScalingActivity;
import org.joda.time.DateTime;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ScalingActivityRepository extends CrudRepository<ScalingActivity, Long> {

    List<ScalingActivity> findByOperator(String operator);

    Long countByOperator(String operator);

    ScalingActivity findFirstByOrderByTimeAsc();

    List<ScalingActivity> findByTimeBetween(DateTime start, DateTime end);

}
