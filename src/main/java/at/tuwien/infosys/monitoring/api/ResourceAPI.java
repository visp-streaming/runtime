package at.tuwien.infosys.monitoring.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/resources")
@RestController
public class ResourceAPI {

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    @RequestMapping(value = {"/getLoad"}, method = RequestMethod.GET)
    public void getResourceLoadForOperatorType(@PathVariable String operator) {

        //TODO aggregate the information for a specific operator type

       //TODO implement me

        return;
    }

}
