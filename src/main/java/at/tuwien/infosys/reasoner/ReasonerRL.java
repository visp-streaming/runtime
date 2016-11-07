package at.tuwien.infosys.reasoner;

import at.tuwien.infosys.reasoner.rl.CentralizedRLReasoner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
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
    
    @Scheduled(fixedRateString = "${visp.reasoning.timespan}")
    public synchronized void updateResourceconfiguration() {

        if (!reasoner.equals("rl")) {
            return;
        }

    	LOG.info(" + Run Adaptation Cycle");
    	rlReasoner.runAdaptationCycle();
    
    }
    
}
