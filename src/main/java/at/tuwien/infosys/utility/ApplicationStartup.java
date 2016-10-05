package at.tuwien.infosys.utility;

import at.tuwien.infosys.datasources.PooledVMRepository;
import at.tuwien.infosys.entities.PooledVM;
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

    @Autowired
    private PooledVMRepository pvmr;


    @Override
    public void onApplicationEvent(final ContextRefreshedEvent event) {


        //utility.createInitialStatus();

        resetPooledVMs();

    }


    private void resetPooledVMs() {
        for(PooledVM vm : pvmr.findAll()) {

            vm.setLinkedhost(null);
            pvmr.save(vm);
        }
    }
}
