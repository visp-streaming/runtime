package ac.at.tuwien.infosys.visp.runtime.ui.fragements;

import ac.at.tuwien.infosys.visp.runtime.configuration.Configurationprovider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import java.util.Arrays;
import java.util.List;

@Configuration
@DependsOn("configurationprovider")
public class Header {

    @Autowired
    private Configurationprovider config;

    public String getHeader() {
        return "VISP Runtime - " + config.getRuntimeIP();
    }

}
