package ac.at.tuwien.infosys.visp.runtime.ui;


import ac.at.tuwien.infosys.visp.runtime.configuration.Configurationprovider;
import ac.at.tuwien.infosys.visp.runtime.datasources.VISPInstanceRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.VISPInstance;
import ac.at.tuwien.infosys.visp.runtime.topology.TopologyManagement;
import ac.at.tuwien.infosys.visp.runtime.topology.TopologyUpdateHandler;
import ac.at.tuwien.infosys.visp.runtime.topology.rabbitMq.UpdateResult;
import ac.at.tuwien.infosys.visp.runtime.utility.Utilities;
import org.apache.commons.io.IOUtils;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@Controller
@DependsOn("configurationprovider")
public class TopologyController {

    @Autowired
    private TopologyUpdateHandler topologyUpdateHandler;

    @Autowired
    private TopologyManagement topologyManagement;

    @Autowired
    private Utilities utilities;

    @Autowired
    private VISPInstanceRepository vir;

    @Value("${visp.odr.host}")
    private String odrHost;

    @Value("${visp.odr.port}")
    private String odrPort;

    @Value("{visp.rt.callback.host")
    private String callbackHost;

    @Value("{visp.rt.callback.port")
    private String callbackPort;

    private Integer currentOptimizationTaskId;

    @Autowired
    private Configurationprovider config;

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    @RequestMapping("/topology")
    public String index(Model model) throws SchedulerException {

        if(topologyManagement.getTopology().size() == 0) {
            model.addAttribute("emptyTopology", true);
        } else {
            model.addAttribute("emptyTopology", false);
            try {
                model.addAttribute("dotContent", getTopologyForVizJs(topologyManagement.getDotFile()));
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

    @RequestMapping(value = "/topology/uploadTopologyGUIOnly", method = RequestMethod.POST)
    public String uploadTopologyGuiOnly(Model model,
                                 @RequestParam("file") MultipartFile file) {
        deleteCurrentOptimizationTask(); //delete if there is an existing optimization task;
        return uploadTopology(model, file);
    }
    @RequestMapping(value = "/topology/uploadTopologyGUI", method = RequestMethod.POST)
    public String uploadTopology(Model model,
            @RequestParam("file") MultipartFile file) {
        /**
         * this method is used to upload an updated topology description file by the user
         */

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
            LOG.error("Could not apply update from user", e);
        }

        return "afterTopologyUpdate";
    }

    @RequestMapping("/topology/optimizePlacements")
    public String startODROptimization() {
        deleteCurrentOptimizationTask(); //delete if there is an existing optimization task;
        RestTemplate restTemplate = new RestTemplate();
        String url = "http://" + odrHost + ":" + odrPort + "/odrApi/addAndStartOptTask";
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("vispRTHost", callbackHost)
                .queryParam("port", callbackPort);
        try {
            ResponseEntity<Integer> response = restTemplate
                    .exchange(builder.build().encode().toUri(), HttpMethod.POST, null, Integer.class);
            this.currentOptimizationTaskId = response.getBody();
            LOG.info("Optimization task with id " + currentOptimizationTaskId + " created.");
        } catch(RestClientException e){
            LOG.error("ODR Reasoner cannot be requested. Url: " + url);
            return "ODR Reasoner invocation failed.";
        }

        return "ODR Reasoner started optimization task for topology with id:" + currentOptimizationTaskId;
    }

    /**
     * deletes the current optimization task from ODR optimization scheduling (if present)
     */
    public void deleteCurrentOptimizationTask() {
        if (currentOptimizationTaskId == null) {
            return;
        }
        RestTemplate restTemplate = new RestTemplate();
        String url = "http://" + odrHost + ":" + odrPort + "/odrApi/deleteOptTask";
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("taskId", currentOptimizationTaskId);
        try {
            restTemplate
                    .exchange(builder.build().encode().toUri(), HttpMethod.DELETE, null, Void.class);
            currentOptimizationTaskId = null;
            LOG.info("Optimization Task with id: "+ currentOptimizationTaskId + " deleted." );
        } catch(RestClientException e){
            LOG.error("ODR Reasoner cannot be requested. Url: " + url);
        }
    }

    @RequestMapping("/topology/clear")
    public String reinitialize(Model model) throws SchedulerException {

        //delete if there is an existing optimization task;
        deleteCurrentOptimizationTask();

        // clear own topology:
        utilities.clearAll();

        List<VISPInstance> allVispInstances = (List<VISPInstance>) vir.findAll();

        LOG.debug("This instance currently knows of " + allVispInstances.size() + " other VISP instances...");

        int clearFails = 0;

        for(VISPInstance instance : allVispInstances) {
            if(instance.getUri().equals(config.getRuntimeIP())) {
                continue;
            }
            LOG.debug("sending clear request to " + instance.getUri() + "...");
            RestTemplate restTemplate = new RestTemplate();
            try {
                String url = "http://" + instance.getUri() + ":8080/clear";
                Map clearResult = restTemplate.getForObject(url, Map.class);
                String errorMessage = (String) clearResult.get("errorMessage");
                if(errorMessage.equals("none")) {
                    LOG.debug("VISP Instance " + instance.getUri() + " replied clear success");
                }
            } catch (Exception e) {
                LOG.error("VISP Instance " + instance.getUri() + " could not perform clear", e);
                clearFails++;
            }
        }

        model.addAttribute("action", "clear");
        model.addAttribute("clearfails", clearFails);
        return "afterTopologyUpdate";
    }


}
