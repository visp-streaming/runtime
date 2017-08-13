package ac.at.tuwien.infosys.visp.runtime.resourceManagement.connectors.impl;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import ac.at.tuwien.infosys.visp.runtime.configuration.Configurationprovider;
import ac.at.tuwien.infosys.visp.runtime.datasources.PooledVMRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.ScalingActivityRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.DockerHost;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.PooledVM;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.ScalingActivity;
import ac.at.tuwien.infosys.visp.runtime.exceptions.ResourceException;
import ac.at.tuwien.infosys.visp.runtime.resourceManagement.connectors.ResourceConnector;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.Container;
import com.spotify.docker.client.messages.Image;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


@Service
public class ResourcePoolConnector extends ResourceConnector {

    @Value("${visp.simulated.startuptime}")
    private Integer startuptime;

    @Value("${visp.computational.resources.cleanuppool}")
    private Boolean cleanupPool;

    private String ressourcePoolName;

    @Autowired
    private ScalingActivityRepository sar;

    @Autowired
    private PooledVMRepository pvmr;

    @Autowired
    private Configurationprovider config;

    private static final Logger LOG = LoggerFactory.getLogger(OpenstackConnector.class);

    public void setRessourcePoolName(String ressourcePoolName) {
        this.ressourcePoolName = ressourcePoolName;
    }

    public DockerHost startVM(DockerHost dh) {

        if (dh == null) {
            dh = new DockerHost("additionaldockerhost");
            dh.setFlavour("m2.medium");
        }

        PooledVM availableVM = pvmr.findFirstByPoolnameAndLinkedhostIsNull(ressourcePoolName);

        if (availableVM == null) {
            LOG.error("There are too little VMs in the resourcePool.");
            throw new ResourceException("There are too little VMs in the resourcePool.");
        }

        dh.setResourcepool(ressourcePoolName);
        dh.setCores(availableVM.getCores());
        dh.setMemory(availableVM.getMemory());
        dh.setStorage(availableVM.getStorage());
        dh.setScheduledForShutdown(false);
        dh.setUrl(availableVM.getUrl());
        dh.setName(availableVM.getName());

        DateTime btuEnd = new DateTime(DateTimeZone.UTC);
        btuEnd = btuEnd.plusSeconds(Integer.valueOf(config.getBtu()) + (startuptime / 1000));
        dh.setBTUend(btuEnd);


        availableVM.setLinkedhost(dh.getName());

        dhr.save(dh);
        pvmr.save(availableVM);
        sar.save(new ScalingActivity("host", new DateTime(DateTimeZone.UTC), "", "startVM", dh.getName()));

        try {
            TimeUnit.MILLISECONDS.sleep(startuptime);
        } catch (InterruptedException ignore) {
            LOG.error("Host could not be selected from resourcepool");
        }
        return dh;
    }


    public final void stopDockerHost(final DockerHost dh) {

        PooledVM selectedVM = pvmr.findFirstByName(dh.getName());

        final DockerClient docker = DefaultDockerClient.builder().uri("http://" + dh.getUrl() + ":2375").connectTimeoutMillis(60000).build();


        List<Container> runningContainer = null;
        try {
            runningContainer = docker.listContainers(DockerClient.ListContainersParam.allContainers());
        } catch (DockerException | InterruptedException e) {
            LOG.error("containers could not be fetched ", e);
        }
        if (runningContainer != null) {
            for (Container container : runningContainer) {
                try {
                    docker.killContainer(container.id());
                } catch (DockerException | InterruptedException e) {
                    LOG.error("container " + container.id() + " could not be cleanedup", e);
                }
            }
        }

        if (runningContainer != null) {
            for (Container container : runningContainer) {
                try {
                    docker.removeContainer(container.id());
                } catch (DockerException | InterruptedException e) {
                    LOG.error("image " + container.id() + " could not be cleanedup", e);
                }
            }
        }

        if (cleanupPool) {
            List<Image> availableImages = new ArrayList<>();
            List<Image> danglingImages = new ArrayList<>();

            try {
                availableImages = docker.listImages(DockerClient.ListImagesParam.allImages());
                danglingImages = docker.listImages(DockerClient.ListImagesParam.danglingImages());
            } catch (DockerException | InterruptedException e) {
                LOG.error("Images could not be cleanedup", e);
            }

            for (Image img : availableImages) {
                deleteImage(docker, img);
            }
            for (Image img : danglingImages) {
                deleteImage(docker, img);
            }
        }

        selectedVM.setLinkedhost(null);

        pvmr.save(selectedVM);

        dhr.delete(dh);
        sar.save(new ScalingActivity("host", new DateTime(DateTimeZone.UTC), "", "stopVM", dh.getName()));
    }

    private void deleteImage(DockerClient docker, Image img) {
        try {
            docker.removeImage(img.id().replace("sha256:", ""));
        } catch (DockerException | InterruptedException e) {
            LOG.error("image " + img.id() + " could not be cleanedup");
        }
    }

    public void markHostForRemoval(DockerHost dh) {
        dh.setScheduledForShutdown(true);
        dh.setTerminationTime(new DateTime(DateTimeZone.UTC));
        dhr.save(dh);
    }

}
