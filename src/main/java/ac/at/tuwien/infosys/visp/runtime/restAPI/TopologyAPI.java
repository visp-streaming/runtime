package ac.at.tuwien.infosys.visp.runtime.restAPI;


import ac.at.tuwien.infosys.visp.runtime.topology.TopologyManagement;
import ac.at.tuwien.infosys.visp.runtime.topology.TopologyUpdateHandler;
import ac.at.tuwien.infosys.visp.runtime.topology.rabbitMq.RabbitMqManager;
import ac.at.tuwien.infosys.visp.topologyParser.TopologyParser;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;

import static org.springframework.http.HttpStatus.OK;

@RestController
public class TopologyAPI {

    private static final Logger LOG = LoggerFactory.getLogger(TopologyAPI.class);

    @Autowired
    TopologyUpdateHandler topologyUpdateHandler;

    @Autowired
    RabbitMqManager rabbitMqManager;

    @Autowired
    private TopologyParser parser;

    @Autowired
    private TopologyManagement topologyManagement;

    @RequestMapping("/addMessageFlow")
    public String addMessageFlow() {
        /**
         * this method changes some aspect about the rabbitmq configuration at runtime
         * it is used to test whether the newly built adaption features actually work
         */

        try {
            rabbitMqManager.addMessageFlow("step1", "step4", "128.130.172.226", "128.130.172.226");
            return "Done!";

        } catch (Exception e) {
            return e.getLocalizedMessage();
        }

    }

    @RequestMapping("/removeMessageFlow")
    public String removeMessageFlow() {
        try {
            rabbitMqManager.removeMessageFlow("step1", "step4", "128.130.172.226", "128.130.172.226");
            return "Done!";

        } catch (Exception e) {
            return e.getLocalizedMessage();
        }
    }

//    @RequestMapping("/uploadTopologyFile")
//    public String index(@RequestParam(value="name", defaultValue="World") String name) {
//        /**
//         * this controller is just for demonstrating that the file upload also works manually
//         * in practice, the file will be sent by another VISP instance and not by the user
//         */
//        return "<html>\n" +
//                "<body>\n" +
//                "  <form action=\"/testDeploymentForTopologyFile\" method=\"POST\" enctype=\"multipart/form-data\">\n" +
//                "    <input type=\"file\" name=\"file\">\n" +
//                "    <input type=\"submit\" value=\"Upload\"> \n" +
//                "  </form>\n" +
//                "</body>\n" +
//                "</html>";
//    }

    @RequestMapping("/getTopology")
    public String getTopology() throws IOException {
        /**
         * returns the currently active topology as a VISP topology description language file
         */
        String topologyFile = parser.generateTopologyFile(topologyManagement.getTopology());
        byte[] encoded = Files.readAllBytes(Paths.get(topologyFile));
        return new String(encoded, Charset.defaultCharset());
    }

    @RequestMapping(value = "/uploadTopology", method = RequestMethod.POST, produces = "application/json")
    @ResponseBody
    public Map<String, String> testDeploymentForTopologyFile(
            @RequestParam("file") MultipartFile file) {
        /**
         * this method is called from another VISP runtime instance
         * the current instance checks whether it can perform the suggested
         * updates or not
         */

        try {
            ByteArrayInputStream stream = new ByteArrayInputStream(file.getBytes());
            String fileContent = IOUtils.toString(stream, "UTF-8");
            TopologyUpdateHandler.UpdateResult result = topologyUpdateHandler.handleUpdateFromUser(fileContent);
            // TODO: big refactoring needed: topologyParser, topologyManagement and topologyUpdateHandler have similar functionalities; divide into stateful and stateless class
        }
        catch (Exception e) {
            return Collections.singletonMap("errorMessage", e.getLocalizedMessage());
        }

        return Collections.singletonMap("errorMessage", "none");
    }


}

