package ac.at.tuwien.infosys.visp.runtime.restAPI;

import ac.at.tuwien.infosys.visp.runtime.utility.Utilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/control")
@RestController
public class SetupAPI {

    @Autowired
    private Utilities utilities;


    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    @RequestMapping(value = {"/setup"}, method = RequestMethod.GET)
    public void setup() {
        utilities.createInitialStatus();
    }

    @RequestMapping(value = {"/export"}, method = RequestMethod.GET)
    public void cleanup() {
        utilities.exportData();
    }

}


