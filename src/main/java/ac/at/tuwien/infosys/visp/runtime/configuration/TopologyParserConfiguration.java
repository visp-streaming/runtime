package ac.at.tuwien.infosys.visp.runtime.configuration;

import ac.at.tuwien.infosys.visp.topologyParser.TopologyParser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TopologyParserConfiguration {
    @Bean
    public TopologyParser topologyParser() {
        return new TopologyParser();
    }
}
