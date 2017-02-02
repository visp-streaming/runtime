package at.tuwien.infosys.datasources.entities;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

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

    public PooledVM() {
    }

}
