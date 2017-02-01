package at.tuwien.infosys.resourceManagement;

import at.tuwien.infosys.datasources.DockerContainerRepository;
import at.tuwien.infosys.datasources.DockerHostRepository;
import at.tuwien.infosys.datasources.PooledVMRepository;
import at.tuwien.infosys.entities.DockerContainer;
import at.tuwien.infosys.entities.DockerHost;
import at.tuwien.infosys.entities.PooledVM;
import org.jvnet.hk2.annotations.Service;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class ResourceUsage {

    @Autowired
    private PooledVMRepository pvmr;

    @Autowired
    private DockerHostRepository dhr;

    @Autowired
    private DockerContainerRepository dcr;

    public void calculateUsageForPool(String resourcePoolName) {

        Double overallCores = 0.0;
        Integer overallRam = 0;
        Float overallStorage = 0.0F;

        Double plannedCoresUsage = 0.0;
        Integer plannedRamUsage = 0;
        Float plannedStorageUsage = 0.0F;

        Double actualCoresUsage = 0.0;
        Long actualRamUsage = 0L;
        Float actualStorageUsage = 0.0F;

        for (PooledVM pooledVM : pvmr.findByPoolName(resourcePoolName)) {
            DockerHost dh = dhr.findFirstByName(pooledVM.getLinkedhost());
            overallCores+=dh.getCores();
            overallRam+=dh.getRam();
            overallStorage+=dh.getStorage();

            for (DockerContainer dc : dcr.findByHost(dh.getName())) {
                plannedCoresUsage+=dc.getCpuCores();
                plannedRamUsage+=dc.getRam();
                plannedStorageUsage+=dc.getStorage();

                actualCoresUsage+=dc.getCpuUsage();
                actualRamUsage+= dc.getMemoryUsage();
            }
        }

        //TODO put the values in a DTO - discuss structure with hiessl

    }


}
