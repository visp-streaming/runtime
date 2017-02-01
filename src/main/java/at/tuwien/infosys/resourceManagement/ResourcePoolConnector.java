package at.tuwien.infosys.resourceManagement;


import at.tuwien.infosys.datasources.PooledVMRepository;
import at.tuwien.infosys.datasources.ScalingActivityRepository;
import at.tuwien.infosys.entities.DockerHost;
import at.tuwien.infosys.entities.PooledVM;
import at.tuwien.infosys.entities.ScalingActivity;
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;


@Service
public class ResourcePoolConnector extends ResourceConnector {

    @Value("${visp.simulated.startuptime}")
    private Integer startuptime;

    @Value("${visp.btu}")
    private Integer BTU;

    @Value("${visp.computational.resources.cleanuppool}")
    private Boolean cleanupPool;

    @Autowired
    private OpenstackConnector opc;

    @Autowired
    private ScalingActivityRepository sar;


    @Autowired
    private PooledVMRepository pvmr;

    private static final Logger LOG = LoggerFactory.getLogger(OpenstackConnector.class);

    public DockerHost startVM(DockerHost dh) {
        PooledVM availableVM = pvmr.findFirstByLinkedhostIsNull();

        if (availableVM == null) {
            LOG.error("There are too little VMs in the resourcePool.");
            throw new RuntimeException("There are too little VMs in the resourcePool.");
        }

        dh.setCores(availableVM.getCores());
        dh.setRam(availableVM.getRam());
        dh.setStorage(availableVM.getStorage());
        dh.setScheduledForShutdown(false);
        dh.setUrl(availableVM.getUrl());
        dh.setName(availableVM.getName());

        DateTime btuEnd = new DateTime(DateTimeZone.UTC);
        btuEnd = btuEnd.plusSeconds(BTU + (startuptime / 1000));
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
        } catch (DockerException e) {
            LOG.error("containers cloud not be fetched ", e);
        } catch (InterruptedException e) {
            LOG.error("containers cloud not be fetched ", e);
        }
        for (Container container : runningContainer) {
             try {
             docker.killContainer(container.id());
             } catch (DockerException e) {
                 LOG.error("container " + container.id() + " could not be cleanedup", e);
             } catch (InterruptedException e) {
                 LOG.error("container " + container.id() + " could not be cleanedup", e);
             }
         }

        for (Container container : runningContainer) {
            try {
                docker.removeContainer(container.id());
            } catch (DockerException e) {
                LOG.error("image " + container.id() + " could not be cleanedup", e);
            } catch (InterruptedException e) {
                LOG.error("image " + container.id() + " could not be cleanedup", e);
            }
        }

        if (cleanupPool) {
            List<Image> availableImages = new ArrayList<>();
            List<Image> danglingImages = new ArrayList<>();

            try {
                availableImages = docker.listImages(DockerClient.ListImagesParam.allImages());
                danglingImages = docker.listImages(DockerClient.ListImagesParam.danglingImages());
            } catch (DockerException e) {
                LOG.error("Images could not be cleanedup", e);
            } catch (InterruptedException e) {
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
        sar.save(new ScalingActivity("host", new DateTime(DateTimeZone.UTC), "", "stopWM", dh.getName()));
    }

    private void deleteImage(DockerClient docker, Image img) {
        try {
            docker.removeImage(img.id().replace("sha256:", ""));
        } catch (DockerException e) {
            LOG.error("image " + img.id() + " could not be cleanedup");
        } catch (InterruptedException e) {
            LOG.error("image " + img.id() + " could not be cleanedup");
        }
    }

    public void markHostForRemoval(DockerHost dh) {
        dh.setScheduledForShutdown(true);
        dh.setTerminationTime(new DateTime(DateTimeZone.UTC));
        dhr.save(dh);
    }


    public void initializeVMs(Integer amount) {
        for (int i=0; i<amount; i++) {
            //TODO get actual specifications
            DockerHost dh = new DockerHost("dockerhost");
            dh.setFlavour("m2.medium");

            dh = opc.startVM(dh);
            PooledVM pvm = new PooledVM();
            pvm.setName(dh.getName());
            pvm.setUrl(dh.getUrl());
            pvm.setCores(dh.getCores());
            pvm.setRam(dh.getRam());
            pvm.setStorage(dh.getStorage());
            pvm.setFlavour(dh.getFlavour());
            pvmr.save(pvm);
        }
    }
}
