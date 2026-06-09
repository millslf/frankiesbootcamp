package com.frankies.bootcamp.job;

import com.frankies.bootcamp.service.ActivityProcessFacade;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.Asynchronous;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@Singleton
@Startup
public class ActivityProcessJob {

    private static final Logger LOG = Logger.getLogger(ActivityProcessJob.class);

    @Inject
    ActivityProcessFacade activityProcessFacade;

    @PostConstruct
    public void onStartup() {
        // kick off immediately without blocking startup
        runNowAsync();
    }

    // Every day at 00:01 local server time
    @Schedule(hour = "0", minute = "1", second = "0",
            timezone = "Australia/Sydney", persistent = true)
    public void runDaily() {
        runSafely();
    }

    @Asynchronous
    public void runNowAsync() {
        runSafely();
    }

    private void runSafely() {
        try {
            activityProcessFacade.run();
        } catch (RuntimeException e) {
            LOG.error("ActivityProcessJob failed", e);
        }
    }
}
