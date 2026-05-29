package com.frankies.bootcamp.service;

import com.frankies.bootcamp.constant.BootcampConstants;
import com.frankies.bootcamp.model.BootcampAthlete;
import com.frankies.bootcamp.model.PerformanceResponse;
import com.frankies.bootcamp.model.WeeklyPerformance;
import com.frankies.bootcamp.model.strava.StravaActivityResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.wildfly.security.credential.store.CredentialStoreException;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class ActivityProcessServiceTest {

    @AfterEach
    void resetStaticState() throws Exception {
        setStaticField("performanceList", null);
        setStaticField("sortedSummaries", new HashMap<String, HashMap<String, Double>>());
        setStaticField("honourRollTotalDistance", new HashMap<Integer, HashMap<String, Double>>());
        setStaticField("honourRollPercentageOfGoal", new HashMap<Integer, HashMap<String, Double>>());
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
    void prepareAthleteSummaryReplacementLogicKeepsSingleEntryPerAthleteId() throws Exception {
        PerformanceResponse existing = createPerformance("athlete-link", "Before", 20.0);
        existing.addWeeklyPerformance(createWeek("Week3", 20.0, 5.0), 3);
        PerformanceResponse other = createPerformance("athlete-other", "Other", 20.0);
        other.addWeeklyPerformance(createWeek("Week3", 20.0, 7.5), 3);
        setPerformanceList(List.of(existing, other));

        PerformanceResponse replacement = createPerformance("athlete-link", "After", 20.0);
        replacement.addWeeklyPerformance(createWeek("Week3", 20.0, 9.0), 3);
        List<PerformanceResponse> updated = new ArrayList<>(getPerformanceList());
        updated.removeIf(existingPerformance -> existingPerformance.getAthlete() != null
                && "athlete-link".equals(existingPerformance.getAthlete().getId()));
        updated.add(replacement);

        assertNotNull(updated);
        assertEquals(2, updated.size());
        assertTrue(updated.stream().anyMatch(p -> p.getAthlete() != null
                && "athlete-link".equals(p.getAthlete().getId())
                && "After".equals(p.getAthlete().getFirstname())));
        assertTrue(updated.stream().anyMatch(p -> p.getAthlete() != null
                && "athlete-other".equals(p.getAthlete().getId())));
        assertFalse(updated.stream().anyMatch(p -> p.getAthlete() != null
                && "athlete-link".equals(p.getAthlete().getId())
                && "Before".equals(p.getAthlete().getFirstname())));
    }

    @Test
    void addActivityEventCreatesOnlyMissingWeeksUpToActivityWeek() throws Exception {
        ActivityProcessService service = new ActivityProcessService();
        PerformanceResponse performance = createPerformance("gap-athlete", "Gap", 25.0);
        performance.addWeeklyPerformance(new WeeklyPerformance("Week1", 0L, 25.0, -1.0), 1);
        setPerformanceList(performance);

        long targetWeek = 5L;
        long activityTime = BootcampConstants.START_TIMESTAMP + ((targetWeek - 1) * BootcampConstants.WEEK_IN_SECONDS) + 120L;
        StravaActivityResponse activity = runActivity(7001L, 8.0, activityTime);

        boolean added = service.addActivityEvent("gap-athlete", activity);

        assertTrue(added);
        assertEquals(5, performance.getWeeklyPerformances().size());
        assertTrue(performance.getWeeklyPerformances().containsKey(1));
        assertTrue(performance.getWeeklyPerformances().containsKey(2));
        assertTrue(performance.getWeeklyPerformances().containsKey(3));
        assertTrue(performance.getWeeklyPerformances().containsKey(4));
        assertTrue(performance.getWeeklyPerformances().containsKey(5));
        assertEquals(8.0, performance.getWeeklyPerformances().get(5).getTotalDistance(), 0.0001);
        assertEquals(0.0, performance.getWeeklyPerformances().get(2).getTotalDistance(), 0.0001);
    }

    @Test
    void buildPerformanceForAthleteMatchesWorkingOldImplementation() throws Exception {
        BootcampAthlete athlete = createAthlete("startup-athlete", "Startup", 25.0);
        athlete.setAccessToken("token");
        athlete.setExpiresAt(Instant.now().getEpochSecond() + 3600);

        long activityTime = BootcampConstants.START_TIMESTAMP + (3L * BootcampConstants.WEEK_IN_SECONDS) + 120L;
        List<StravaActivityResponse> activities = List.of(runActivity(8801L, 8.0, activityTime));

        TestActivityProcessService newService = new TestActivityProcessService(activities);
        TestActivityProcessOld oldService = new TestActivityProcessOld(activities);

        PerformanceResponse newPerformance = runWithTimeout(() -> newService.buildPerformanceForAthlete(athlete),
                "ActivityProcessService.buildPerformanceForAthlete should finish quickly for a normal historic gap");
        PerformanceResponse oldPerformance = runWithTimeout(() -> oldService.buildPerformanceForAthleteUsingOldLogic(athlete),
                "ActivityProcessOld.buildPerformanceForAthlete should finish quickly for a normal historic gap");

        assertNotNull(newPerformance);
        assertNotNull(oldPerformance);
        assertEquals(oldPerformance.getDistanceToDate(), newPerformance.getDistanceToDate(), 0.0001);
        assertEquals(oldPerformance.getScoreToDate(), newPerformance.getScoreToDate(), 0.0001);
        assertEquals(oldPerformance.getWeeklyPerformances().size(), newPerformance.getWeeklyPerformances().size());
        assertEquals(
                oldPerformance.getWeeklyPerformances().keySet().stream().mapToInt(Integer::intValue).max().orElseThrow(),
                newPerformance.getWeeklyPerformances().keySet().stream().mapToInt(Integer::intValue).max().orElseThrow()
        );

        for (Map.Entry<Integer, WeeklyPerformance> entry : oldPerformance.getWeeklyPerformances().entrySet()) {
            WeeklyPerformance actualWeek = newPerformance.getWeeklyPerformances().get(entry.getKey());
            assertNotNull(actualWeek, "Missing week " + entry.getKey());
            assertEquals(entry.getValue().getTotalDistance(), actualWeek.getTotalDistance(), 0.0001);
            assertEquals(entry.getValue().getWeekGoal(), actualWeek.getWeekGoal(), 0.0001);
            assertEquals(entry.getValue().getWeekScore(), actualWeek.getWeekScore(), 0.0001);
        }
    }

    private static BootcampAthlete createAthlete(String athleteId, String firstName, double goal) {
        BootcampAthlete athlete = new BootcampAthlete();
        athlete.setId(athleteId);
        athlete.setFirstname(firstName);
        athlete.setLastname("Test");
        athlete.setEmail(firstName.toLowerCase() + "@example.com");
        athlete.setGoal(goal);
        return athlete;
    }

    private static PerformanceResponse createPerformance(String athleteId, String firstName, double goal) {
        BootcampAthlete athlete = createAthlete(athleteId, firstName, goal);

        PerformanceResponse performance = new PerformanceResponse();
        performance.setAthlete(athlete);
        performance.setDistanceToDate(0.0);
        performance.setScoreToDate(0.0);
        return performance;
    }

    private static WeeklyPerformance createWeek(String label, double goal, double totalDistance) throws Exception {
        WeeklyPerformance week = new WeeklyPerformance(label, 0L, goal, -1.0);
        setObjectField(week, "totalDistance", totalDistance);
        setObjectField(week, "totalPercentOfGoal", totalDistance / goal);
        setObjectField(week, "weekScore", Math.min(totalDistance / goal, 1.0));
        setObjectField(week, "averageWeeklyScore", Math.min(totalDistance / goal, 1.0));
        return week;
    }

    private static StravaActivityResponse runActivity(long id, double kilometers, long epochSeconds) {
        StravaActivityResponse activity = new StravaActivityResponse();
        activity.setId(id);
        activity.setType("Run");
        activity.setSport_type("Run");
        activity.setDistance(kilometers * 1000);
        activity.setStart_date(Instant.ofEpochSecond(epochSeconds).toString());
        return activity;
    }

    private static void setPerformanceList(PerformanceResponse performance) throws Exception {
        setPerformanceList(List.of(performance));
    }

    private static void setPerformanceList(List<PerformanceResponse> performances) throws Exception {
        setStaticField("performanceList", new ArrayList<>(performances));
        setStaticField("sortedSummaries", new HashMap<String, HashMap<String, Double>>());
        setStaticField("honourRollTotalDistance", new HashMap<Integer, HashMap<String, Double>>());
        setStaticField("honourRollPercentageOfGoal", new HashMap<Integer, HashMap<String, Double>>());
    }

    private static void setStaticField(String name, Object value) throws Exception {
        Field field = ActivityProcessService.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(null, value);
    }

    @SuppressWarnings("unchecked")
    private static List<PerformanceResponse> getPerformanceList() throws Exception {
        Field field = ActivityProcessService.class.getDeclaredField("performanceList");
        field.setAccessible(true);
        return (List<PerformanceResponse>) field.get(null);
    }

    private static void setObjectField(Object target, String name, Object value) throws Exception {
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    private static PerformanceResponse runWithTimeout(ThrowingSupplier<PerformanceResponse> supplier, String timeoutMessage) throws Exception {
        Future<PerformanceResponse> future = Executors.newSingleThreadExecutor().submit(() -> {
            try {
                return supplier.get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        try {
            return future.get(2, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            fail(timeoutMessage);
            return null;
        } catch (ExecutionException e) {
            fail("Execution threw unexpectedly: " + e.getCause());
            return null;
        }
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }

    private static final class TestActivityProcessService extends ActivityProcessService {
        private TestActivityProcessService(List<StravaActivityResponse> activities) {
            try {
                setObjectField(this, "strava", new FakeStravaService(activities));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static final class TestActivityProcessOld extends ActivityProcessOld {
        private TestActivityProcessOld(List<StravaActivityResponse> activities) {
            try {
                setObjectField(this, "strava", new FakeStravaService(activities));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private PerformanceResponse buildPerformanceForAthleteUsingOldLogic(BootcampAthlete athlete) throws Exception {
            BootcampAthlete refreshedAthlete = ((FakeStravaService) getObjectField(this, "strava")).refreshToken(athlete);
            PerformanceResponse performance = new PerformanceResponse();
            performance.setAthlete(refreshedAthlete);

            List<StravaActivityResponse> stravaActivities = ((FakeStravaService) getObjectField(this, "strava"))
                    .getAthleteActivitiesForPeriod(BootcampConstants.START_TIMESTAMP, refreshedAthlete.getAccessToken());
            double distance = 0;
            double score = 0;
            int week = 1;
            long weekEnding = BootcampConstants.START_TIMESTAMP + BootcampConstants.WEEK_IN_SECONDS;
            WeeklyPerformance weeklyPerformance = new WeeklyPerformance("Week" + week, weekEnding, refreshedAthlete.getGoal(), -1.0);

            for (StravaActivityResponse activity : stravaActivities) {
                int loopCount = 0;
                while (Instant.parse(activity.getStart_date()).getEpochSecond() > weekEnding) {
                    weeklyPerformance.setAverageWeeklyScore(score, week - 1);
                    weeklyPerformance.setIsSick(refreshedAthlete.isSick(week));
                    score += weeklyPerformance.getWeekScore();
                    performance.addWeeklyPerformance(weeklyPerformance, week);
                    week++;
                    weekEnding = weekEnding + BootcampConstants.WEEK_IN_SECONDS;
                    weeklyPerformance = new WeeklyPerformance("Week" + week, weekEnding, weeklyPerformance.getWeekGoal(),
                            loopCount == 0 ? weeklyPerformance.getTotalDistance() : 0.0);
                    loopCount++;
                }
                com.frankies.bootcamp.sport.BaseSport sport = com.frankies.bootcamp.sport.SportFactory.getSport(activity);
                if (sport != null) {
                    performance.addSport(activity.getId(), week, sport);
                    weeklyPerformance.addSports(sport);
                    distance += sport.getCalculatedDistance();
                }
            }

            int loopCount = 0;
            if (performance.getWeeklyPerformances() == null) {
                performance.addWeeklyPerformance(weeklyPerformance, week);
            }
            while (performance.getWeeklyPerformances().size() < getNumberOfWeeksSinceStart()) {
                weeklyPerformance.setAverageWeeklyScore(score, week - 1);
                weeklyPerformance.setIsSick(refreshedAthlete.isSick(week));
                score += weeklyPerformance.getWeekScore();
                performance.addWeeklyPerformance(weeklyPerformance, week);
                week++;
                weekEnding = weekEnding + BootcampConstants.WEEK_IN_SECONDS;
                weeklyPerformance = new WeeklyPerformance("Week" + week, weekEnding, weeklyPerformance.getWeekGoal(),
                        loopCount == 0 ? weeklyPerformance.getTotalDistance() : 0.0);
                loopCount++;
            }

            performance.setDistanceToDate(distance);
            performance.setScoreToDate(score);
            return performance;
        }
    }

    private static Object getObjectField(Object target, String name) throws Exception {
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(target);
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    private static final class FakeStravaService extends StravaService {
        private final List<StravaActivityResponse> activities;

        private FakeStravaService(List<StravaActivityResponse> activities) {
            this.activities = activities;
        }

        @Override
        public BootcampAthlete refreshToken(BootcampAthlete athlete) {
            return athlete;
        }

        @Override
        public List<StravaActivityResponse> getAthleteActivitiesForPeriod(long after, String accessToken) {
            return activities;
        }
    }

}
