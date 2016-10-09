package at.tuwien.infosys.entities;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class ApplicationQoSMetrics {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private long id;

    private String timestamp;
    private String applicationName;
    private Double averageResponseTime;

    public ApplicationQoSMetrics() {
    }
    
	public ApplicationQoSMetrics(String time, String applicationName,
			Double averageResponseTime) {
		super();
		this.timestamp = time;
		this.applicationName = applicationName;
		this.averageResponseTime = averageResponseTime;
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

	public String getApplicationName() {
		return applicationName;
	}

	public void setApplicationName(String applicationName) {
		this.applicationName = applicationName;
	}

	public Double getAverageResponseTime() {
		return averageResponseTime;
	}

	public void setAverageResponseTime(Double averageResponseTime) {
		this.averageResponseTime = averageResponseTime;
	}

	@Override
    public String toString() {
        return "App{" +
	                "id=" + id +
	                ", applicationId='" + applicationName + '\'' +
	                ", timestamp='" + timestamp + '\'' +
	                ", averageRespTime='" + averageResponseTime + '\'' +
                '}';
    }
}
