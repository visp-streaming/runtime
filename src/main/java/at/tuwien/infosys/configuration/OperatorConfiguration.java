package at.tuwien.infosys.configuration;


import at.tuwien.infosys.entities.DockerContainer;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Service
public class OperatorConfiguration {

    private HashMap<String, String> imageConfiguration = new HashMap<>();
    private HashMap<String, Integer> queueSLA = new HashMap<>();
    private HashMap<String, Double> durationSLA = new HashMap<>();



    public OperatorConfiguration() {
        imageConfiguration.put("speed", "chochreiner/vispprocessingnodes");
        imageConfiguration.put("avgSpeed", "chochreiner/vispprocessingnodes");
        imageConfiguration.put("aggregation", "chochreiner/vispprocessingnodes");
        imageConfiguration.put("distance", "chochreiner/vispprocessingnodes");
        imageConfiguration.put("analysis", "chochreiner/vispprocessingnodes");
        imageConfiguration.put("monitor", "chochreiner/vispprocessingnodes");
        imageConfiguration.put("step1", "chochreiner/vispprocessingnodes");
        imageConfiguration.put("step2", "chochreiner/vispprocessingnodes");
        imageConfiguration.put("step3", "chochreiner/vispprocessingnodes");
        imageConfiguration.put("step4", "chochreiner/vispprocessingnodes");
        imageConfiguration.put("step5", "chochreiner/vispprocessingnodes");
        imageConfiguration.put("log", "chochreiner/vispprocessingnodes");
        imageConfiguration.put("source", "chochreiner/vispprocessingnodes");


        //TODO read SLA from topology

        queueSLA.put("speed", 100);
        queueSLA.put("avgSpeed", 100);
        queueSLA.put("aggregation", 100);
        queueSLA.put("distance", 100);
        queueSLA.put("analysis", 100);
        queueSLA.put("monitor", 100);
        queueSLA.put("step1", 100);
        queueSLA.put("step2", 100);
        queueSLA.put("step3", 100);
        queueSLA.put("step4", 100);
        queueSLA.put("step5", 100);
        queueSLA.put("log", 100);
        queueSLA.put("source", 100);


        durationSLA.put("speed", 0.5);
        durationSLA.put("avgSpeed", 0.5);
        durationSLA.put("aggregation", 0.5);
        durationSLA.put("distance", 0.5);
        durationSLA.put("analysis", 0.5);
        durationSLA.put("monitor", 0.5);
        durationSLA.put("step1", 1.0);
        durationSLA.put("step2", 2.0);
        durationSLA.put("step3", 3.0);
        durationSLA.put("step4", 4.0);
        durationSLA.put("step5", 5.0);
        durationSLA.put("log", 0.5);
        durationSLA.put("source", 0.5);


    }

    public String getImage(String operator) {
        return imageConfiguration.get(operator);
    }

    public Integer getQueueSLA(String operator) {
        return queueSLA.get(operator);
    }

    public Double getDurationSLA(String operator) {
        return durationSLA.get(operator);
    }


    public DockerContainer createDockerContainerConfiguration(String operator) {

        return new DockerContainer(operator, 0.45, 300, 1);
    }

}
