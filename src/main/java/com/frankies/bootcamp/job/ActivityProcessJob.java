package com.frankies.bootcamp.job;

import com.frankies.bootcamp.service.ActivityProcessService;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.Asynchronous;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;

@Singleton
@Startup
public class ActivityProcessJob {

    @Inject
    ActivityProcessService activityProcessService;

    @PostConstruct
    public void onStartup() {
        // kick off immediately without blocking startup
        runNowAsync();
    }

    // Every day at 00:01 local server time
    @Schedule(hour = "0", minute = "1", second = "0",
            timezone = "Australia/Sydney", persistent = true)
    public void runDaily() {
        activityProcessService.run();
    }

    @Asynchronous
    public void runNowAsync() {
        activityProcessService.run();
    }
}

