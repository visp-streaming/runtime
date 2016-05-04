package at.tuwien.infosys.utility;

import at.tuwien.infosys.topology.TopologyManagement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class ApplicationStartup implements ApplicationListener<ApplicationStartedEvent> {

    @Value("${visp.infrastructurehost}")
    private String infrastructureHost;

    @Autowired
    private Utilities utility;

    @Autowired
    private TopologyManagement tmgmt;

    @Override
    public void onApplicationEvent(final ApplicationStartedEvent event) {
        //tmgmt.cleanup(infrastructureHost);
        //utility.createInitialStatus();
    }
}
