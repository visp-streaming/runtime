package ac.at.tuwien.infosys.visp.runtime;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@PropertySource(value = {"classpath:/credential.properties"})
@DependsOn("datasourceconfig")
public class VISPRuntime {

    public static void main(String[] args) {
        SpringApplication.run(VISPRuntime.class, args);
    }
}
