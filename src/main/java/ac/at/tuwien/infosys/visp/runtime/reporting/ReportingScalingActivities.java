package ac.at.tuwien.infosys.visp.runtime.reporting;

import ac.at.tuwien.infosys.visp.runtime.datasources.ScalingActivityRepository;
import ac.at.tuwien.infosys.visp.runtime.datasources.entities.ScalingActivity;
import ac.at.tuwien.infosys.visp.runtime.entities.GraphData;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReportingScalingActivities {


    @Autowired
    private ScalingActivityRepository sar;

    private static final Logger LOG = LoggerFactory.getLogger(ReportingScalingActivities.class);

    public void generateCSV() {

        List<GraphData> data = new ArrayList<>();

        ScalingActivity firstSa = sar.findFirstByOrderByTimeAsc();

        DateTime firstTime = firstSa.getTime();
        DateTime nextTime = firstTime.plusMinutes(1);

        List<ScalingActivity> activities = sar.findByTimeBetween(firstTime, nextTime);

        Integer inactivityCounter = 0;
        Integer vmCounter = 0;

        Integer vmCost = 1;
        Integer totalVMCost = 0;

        //assume that after 15 minutes lack of data there is no more data
        while (inactivityCounter < 16) {
            GraphData gd = new GraphData(firstTime.toString());

            for (ScalingActivity sa : activities) {
                switch (sa.getScalingActivity()) {
                    case "startVM":
                        gd.vmUpInc();
                        vmCounter++;
                        totalVMCost += vmCost;
                        break;
                    case "stopVM":
                        gd.vmDownInc();
                        vmCounter--;
                        break;
                    case "prolongLease":
                        gd.vmProlongLease();
                        totalVMCost += vmCost;
                        break;
                    case "scaleup":
                        gd.operatorUpInc();
                        break;
                    case "scaledown":
                        gd.operatorDownInc();
                        break;
                    case "migrate":
                        gd.operatorMigrateInc();
                        break;
                    default: //do nothing
                }
            }

            gd.setTotalVMs(vmCounter);

            data.add(gd);

            firstTime = nextTime;
            nextTime = firstTime.plusMinutes(1);
            activities = sar.findByTimeBetween(firstTime, nextTime);
            if (activities.isEmpty()) {
                inactivityCounter++;
            } else {
                inactivityCounter = 0;
            }
        }

        Path overalmetricsPath = Paths.get("reporting/overallMetrics.csv");
        try {
            if (Files.exists(overalmetricsPath)) {
                Files.delete(overalmetricsPath);
            }
            Files.createFile(overalmetricsPath);
            Files.write(overalmetricsPath, ("TotalCost: " + totalVMCost).getBytes());
        } catch (IOException e) {
            LOG.error(e.getMessage());
        }

        CsvMapper mapper = new CsvMapper();
        CsvSchema schema = mapper.schemaFor(GraphData.class).withHeader();
        try {
            Path path = Paths.get("reporting/scalingactivities.csv");
            if (Files.exists(path)) {
                Files.delete(path);
            }
            Files.createFile(path);
            Files.write(path, mapper.writer(schema).writeValueAsString(data).getBytes());
        } catch (IOException e) {
            LOG.error(e.getMessage());
        }
    }

    //TODO collect container monitoring information
}
