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
        if (overallFactor == Double.NaN) { this.overallFactor = 0.0; } else { this.overallFactor = overallFactor; }
        if (instanceFactor == Double.NaN) { this.instanceFactor = 0.0; } else { this.instanceFactor = instanceFactor; }
        if (instanceFactorWeighted == Double.NaN) { this.instanceFactorWeighted = 0.0; } else { this.instanceFactorWeighted = instanceFactorWeighted; }
        if (delayFactor == Double.NaN) { this.delayFactor = 0.0; } else { this.delayFactor = delayFactor; }
        if (delayFactorWeighted == Double.NaN) { this.delayFactorWeighted = 0.0; } else { this.delayFactorWeighted = delayFactorWeighted; }
        if (expectedDuration == Double.NaN) { this.expectedDuration = 0.0; } else { this.expectedDuration = expectedDuration; }
        if (avgDuration == Double.NaN) { this.avgDuration = 0.0; } else { this.avgDuration = avgDuration; }
        if (scalingFactor == Double.NaN) { this.scalingFactor = 0.0; } else { this.scalingFactor = scalingFactor; }
        if (scalingFactorWeighted == Double.NaN) { this.scalingFactorWeighted = 0.0; } else { this.scalingFactorWeighted = scalingFactorWeighted; }
        this.instances = instances;
        this.maxInstances = maxInstances;
        this.minInstances = minInstances;
        this.relaxationFactor = relaxationFactor;
        this.penaltyCost = penaltyCost;
        this.scalings = scalings;
        this.totalscalings = totalscalings;
        this.queueFactor = queueFactor;
        this.queueLoad = queueLoad;
    }

    public BTULogging() {
    }
}
