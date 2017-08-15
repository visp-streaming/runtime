package ac.at.tuwien.infosys.visp.runtime.datasources;


import java.util.List;

import ac.at.tuwien.infosys.visp.runtime.datasources.entities.DockerContainer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DockerContainerRepository extends JpaRepository<DockerContainer, Long> {

    List<DockerContainer> findByStatus(String status);

    List<DockerContainer> findByOperatorName(String operatorName);

    List<DockerContainer> findByOperatorType(String operatorType);

    List<DockerContainer> findByOperatorNameAndStatus(String operatorName, String status);
    
    List<DockerContainer> findByOperatorTypeAndStatus(String operatorType, String status);

    List<DockerContainer> findByHost(String host);

    List<DockerContainer> findByHostAndStatus(String host, String status);

    DockerContainer findFirstById(Long id);


    @Query("SELECT dc FROM DockerContainer dc, DockerHost dh where dh.name = dc.host and dh.resourcepool = :resourcepool and dc.status = 'running' and dc.operatorName = :operatorname")
    List<DockerContainer> findAllRunningByOperatorNameAndResourcepool(@Param("operatorname") String operatorName, @Param("resourcepool") String resourcepool);

    @Query("SELECT dc FROM DockerContainer dc, DockerHost dh where dh.name = dc.host and dh.resourcepool = :resourcepool and dc.status = 'running' and dc.operatorType = :operatortype")
    List<DockerContainer> findAllRunningByOperatorTypeAndResourcepool(@Param("operatortype") String operatorType, @Param("resourcepool") String resourcepool);

}
