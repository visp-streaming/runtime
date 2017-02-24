package ac.at.tuwien.infosys.visp.runtime.reasoner;

import ac.at.tuwien.infosys.visp.runtime.reasoner.rl.CentralizedRLReasoner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
@DependsOn("resourceProvider")
public class ReasonerRL {

    @Autowired
    private CentralizedRLReasoner rlReasoner;

    @Value("${visp.reasoner}")
    private String reasoner;

    private static final Logger LOG = LoggerFactory.getLogger(ReasonerRL.class);

    @PostConstruct
    public void init() {
    	LOG.info("Initializing RLReasoner");
    	rlReasoner.initialize();
    }
    
    //@Scheduled(fixedRateString = "${visp.reasoning.timespan}")
    public synchronized void updateResourceconfiguration() {

        if (!reasoner.equals("rl")) {
            return;
        }

    	LOG.info(" + Run Adaptation Cycle");
    	rlReasoner.runAdaptationCycle();
    
    }
    
}
