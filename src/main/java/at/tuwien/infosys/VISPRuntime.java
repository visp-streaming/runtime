package at.tuwien.infosys;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.annotation.PreDestroy;

@SpringBootApplication
@EnableScheduling
public class VISPRuntime {

    public static void main(String[] args) {
        SpringApplication.run(VISPRuntime.class, args);
    }

    @PreDestroy
    public void exportData() {
        //TODO implement Dataexport functionality and generate graphics


    }

}
