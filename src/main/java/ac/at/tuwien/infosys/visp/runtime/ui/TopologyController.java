package ac.at.tuwien.infosys.visp.runtime.ui;


import ac.at.tuwien.infosys.visp.runtime.resourceManagement.ResourceProvider;
import ac.at.tuwien.infosys.visp.runtime.topology.TopologyManagement;
import ac.at.tuwien.infosys.visp.runtime.topology.TopologyUpdateHandler;
import ac.at.tuwien.infosys.visp.runtime.topology.rabbitMq.UpdateResult;
import ac.at.tuwien.infosys.visp.topologyParser.TopologyParser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Controller
public class TopologyController {


    @Autowired
    private ResourceProvider rp;

    @Autowired
    TopologyUpdateHandler topologyUpdateHandler;

    @Autowired
    TopologyParser topologyParser;

    @Autowired
    TopologyManagement topologyManagement;

    @Value("${visp.runtime.ip}")
    private String runtimeip;

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    @RequestMapping("/changeTopology")
    public String index(Model model) throws SchedulerException {
        model.addAttribute("pagetitle", "VISP Runtime - " + runtimeip);
        //model.addAttribute("dotContent", "digraph {         \"step1\" -> \"step2\"                 \"step2\" [style=filled, fontname=\"helvetica\", shape=box, fillcolor=skyblue, label=<step2<BR />         <FONT POINT-SIZE=\"10\">128.130.172.181/openstackpool</FONT>>]         \"step2\" -> \"log\"                 \"log\" [style=filled, fontname=\"helvetica\", shape=box, fillcolor=springgreen, label=<log<BR />         <FONT POINT-SIZE=\"10\">128.130.172.181/openstackpool</FONT>>]         \"source\" -> \"step1\"                 \"step1\" [style=filled, fontname=\"helvetica\", shape=box, fillcolor=skyblue, label=<step1<BR />         <FONT POINT-SIZE=\"10\">128.130.172.222/openstackpool</FONT>>]                 \"source\" [style=filled, fontname=\"helvetica\", shape=box, fillcolor=beige, label=<source<BR />         <FONT POINT-SIZE=\"10\">128.130.172.181/openstackpool</FONT>>]          }");


        if(topologyManagement.getTopology().size() == 0) {
            model.addAttribute("emptyTopology", true);
        } else {
            model.addAttribute("emptyTopology", false);
            try {
                model.addAttribute("dotContent", getTopologyForVizJs(topologyManagement.getDotfile()));
            } catch (Exception e) {
                LOG.error("Unable to load graphviz image", e);
            }
        }
        return "changeTopology";
    }

    private String getTopologyForVizJs(String dotFilePath) throws IOException {
        String dotContent = new String(Files.readAllBytes(Paths.get(dotFilePath)));
        dotContent = dotContent.replaceAll("[\\t\\n\\r]"," ");
        dotContent = dotContent.replaceAll("\"","\\\"");
        return dotContent;
    }

    @RequestMapping(value = "/uploadTopologyGUI", method = RequestMethod.POST)
    public String uploadTopology(Model model,
            @RequestParam("file") MultipartFile file) {
        /**
         * this method is used to upload an updated topology description file by the user
         */

        model.addAttribute("pagetitle", "VISP Runtime - " + runtimeip);
        try {
            ByteArrayInputStream stream = new ByteArrayInputStream(file.getBytes());
            String fileContent = IOUtils.toString(stream, "UTF-8");
            UpdateResult result = topologyUpdateHandler.handleUpdateFromUser(fileContent);
            model.addAttribute("updateResult", result);
            if (result.dotPath != null && result.getStatus() == UpdateResult.UpdateStatus.SUCCESSFUL) {
                model.addAttribute("dotContent", getTopologyForVizJs(result.dotPath));
            } else {
                model.addAttribute("graphvizAvailable", false);
            }
        }
        catch (Exception e) {
            LOG.error(e.getStackTrace().toString());
            LOG.error(e.toString(), e);
        }

        return "afterTopologyUpdate";
    }



}
