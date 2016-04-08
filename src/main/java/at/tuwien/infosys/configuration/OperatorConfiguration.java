package at.tuwien.infosys.configuration;


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
    }

    public String getImage(String operator) {
        return configuration.get(operator);
    }
}
