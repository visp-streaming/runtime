package at.tuwien.infosys.entities;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class OperatorQoSMetrics {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private long id;

    private String name;
    private String timestamp;
    private double processedMessages;
    private double receivedMessages;

    public OperatorQoSMetrics() {
    }

	public OperatorQoSMetrics(String name, String timestamp,
			double processedMessages, double receivedMessages) {
		super();
		this.name = name;
		this.timestamp = timestamp;
		this.processedMessages = processedMessages;
		this.receivedMessages = receivedMessages;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public double getProcessedMessages() {
		return processedMessages;
	}

	public void setProcessedMessages(double processedMessages) {
		this.processedMessages = processedMessages;
	}

	public double getReceivedMessages() {
		return receivedMessages;
	}

	public void setReceivedMessages(double receivedMessages) {
		this.receivedMessages = receivedMessages;
	}

	@Override
    public String toString() {
        return "ost{" +
	                + id +
	                ", " + name +
	                ", "+ timestamp +
	                ", r:"+ receivedMessages +
	                ", p:"+ processedMessages +
                '}';
    }
}
