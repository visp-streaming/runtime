package at.tuwien.infosys.entities;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import reled.model.Resource;
import reled.model.ResourcePool;

import com.google.common.base.Joiner;

@Entity
public class OperatorReplicationReport {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private long id;

    private String name;
    private String timestamp;
    private int numberOfReplicas; 
    private double averageUtilization;
    private String utilization;

    public OperatorReplicationReport() {
    }

	public OperatorReplicationReport(String name, String timestamp,
			ResourcePool deployment) {
		super();
		this.name = name;
		this.timestamp = timestamp;
		if (deployment != null && deployment.getResources() != null){
			this.numberOfReplicas = deployment.getResources().size();
			this.averageUtilization = deployment.getAverageUtilization();
			
			List<Double> utils = new ArrayList<Double>();
			for (Resource r : deployment.getResources()){
				utils.add(r.getUtilization());
			}
			this.utilization = Joiner.on(',').join(utils);
		} else {
			this.numberOfReplicas 	= 0;
			this.averageUtilization = 0;
			this.utilization = "";
		}
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

	public int getNumberOfReplicas() {
		return numberOfReplicas;
	}

	public void setNumberOfReplicas(int numberOfReplicas) {
		this.numberOfReplicas = numberOfReplicas;
	}

	public double getAverageUtilization() {
		return averageUtilization;
	}

	public void setAverageUtilization(double averageUtilization) {
		this.averageUtilization = averageUtilization;
	}

	public String getUtilization() {
		return utilization;
	}

	public void setUtilization(String utilization) {
		this.utilization = utilization;
	}

	@Override
    public String toString() {
        return "or{" +
	                + id +
	                ", " + name +
	                ", "+ timestamp +
	                ", repl:" + numberOfReplicas +
	                ", u:" + averageUtilization +
	                ", [" + utilization + "]" +
                '}';
    }
}
