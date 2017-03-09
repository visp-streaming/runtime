package ac.at.tuwien.infosys.visp.runtime.reasoner;

import ac.at.tuwien.infosys.visp.runtime.reasoner.rl.CentralizedRLReasoner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
@DependsOn("resourceProvider")
@ConditionalOnProperty(name = "visp.reasoner", havingValue = "rl")
public class ReasonerRL {

    @Autowired
    private CentralizedRLReasoner rlReasoner;

    private static final Logger LOG = LoggerFactory.getLogger(ReasonerRL.class);

    @PostConstruct
    public void init() {
    	LOG.info("Initializing RLReasoner");
    	rlReasoner.initialize();
    }
    
    @Scheduled(fixedRateString = "${visp.reasoning.timespan}")
    public synchronized void updateResourceconfiguration() {

    	LOG.info(" + Run Adaptation Cycle");
    	rlReasoner.runAdaptationCycle();
    
    }
    
}
