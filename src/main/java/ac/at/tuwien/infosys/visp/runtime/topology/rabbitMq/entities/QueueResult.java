package ac.at.tuwien.infosys.visp.runtime.topology.rabbitMq.entities;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class QueueResult {
    /**
     * DTO representing the result of the rabbitmq API call
     */
    String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "QueueResult{" +
                "name='" + name + '\'' +
                '}';
    }
}
