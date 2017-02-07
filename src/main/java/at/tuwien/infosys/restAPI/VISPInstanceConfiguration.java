package at.tuwien.infosys.restAPI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RequestMapping("/vispconfiguration")
@RestController
public class VISPInstanceConfiguration {


    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    @RequestMapping(value = {"/registerInstance/{instanceURI}"}, method = RequestMethod.PUT)
    public void registerInstance(@PathVariable String instanceURI) {

        //TODO implement

        return ;

    }

    @RequestMapping(value = {"/unregisterInstance/{instanceURI}"}, method = RequestMethod.DELETE)
    public void unregisterInstance(@PathVariable String instanceURI) {

        //TODO implement

        return ;

    }

    @RequestMapping(value = {"/registerResourcePool/{instanceURI}/{resourcePool}"}, method = RequestMethod.PUT)
    public void registerResourcePool(@PathVariable String instanceURI, @PathVariable String resourcePool) {

        //TODO implement

        return ;

    }

    @RequestMapping(value = {"/unregisterResourcePool/{instanceURI}/{resourcePool}"}, method = RequestMethod.DELETE)
    public void unregisterResourcePool(@PathVariable String instanceURI, @PathVariable String resourcePool) {

        //TODO implement

        return ;

    }


    @RequestMapping(value = {"/listInstances"}, method = RequestMethod.GET)
    public List<String> listInstances() {

        //TODO implement

        return null;

    }


    @RequestMapping(value = {"/listResourcePool", "/listResourcePool/{instanceId}"}, method = RequestMethod.GET)
    public List<String> instantiatedServices(@RequestHeader HttpHeaders headers, @PathVariable Optional<String> instanceId) {
        if (instanceId.isPresent()) {


        }

        //TODO implement

        return null;

    }



}
