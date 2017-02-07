package ac.at.tuwien.infosys.visp.runtime.monitoring;

import ac.at.tuwien.infosys.visp.runtime.datasources.DockerContainerMonitorRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.DockerContainerRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.DockerContainer;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.DockerContainerMonitor;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.PooledVM;
import ac.at.tuwien.infosys.visp.runtime.entities.ResourcePoolUsage;
import ac.at.tuwien.infosys.visp.runtime.entities.ResourceTriple;
import ac.at.tuwien.infosys.visp.runtime.datasources.PooledVMRepository;
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

    public ResourceTriple calculatelatestActualUsageForOperator(String operator) {
        ResourceTriple result = new ResourceTriple();
        DockerContainerMonitor dcm = dcmr.findFirstByOperatorOrderByTimestampDesc(operator);
        result.setCores((double) dcm.getCpuUsage());
        result.setMemory((int) dcm.getMemoryUsage());
        result.setStorage(0F);

        //CPUstats = usage in % of the assigned shares (from actual resources)
        return result;
    }

    public ResourceTriple calculateAverageUsageForOperator(String operator) {
        ResourceTriple result = new ResourceTriple();
        Integer counter = 0;

        for (DockerContainerMonitor dcm : dcmr.findByOperator(operator)) {
            result.incrementCores((double) dcm.getCpuUsage());
            result.incrementMemory((int) dcm.getMemoryUsage());
            result.incrementStorage(0F);
            counter++;
        }

        //CPUstats = usage in % of the assigned shares (from actual resources)

        result.divideForMultipleRecordings(counter);
        return result;
    }

}
