package at.tuwien.infosys.datasources.entities;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Data
@Entity
public class VISPConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    private String start;
    private String end;
    private Double delay;
    private Double dataRate;
    private Boolean availability;

    public VISPConnection() {
        this.availability = true;
    }

    public VISPConnection(String start, String end, Double delay, Double dataRate) {
        this.start = start;
        this.end = end;
        this.delay = delay;
        this.dataRate = dataRate;
        this.availability = true;
    }
}
