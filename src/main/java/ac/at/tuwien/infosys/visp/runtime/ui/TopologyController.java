package ac.at.tuwien.infosys.visp.runtime.ui;


import ac.at.tuwien.infosys.visp.runtime.datasources.DockerHostRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.PooledVMRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.DockerHost;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.PooledVM;
import ac.at.tuwien.infosys.visp.runtime.resourceManagement.ResourceProvider;
import ac.at.tuwien.infosys.visp.runtime.resourceManagement.connectors.impl.OpenstackConnector;
import ac.at.tuwien.infosys.visp.runtime.topology.TopologyManagement;
import ac.at.tuwien.infosys.visp.runtime.topology.TopologyUpdateHandler;
import ac.at.tuwien.infosys.visp.runtime.ui.entities.CreatePooledvmForm;
import ac.at.tuwien.infosys.visp.topologyParser.TopologyParser;
import com.sun.org.apache.xml.internal.security.utils.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.OK;

@Controller
public class TopologyController {


    @Autowired
    private ResourceProvider rp;

    @Autowired
    TopologyUpdateHandler topologyUpdateHandler;

    @Autowired
    TopologyParser topologyParser;

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());


    @RequestMapping("/changeTopology")
    public String index(Model model) throws SchedulerException {
        try {
            String graphvizImage = Base64.encode(FileUtils.readFileToByteArray(new File(topologyParser.getCurrentGraphvizPngFile())));
            model.addAttribute("currentTopologyImage", graphvizImage);
        } catch (IOException e) {
            LOG.error("Unable to load graphviz image", e);
        }
        return "changeTopology";
    }

    @RequestMapping(value = "/uploadTopologyGUI", method = RequestMethod.POST)
    public String uploadTopology(Model model,
            @RequestParam("file") MultipartFile file) {
        /**
         * this method is used to upload an updated topology description file
         * it is forwarded to the service class where it will cause a topology update
         */

        try {
            ByteArrayInputStream stream = new ByteArrayInputStream(file.getBytes());
            String fileContent = IOUtils.toString(stream, "UTF-8");
            TopologyUpdateHandler.UpdateResult result = topologyUpdateHandler.handleUpdate(fileContent);
            model.addAttribute("updateResult", result);
            if (result.pathToGraphviz != null) {
                String graphvizImage = Base64.encode(FileUtils.readFileToByteArray(new File(result.pathToGraphviz)));
                model.addAttribute("graphvizImage", graphvizImage);
            } else {
                model.addAttribute("graphvizAvailable", false);
            }
        }
        catch (Exception e) {
            LOG.error(e.getLocalizedMessage());
        }

        return "afterTopologyUpdate";
    }



}
