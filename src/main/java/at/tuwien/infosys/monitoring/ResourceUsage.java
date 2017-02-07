package at.tuwien.infosys.monitoring;

import at.tuwien.infosys.datasources.DockerContainerMonitorRepository;
import at.tuwien.infosys.datasources.DockerContainerRepository;
import at.tuwien.infosys.datasources.PooledVMRepository;
import at.tuwien.infosys.datasources.entities.DockerContainer;
import at.tuwien.infosys.datasources.entities.DockerContainerMonitor;
import at.tuwien.infosys.datasources.entities.PooledVM;
import at.tuwien.infosys.entities.ResourcePoolUsage;
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


    private Double cost;
    private Integer cpuFrequency;
    private Double availability;


    public ResourcePoolUsage calculateUsageForPool(String resourcePoolName) {

        ResourcePoolUsage rp = new ResourcePoolUsage(resourcePoolName);
        ResourceTriple overall = new ResourceTriple();
        ResourceTriple planned = new ResourceTriple();
        ResourceTriple actual = new ResourceTriple();

        for (PooledVM pooledVM : pvmr.findByPoolname(resourcePoolName)) {
            if (rp.getCost() == null) {
                rp.setCost(pooledVM.getCost());
            }
            if (rp.getCpuFrequency() == null) {
                rp.setCpuFrequency(pooledVM.getCpuFrequency());
            }

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

        rp.setActualResources(actual);
        rp.setOverallResources(overall);
        rp.setPlannedResources(planned);

        //CPUstats = usage in % of the assigned shares (from actual resources)

        return rp;
    }

    public ResourceTriple calculateActualUsageForOperator(String operator) {
        ResourceTriple result = new ResourceTriple();
        for (DockerContainerMonitor dcm : dcmr.findByOperator(operator)) {
            result.incrementCores((double) dcm.getCpuUsage());
            result.incrementMemory((int) dcm.getMemoryUsage());
            result.incrementStorage(0F);
        }
        //CPUstats = usage in % of the assigned shares (from actual resources)
        return result;
    }


}
