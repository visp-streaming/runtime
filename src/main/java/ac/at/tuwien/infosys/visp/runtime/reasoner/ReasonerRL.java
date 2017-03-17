package ac.at.tuwien.infosys.visp.runtime.reasoner;

import ac.at.tuwien.infosys.visp.runtime.configuration.Configurationprovider;
import ac.at.tuwien.infosys.visp.runtime.reasoner.rl.CentralizedRLReasoner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
@DependsOn({"configurationprovider","resourceProvider"})
public class ReasonerRL {

    @Autowired
    private CentralizedRLReasoner rlReasoner;

    private static final Logger LOG = LoggerFactory.getLogger(ReasonerRL.class);

    @Autowired
    private Configurationprovider config;

    @PostConstruct
    public void init() {
    	LOG.info("Initializing RLReasoner");
    	rlReasoner.initialize();
    }
    
    @Scheduled(fixedRateString = "${visp.reasoning.timespan}")
    public synchronized void updateResourceconfiguration() {
        if (!config.getReasoner().equals("rl")) {
            return;
        }

    	LOG.info(" + Run Adaptation Cycle");
    	rlReasoner.runAdaptationCycle();
    
    }
    
}
