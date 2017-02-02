package at.tuwien.infosys.datasources.entities;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Data
@Entity
public class ApplicationQoSMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    private String timestamp;
    private String applicationName;
    private Double averageResponseTime;

    public ApplicationQoSMetrics() {
    }

    public ApplicationQoSMetrics(String time, String applicationName, Double averageResponseTime) {
        super();
        this.timestamp = time;
        this.applicationName = applicationName;
        this.averageResponseTime = averageResponseTime;
    }

}
