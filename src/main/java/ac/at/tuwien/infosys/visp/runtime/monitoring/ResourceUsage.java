package ac.at.tuwien.infosys.visp.runtime.monitoring;

import ac.at.tuwien.infosys.visp.common.resources.ResourcePoolUsage;
import ac.at.tuwien.infosys.visp.common.resources.ResourceTriple;
import ac.at.tuwien.infosys.visp.runtime.configuration.OperatorConfigurationBootstrap;
import ac.at.tuwien.infosys.visp.runtime.datasources.DockerContainerMonitorRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.DockerContainerRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.PooledVMRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.DockerContainer;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.DockerContainerMonitor;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.PooledVM;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ResourceUsage {

    @Autowired
    private PooledVMRepository pvmr;

    @Autowired
    private DockerContainerMonitorRepository dcmr;

    @Autowired
    private DockerContainerRepository dcr;

    @Autowired
    private OperatorConfigurationBootstrap opconf;

    public ResourcePoolUsage calculateUsageForPool(String resourcePoolName) {

        if (pvmr.findByPoolname(resourcePoolName).isEmpty()) {
            return null;
        }

        ResourcePoolUsage rp = new ResourcePoolUsage(resourcePoolName);
        //TODO calculate the availability
        rp.setAvailability((Math.floor(80 + Math.random() * (99 - 80 + 1))) / 100);
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
                planned.incrementCores(dc.getCpuCores());
                planned.incrementMemory(dc.getMemory());
                planned.incrementStorage(Float.valueOf(dc.getStorage()));

                DockerContainerMonitor dcm = dcmr.findFirstByContaineridOrderByTimestampDesc(dc.getContainerid());

                if (dcm != null) {
                    actual.incrementCores(dcm.getCpuUsage());
                    actual.incrementMemory((int) dcm.getMemoryUsage());

                    //TODO fix as soon docker provides information about the actual used storage size
                    actual.incrementStorage(Float.valueOf(dc.getStorage()));
                }
            }
        }

        actual.setMemory(actual.getMemory());
        //CPUstats = usage in % of the assigned shares (from actual resources) from the monitor
        actual.setCores(planned.getCores() * actual.getCores() / 100);

        rp.setActualResources(actual);
        rp.setOverallResources(overall);
        rp.setPlannedResources(planned);


        return rp;
    }

    public ResourceTriple calculatelatestActualUsageForOperatorType(String operator) {
        DockerContainerMonitor dcm = dcmr.findFirstByOperatorOrderByTimestampDesc(operator);
        return getResourceTriple(dcm);
    }

    public ResourceTriple calculatelatestActualUsageForOperatorid(String operatorid) {
        DockerContainerMonitor dcm = dcmr.findFirstByOperatoridOrderByTimestampDesc(operatorid);
        return getResourceTriple(dcm);
    }


    private ResourceTriple getResourceTriple(DockerContainerMonitor dcm) {
        ResourceTriple result = new ResourceTriple();
        //CPUstats = usage in % of the assigned shares (from actual resources)
        result.setMemory((int) dcm.getMemoryUsage());

        ResourceTriple planned = opconf.getExpected(dcm.getOperator());
        result.setStorage(planned.getStorage());
        result.setCores(planned.getCores() * dcm.getCpuUsage() / 100);

        return result;
    }

    public ResourceTriple calculateAverageUsageForOperatorType(String operator) {
        List<DockerContainerMonitor> recordings = dcmr.findByOperator(operator);
        return getResourceTriple(recordings);
    }

    public ResourceTriple calculateAverageUsageForOperatorID(String operatorid) {
        List<DockerContainerMonitor> recordings = dcmr.findByOperatorid(operatorid);
        return getResourceTriple(recordings);
    }

    private ResourceTriple getResourceTriple(List<DockerContainerMonitor> recordings) {
        ResourceTriple result = new ResourceTriple();
        Integer counter = 0;

        for (DockerContainerMonitor dcm : recordings) {
            result.incrementCores(dcm.getCpuUsage());
            result.incrementMemory((int) dcm.getMemoryUsage());

            ResourceTriple planned = opconf.getExpected(dcm.getOperator());
            result.incrementStorage(planned.getStorage());
            counter++;
        }

        //CPUstats = usage in % of the assigned shares (from actual resources)

        result.divideForMultipleRecordings(counter);
        return result;
    }

}
