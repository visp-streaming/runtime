package ac.at.tuwien.infosys.visp.runtime.datasources.entities;

import lombok.Data;
import org.hibernate.annotations.Type;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Data
@Entity
public class BTULogging {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @Type(type = "org.jadira.usertype.dateandtime.joda.PersistentDateTime")
    private DateTime timeStamp;

    private String operatorName;
    private Double overallFactor;
    private Double instanceFactor;
    private Double instanceFactorWeighted;
    private Integer instances;
    private Integer maxInstances;
    private Integer minInstances;
    private Double delayFactor;
    private Double delayFactorWeighted;
    private Double expectedDuration;
    private Double avgDuration;
    private Double relaxationFactor;
    private Double penaltyCost;
    private Double scalingFactor;
    private Double scalingFactorWeighted;
    private Long scalings;
    private Long totalscalings;
    private Integer queueFactor;
    private Integer queueLoad;


    public BTULogging(String operatorName, Double overallFactor, Double instanceFactor, Double instanceFactorWeighted, Integer instances, Integer maxInstances, Integer minInstances, Double delayFactor, Double delayFactorWeighted, Double expectedDuration, Double avgDuration, Double relaxationFactor, Double penaltyCost, Double scalingFactor, double scalingFactorWeighted, Long scalings, Long totalscalings, Integer queueFactor, Integer queueLoad) {
        this.timeStamp = new DateTime(DateTimeZone.UTC);
        this.operatorName = operatorName;
        this.overallFactor = overallFactor;
        this.instanceFactor = instanceFactor;
        this.instanceFactorWeighted = instanceFactorWeighted;
        this.instances = instances;
        this.maxInstances = maxInstances;
        this.minInstances = minInstances;
        this.delayFactor = delayFactor;
        this.delayFactorWeighted = delayFactorWeighted;
        this.expectedDuration = expectedDuration;
        this.avgDuration = avgDuration;
        this.relaxationFactor = relaxationFactor;
        this.penaltyCost = penaltyCost;
        this.scalingFactor = scalingFactor;
        this.scalingFactorWeighted = scalingFactorWeighted;
        this.scalings = scalings;
        this.totalscalings = totalscalings;
        this.queueFactor = queueFactor;
        this.queueLoad = queueLoad;
    }

    public BTULogging() {
    }
}
