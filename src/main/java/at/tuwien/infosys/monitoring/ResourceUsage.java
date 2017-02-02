package at.tuwien.infosys.monitoring;

import at.tuwien.infosys.datasources.DockerContainerMonitorRepository;
import at.tuwien.infosys.datasources.DockerContainerRepository;
import at.tuwien.infosys.datasources.PooledVMRepository;
import at.tuwien.infosys.datasources.entities.DockerContainer;
import at.tuwien.infosys.datasources.entities.DockerContainerMonitor;
import at.tuwien.infosys.datasources.entities.PooledVM;
import at.tuwien.infosys.entities.ResourcePool;
import at.tuwien.infosys.entities.ResourceTriple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ResourceUsage {

    @Autowired
    private PooledVMRepository pvmr;

    @Autowired
    private DockerContainerMonitorRepository dcmr;

    @Autowired
    private DockerContainerRepository dcr;

    public ResourcePool calculateUsageForPool(String resourcePoolName) {

        ResourcePool rp = new ResourcePool(resourcePoolName);
        ResourceTriple overall = new ResourceTriple();
        ResourceTriple planned = new ResourceTriple();
        ResourceTriple actual = new ResourceTriple();

        for (PooledVM pooledVM : pvmr.findByPoolname(resourcePoolName)) {
            overall.incrementCores(pooledVM.getCores());
            overall.incrementMemory(pooledVM.getMemory());
            overall.incrementStorage(pooledVM.getStorage());

            for (DockerContainer dc : dcr.findByHost(pooledVM.getLinkedhost())) {
                DockerContainerMonitor dcm = dcmr.findFirstByContaineridOrderByTimestampDesc(dc.getContainerid());

                planned.incrementCores(dc.getCpuCores());
                planned.incrementMemory(dc.getMemory());
                planned.incrementStorage(Float.valueOf(dc.getStorage()));

                actual.incrementCores((double) dcm.getCpuUsage());
                actual.incrementMemory((int) dcm.getMemoryUsage());
                planned.incrementStorage((float) -1);
            }
        }

        actual.setMemory(actual.getMemory()/1024);

        //TODO clarify the actual CPU usage

        rp.setActualResources(actual);
        rp.setOverallResources(overall);
        rp.setPlannedResources(planned);

        return rp;
    }
}
