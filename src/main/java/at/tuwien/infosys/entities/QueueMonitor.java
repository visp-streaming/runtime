package at.tuwien.infosys.entities;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class QueueMonitor {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private long id;

    private String time;
    private String operator;
    private String queue;
    private Integer amount;

    public QueueMonitor() {
    }

    public QueueMonitor(String time, String operator, String queue, Integer amount) {
        this.time = time;
        this.operator = operator;
        this.queue = queue;
        this.amount = amount;
    }

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
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

    public String getQueue() {
        return queue;
    }

    public void setQueue(String queue) {
        this.queue = queue;
    }
}
