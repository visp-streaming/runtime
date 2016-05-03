package at.tuwien.infosys.configuration;


import at.tuwien.infosys.entities.DockerContainer;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Service
public class OperatorConfiguration {

    private HashMap<String, String> configuration = new HashMap<>();

    public OperatorConfiguration() {
        configuration.put("speed", "chochreiner/vispprocessingnodes");
        configuration.put("avgSpeed", "chochreiner/vispprocessingnodes");
        configuration.put("aggregation", "chochreiner/vispprocessingnodes");
        configuration.put("distance", "chochreiner/vispprocessingnodes");
        configuration.put("analysis", "chochreiner/vispprocessingnodes");
        configuration.put("monitor", "chochreiner/vispprocessingnodes");
        configuration.put("step1", "chochreiner/vispprocessingnodes");
        configuration.put("step2", "chochreiner/vispprocessingnodes");
        configuration.put("step3", "chochreiner/vispprocessingnodes");
        configuration.put("step4", "chochreiner/vispprocessingnodes");
        configuration.put("step5", "chochreiner/vispprocessingnodes");
        configuration.put("log", "chochreiner/vispprocessingnodes");
    }

    public String getImage(String operator) {
        return configuration.get(operator);
    }

    public DockerContainer createDockerContainerConfiguration(String operator) {

        return new DockerContainer(operator, 0.45, 300, 1);
    }

}
