package at.tuwien.infosys.quartz.jobs;


import at.tuwien.infosys.reasoner.Reasoner;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;

public class ManageChildProcessesJob implements Job {

        @Autowired
        private Reasoner service;

        @Override
        public void execute(JobExecutionContext jobExecutionContext) {
            service.updateResourceconfiguration();
        }
}
