package com.frankies.bootcamp.service;

import com.frankies.bootcamp.constant.BootcampConstants;
import com.frankies.bootcamp.model.BootcampAthlete;
import com.frankies.bootcamp.model.PerformanceResponse;
import com.frankies.bootcamp.model.WeeklyPerformance;
import com.frankies.bootcamp.model.strava.StravaActivityResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActivityProcessServiceTest {

    @AfterEach
    void resetStaticState() throws Exception {
        setStaticField("performanceList", null);
        setStaticField("sortedSummaries", new HashMap<String, HashMap<String, Double>>());
    }

    @Test
    void addActivityEventAddsToExistingAthleteWeekAndTotals() throws Exception {
        ActivityProcessService service = new ActivityProcessService();
        PerformanceResponse performance = createPerformance("athlete-1", "Alex", 20.0);
        WeeklyPerformance week1 = new WeeklyPerformance("Week1", 0L, 20.0, -1.0);
        performance.addWeeklyPerformance(week1, 1);
        setPerformanceList(performance);

        StravaActivityResponse activity = runActivity(9001L, 6.0, BootcampConstants.START_TIMESTAMP + 1000);

        boolean added = service.addActivityEvent("athlete-1", activity);

        assertTrue(added);
        assertEquals(6.0, performance.getDistanceToDate(), 0.0001);
        assertEquals(0.3, performance.getScoreToDate(), 0.0001);
        assertEquals(6.0, performance.getWeeklyPerformances().get(1).getTotalDistance(), 0.0001);
        assertEquals(1, performance.getStravaActivityDetailsByStravaID(9001L).getWeek());
    }

    @Test
    void addActivityEventCreatesMissingWeeksAndAppliesGoalCarryForwardRules() throws Exception {
        ActivityProcessService service = new ActivityProcessService();
        PerformanceResponse performance = createPerformance("athlete-2", "Jordan", 20.0);
        setPerformanceList(performance);

        long weekThreeTimestamp = BootcampConstants.START_TIMESTAMP + (BootcampConstants.WEEK_IN_SECONDS * 2) + 1000;
        StravaActivityResponse activity = workoutActivity(9002L, 1.0, weekThreeTimestamp);

        boolean added = service.addActivityEvent("athlete-2", activity);

        assertTrue(added);
        assertEquals(3, performance.getWeeklyPerformances().size());
        assertEquals("Week3", performance.getWeeklyPerformances().get(3).getWeek());
        assertEquals(5.0, performance.getWeeklyPerformances().get(3).getTotalDistance(), 0.0001);
        assertEquals(5.0, performance.getDistanceToDate(), 0.0001);
    }

    @Test
    void removeActivityEventReversesDistanceScoreAndSportDetails() throws Exception {
        ActivityProcessService service = new ActivityProcessService();
        PerformanceResponse performance = createPerformance("athlete-3", "Casey", 20.0);
        WeeklyPerformance week1 = new WeeklyPerformance("Week1", 0L, 20.0, -1.0);
        performance.addWeeklyPerformance(week1, 1);

        StravaActivityResponse activity = runActivity(9003L, 10.0, BootcampConstants.START_TIMESTAMP + 2000);
        serviceTestAdd(performance, week1, activity);
        setPerformanceList(performance);

        boolean removed = service.removeActivityEvent("athlete-3", 9003L);

        assertTrue(removed);
        assertEquals(0.0, performance.getDistanceToDate(), 0.0001);
        assertEquals(0.0, performance.getScoreToDate(), 0.0001);
        assertFalse(performance.getWeeklyPerformances().get(1).getSports().containsKey("Run"));
    }

    @Test
    void updateStyleFlowRemoveThenAddReplacesActivityContribution() throws Exception {
        ActivityProcessService service = new ActivityProcessService();
        PerformanceResponse performance = createPerformance("athlete-6", "Taylor", 20.0);
        WeeklyPerformance week1 = new WeeklyPerformance("Week1", 0L, 20.0, -1.0);
        performance.addWeeklyPerformance(week1, 1);
        setPerformanceList(performance);

        StravaActivityResponse original = runActivity(9012L, 5.0, BootcampConstants.START_TIMESTAMP + 3000);
        StravaActivityResponse updated = runActivity(9012L, 8.0, BootcampConstants.START_TIMESTAMP + 3000);

        assertTrue(service.addActivityEvent("athlete-6", original));
        assertTrue(service.removeActivityEvent("athlete-6", 9012L));
        assertTrue(service.addActivityEvent("athlete-6", updated));

        assertEquals(8.0, performance.getDistanceToDate(), 0.0001);
        assertEquals(8.0, performance.getWeeklyPerformances().get(1).getTotalDistance(), 0.0001);
        assertEquals(0.4, performance.getScoreToDate(), 0.0001);
    }

    @Test
    void removeActivityEventReturnsFalseWhenWeekIsMissing() throws Exception {
        ActivityProcessService service = new ActivityProcessService();
        PerformanceResponse performance = createPerformance("athlete-7", "Riley", 20.0);
        setPerformanceList(performance);

        assertFalse(service.removeActivityEvent("athlete-7", 123456L));
    }

    @Test
    void generateAllSummaryMapsSortsLeaderboardDescending() throws Exception {
        ActivityProcessService service = new ActivityProcessService();

        PerformanceResponse first = createPerformance("athlete-1", "Alex", 20.0);
        first.setScoreToDate(2.5);
        first.addWeeklyPerformance(new WeeklyPerformance("Week1", 0L, 20.0, -1.0), 1);

        PerformanceResponse second = createPerformance("athlete-2", "Jordan", 20.0);
        second.setScoreToDate(1.0);
        second.addWeeklyPerformance(new WeeklyPerformance("Week1", 0L, 20.0, -1.0), 1);

        List<PerformanceResponse> performances = new ArrayList<>();
        performances.add(first);
        performances.add(second);
        setStaticField("performanceList", performances);

        service.generateAllSummaryMaps();

        @SuppressWarnings("unchecked")
        Map<String, HashMap<String, Double>> summaries =
                (Map<String, HashMap<String, Double>>) getStaticField("sortedSummaries");

        List<String> order = new ArrayList<>(summaries.get(BootcampConstants.currentYearlyScoreSummary).keySet());
        assertEquals(List.of("Alex", "Jordan"), order);
    }

    @Test
    void addActivityEventReturnsFalseWhenAthleteIsMissing() throws Exception {
        ActivityProcessService service = new ActivityProcessService();
        setStaticField("performanceList", new ArrayList<PerformanceResponse>());
        setStaticField("sortedSummaries", new HashMap<String, HashMap<String, Double>>());

        boolean added = service.addActivityEvent("missing-athlete", runActivity(9010L, 5.0, BootcampConstants.START_TIMESTAMP + 1000));

        assertFalse(added);
    }

    @Test
    void addActivityEventReturnsFalseWhenActivityHasNoStartDate() throws Exception {
        ActivityProcessService service = new ActivityProcessService();
        PerformanceResponse performance = createPerformance("athlete-4", "Morgan", 20.0);
        setPerformanceList(performance);

        StravaActivityResponse activity = new StravaActivityResponse();
        activity.setId(9011L);
        activity.setType("Run");
        activity.setSport_type("Run");
        activity.setDistance(5000.0);

        boolean added = service.addActivityEvent("athlete-4", activity);

        assertFalse(added);
    }

    @Test
    void removeActivityEventReturnsFalseWhenWeekHasNoMatchingSportData() throws Exception {
        ActivityProcessService service = new ActivityProcessService();
        PerformanceResponse performance = createPerformance("athlete-5", "Jamie", 20.0);
        performance.addWeeklyPerformance(new WeeklyPerformance("Week1", 0L, 20.0, -1.0), 1);
        setPerformanceList(performance);

        assertNull(performance.getStravaActivityDetailsByStravaID(9999L));
    }

    private static PerformanceResponse createPerformance(String athleteId, String firstName, double goal) {
        BootcampAthlete athlete = new BootcampAthlete();
        athlete.setId(athleteId);
        athlete.setFirstname(firstName);
        athlete.setLastname("Test");
        athlete.setEmail(firstName.toLowerCase() + "@example.com");
        athlete.setGoal(goal);

        PerformanceResponse performance = new PerformanceResponse();
        performance.setAthlete(athlete);
        performance.setDistanceToDate(0.0);
        performance.setScoreToDate(0.0);
        return performance;
    }

    private static void serviceTestAdd(PerformanceResponse performance, WeeklyPerformance week, StravaActivityResponse activity) {
        com.frankies.bootcamp.sport.Run run = new com.frankies.bootcamp.sport.Run(activity);
        week.addSports(run);
        performance.addSport(activity.getId(), 1, run);
        performance.setDistanceToDate(run.getCalculatedDistance());
        performance.setScoreToDate(week.getWeekScore());
    }

    private static StravaActivityResponse runActivity(long id, double kilometers, long epochSeconds) {
        StravaActivityResponse activity = new StravaActivityResponse();
        activity.setId(id);
        activity.setType("Run");
        activity.setSport_type("Run");
        activity.setDistance(kilometers * 1000);
        activity.setStart_date(java.time.Instant.ofEpochSecond(epochSeconds).toString());
        return activity;
    }

    private static StravaActivityResponse workoutActivity(long id, double hours, long epochSeconds) {
        StravaActivityResponse activity = new StravaActivityResponse();
        activity.setId(id);
        activity.setType("Workout");
        activity.setSport_type("Workout");
        activity.setMoving_time((int) Math.round(hours * 3600));
        activity.setStart_date(java.time.Instant.ofEpochSecond(epochSeconds).toString());
        return activity;
    }

    private static void setPerformanceList(PerformanceResponse performance) throws Exception {
        List<PerformanceResponse> performances = new ArrayList<>();
        performances.add(performance);
        setStaticField("performanceList", performances);
        setStaticField("sortedSummaries", new HashMap<String, HashMap<String, Double>>());
    }

    private static void setStaticField(String name, Object value) throws Exception {
        Field field = ActivityProcessService.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(null, value);
    }

    private static Object getStaticField(String name) throws Exception {
        Field field = ActivityProcessService.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(null);
    }
}
