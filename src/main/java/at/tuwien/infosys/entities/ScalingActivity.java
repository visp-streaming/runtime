package at.tuwien.infosys.entities;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class ScalingActivity {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private long id;

    private String time;
    private String operator;
    private String scalingActivity;
    private String host;

    public ScalingActivity() {
    }

    public ScalingActivity(String time, String operator, String scalingActivity, String host) {
        this.time = time;
        this.operator = operator;
        this.scalingActivity = scalingActivity;
        this.host = host;
    }

    @Override
    public String toString() {
        return "ScalingActivity{" +
                "id=" + id +
                ", time='" + time + '\'' +
                ", operator='" + operator + '\'' +
                ", scalingActivity='" + scalingActivity + '\'' +
                ", host='" + host + '\'' +
                '}';
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getScalingActivity() {
        return scalingActivity;
    }

    public void setScalingActivity(String scalingActivity) {
        this.scalingActivity = scalingActivity;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }
}
