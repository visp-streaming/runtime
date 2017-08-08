package ac.at.tuwien.infosys.visp.runtime.datasources.entities;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import lombok.Data;
import org.hibernate.annotations.Type;
import org.joda.time.DateTime;

@Data
@Entity
public class QueueMonitor {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @Type(type = "org.jadira.usertype.dateandtime.joda.PersistentDateTime")
    private DateTime time;

    private String operator;
    private String queue;
    private Integer amount;
    private Double incomingRate;
    private Double deliveryRate;


    public QueueMonitor() {
    }

    public QueueMonitor(DateTime time, String operator, String queue, Integer amount, Double incomingRate, Double deliveryRate) {
        this.time = time;
        this.operator = operator;
        this.queue = queue;
        this.amount = amount;
        this.incomingRate = incomingRate;
        this.deliveryRate = deliveryRate;
    }

}
