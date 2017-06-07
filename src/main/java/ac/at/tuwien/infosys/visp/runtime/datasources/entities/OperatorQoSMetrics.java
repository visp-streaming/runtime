package ac.at.tuwien.infosys.visp.runtime.datasources.entities;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Data
@Entity
public class OperatorQoSMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    private String name;
    private String timestamp;
    private double processedMessages;
    private double receivedMessages;
    //time (in seconds) since the last measurement
    private double deltaSeconds;

    public OperatorQoSMetrics() {
    }

    public OperatorQoSMetrics(String name, String timestamp, double processedMessages, double receivedMessages) {
        super();
        this.name = name;
        this.timestamp = timestamp;
        this.processedMessages = processedMessages;
        this.receivedMessages = receivedMessages;
    }

}
