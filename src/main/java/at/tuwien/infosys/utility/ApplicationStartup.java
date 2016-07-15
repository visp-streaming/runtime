package at.tuwien.infosys.utility;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

@Component
public class ApplicationStartup implements ApplicationListener<ContextRefreshedEvent> {

    @Value("${visp.infrastructurehost}")
    private String infrastructureHost;

    @Autowired
    private Utilities utility;


    @Override
    public void onApplicationEvent(final ContextRefreshedEvent event) {


        utility.createInitialStatus();
    }
}
