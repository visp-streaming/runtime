package at.tuwien.infosys.datasources.entities;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
public class PooledVM {


    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    private String poolname;
    private String name;
    private String url;
    private Double cores;
    private Integer memory;
    private Float storage;
    private String flavour;
    private String linkedhost;
    private Double cost;
    private Integer cpuFrequency;


    public PooledVM() {
        this.cost = 1.5;
        this.cpuFrequency = 2400;
    }
}
