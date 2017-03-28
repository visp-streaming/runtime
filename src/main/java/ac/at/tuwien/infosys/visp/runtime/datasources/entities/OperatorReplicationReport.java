package ac.at.tuwien.infosys.visp.runtime.datasources.entities;

import com.google.common.base.Joiner;
import lombok.Data;
import reled.model.Resource;
import reled.model.ResourcePool;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
public class OperatorReplicationReport {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    private String name;
    private String timestamp;
    private int numberOfReplicas;
    private double averageUtilization;
    private String utilization;

    public OperatorReplicationReport() {
    }

    public OperatorReplicationReport(String name, String timestamp, ResourcePool deployment) {
        super();
        this.name = name;
        this.timestamp = timestamp;
        if (deployment != null && deployment.getResources() != null) {
            this.numberOfReplicas = deployment.getResources().size();
            this.averageUtilization = deployment.getAverageUtilization();

            List<Double> utils = new ArrayList<>();
            for (Resource r : deployment.getResources()) {
                utils.add(r.getUtilization());
            }
            this.utilization = Joiner.on(',').join(utils);
        } else {
            this.numberOfReplicas = 0;
            this.averageUtilization = 0;
            this.utilization = "";
        }
    }

}
