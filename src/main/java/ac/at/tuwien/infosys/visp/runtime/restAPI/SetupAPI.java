package ac.at.tuwien.infosys.visp.runtime.restAPI;

import ac.at.tuwien.infosys.visp.runtime.utility.Utilities;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/control")
@RestController
public class SetupAPI {

    @Autowired
    private Utilities utilities;

    @RequestMapping(value = {"/setup"}, method = RequestMethod.GET)
    public void setup() {
        utilities.clearAll();
    }

}


