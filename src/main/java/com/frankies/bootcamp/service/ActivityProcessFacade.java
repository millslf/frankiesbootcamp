package com.frankies.bootcamp.service;

import com.frankies.bootcamp.model.BootcampAthlete;
import com.frankies.bootcamp.model.PerformanceResponse;
import com.frankies.bootcamp.model.WeeklyPerformance;
import com.frankies.bootcamp.model.strava.StravaActivityResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.wildfly.security.credential.store.CredentialStoreException;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class ActivityProcessFacade {

    private static final String ACTIVITY_MODE_KEY = "BOOTCAMP_ACTIVITY_MODE";

    @Inject
    private ActivityProcessService inMemoryActivityProcessService;

    @Inject
    private PersistentActivityProcessService persistentActivityProcessService;

    public ActivityProcessingMode getMode() {
        String configuredMode = System.getProperty(ACTIVITY_MODE_KEY);
        if (configuredMode == null || configuredMode.isBlank()) {
            configuredMode = System.getenv(ACTIVITY_MODE_KEY);
        }
        return ActivityProcessingMode.fromProperty(configuredMode);
    }

    public void prepareSummary() throws SQLException, CredentialStoreException, NoSuchAlgorithmException, IOException {
        if (getMode() == ActivityProcessingMode.PERSISTENT) {
            persistentActivityProcessService.prepareSummary();
            return;
        }
        inMemoryActivityProcessService.prepareSummary();
    }

    public void prepareAthleteSummary(BootcampAthlete athlete) throws SQLException, CredentialStoreException, NoSuchAlgorithmException, IOException {
        if (getMode() == ActivityProcessingMode.PERSISTENT) {
            persistentActivityProcessService.prepareAthleteSummary(athlete);
            return;
        }
        inMemoryActivityProcessService.prepareAthleteSummary(athlete);
    }

    public List<PerformanceResponse> getPerformanceList() {
        return getMode() == ActivityProcessingMode.PERSISTENT
                ? persistentActivityProcessService.getPerformanceList()
                : inMemoryActivityProcessService.getPerformanceList();
    }

    public Map<Integer, WeeklyPerformance> getAthleteHistory(String athleteId) {
        return getMode() == ActivityProcessingMode.PERSISTENT
                ? persistentActivityProcessService.getAthleteHistory(athleteId)
                : inMemoryActivityProcessService.getAthleteHistory(athleteId);
    }

    public HashMap<Integer, HashMap<String, Double>> getHonourRollTotalDistance() {
        return getMode() == ActivityProcessingMode.PERSISTENT
                ? persistentActivityProcessService.getHonourRollTotalDistance()
                : inMemoryActivityProcessService.getHonourRollTotalDistance();
    }

    public HashMap<Integer, HashMap<String, Double>> getHonourRollPercentageOfGoal() {
        return getMode() == ActivityProcessingMode.PERSISTENT
                ? persistentActivityProcessService.getHonourRollPercentageOfGoal()
                : inMemoryActivityProcessService.getHonourRollPercentageOfGoal();
    }

    public String getLoggedInAthleteSummary(String athleteId)
            throws IOException, CredentialStoreException, NoSuchAlgorithmException, SQLException {
        return getMode() == ActivityProcessingMode.PERSISTENT
                ? persistentActivityProcessService.getLoggedInAthleteSummary(athleteId)
                : inMemoryActivityProcessService.getLoggedInAthleteSummary(athleteId);
    }

    public int getNumberOfWeeksSinceStart() {
        return getMode() == ActivityProcessingMode.PERSISTENT
                ? persistentActivityProcessService.getNumberOfWeeksSinceStart()
                : inMemoryActivityProcessService.getNumberOfWeeksSinceStart();
    }

    public Map<String, HashMap<String, Double>> getSortedSummaries() {
        return getMode() == ActivityProcessingMode.PERSISTENT
                ? persistentActivityProcessService.getSortedSummaries()
                : inMemoryActivityProcessService.getSortedSummaries();
    }

    public String getZenBotStatsContext(String athleteId) {
        return getMode() == ActivityProcessingMode.PERSISTENT
                ? persistentActivityProcessService.getZenBotStatsContext(athleteId)
                : inMemoryActivityProcessService.getZenBotStatsContext(athleteId);
    }

    public boolean addActivityEvent(String athleteId, StravaActivityResponse activity) {
        return getMode() == ActivityProcessingMode.PERSISTENT
                ? persistentActivityProcessService.addActivityEvent(athleteId, activity)
                : inMemoryActivityProcessService.addActivityEvent(athleteId, activity);
    }

    public boolean removeActivityEvent(String athleteId, Long activityId) {
        return getMode() == ActivityProcessingMode.PERSISTENT
                ? persistentActivityProcessService.removeActivityEvent(athleteId, activityId)
                : inMemoryActivityProcessService.removeActivityEvent(athleteId, activityId);
    }

    public void run() {
        if (getMode() == ActivityProcessingMode.PERSISTENT) {
            persistentActivityProcessService.run();
            return;
        }
        inMemoryActivityProcessService.run();
    }
}
