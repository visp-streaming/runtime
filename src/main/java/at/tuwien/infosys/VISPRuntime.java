package at.tuwien.infosys;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@PropertySource(value = {"classpath:/credential.properties"})
public class VISPRuntime {

    public static void main(String[] args) {
        SpringApplication.run(VISPRuntime.class, args);
    }
}
