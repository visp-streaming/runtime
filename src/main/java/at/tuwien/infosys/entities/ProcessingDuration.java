package at.tuwien.infosys.entities;

import org.hibernate.annotations.Type;
import org.joda.time.DateTime;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class ProcessingDuration {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private long id;

    @Type(type = "org.jadira.usertype.dateandtime.joda.PersistentDateTime")
    private DateTime time;

    private String operator;
    private Double duration;

    public ProcessingDuration() {
    }

    public ProcessingDuration(DateTime time, String operator, Double duration) {
        this.time = time;
        this.operator = operator;
        this.duration = duration;
    }

    public DateTime getTime() {
        return time;
    }

    public void setTime(DateTime time) {
        this.time = time;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public Double getDuration() {
        return duration;
    }

    public void setDuration(Double duration) {
        this.duration = duration;
    }

    @Override
    public String toString() {
        return "ProcessingDuration{" +
                "id=" + id +
                ", time='" + time + '\'' +
                ", operator='" + operator + '\'' +
                ", duration=" + duration +
                '}';
    }
}
