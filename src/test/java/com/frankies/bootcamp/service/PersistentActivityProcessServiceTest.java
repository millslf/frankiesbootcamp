package com.frankies.bootcamp.service;

import com.frankies.bootcamp.constant.BootcampConstants;
import com.frankies.bootcamp.model.BootcampAthlete;
import com.frankies.bootcamp.model.PerformanceResponse;
import com.frankies.bootcamp.model.WeeklyPerformance;
import com.frankies.bootcamp.model.strava.StravaActivityResponse;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.wildfly.security.credential.store.CredentialStoreException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PersistentActivityProcessServiceTest {

    @Test
    void prepareAthleteSummaryBuildsPersistentRowsWithMatchingTotals() throws Exception {
        BootcampAthlete athlete = createAthlete("athlete-1", "Alex", "Runner", 20.0);
        List<StravaActivityResponse> activities = List.of(
                runActivity(101L, 6.0, BootcampConstants.START_TIMESTAMP + 1000L),
                runActivity(102L, 4.0, BootcampConstants.START_TIMESTAMP + BootcampConstants.WEEK_IN_SECONDS + 1000L)
        );

        FakeDBService db = new FakeDBService(List.of(athlete));
        PersistentActivityProcessService service = createService(db, activities, 2);

        service.prepareAthleteSummary(athlete);

        assertEquals(1, service.getPerformanceList().size());
        PerformanceResponse performance = service.getPerformanceList().get(0);
        assertEquals(10.0, performance.getDistanceToDate(), 0.0001);
        assertEquals(0.5222222222222221, performance.getScoreToDate(), 0.0001);
        assertEquals(2, performance.getWeeklyPerformances().size());
        assertEquals(6.0, performance.getWeeklyPerformances().get(1).getTotalDistance(), 0.0001);
        assertEquals(4.0, performance.getWeeklyPerformances().get(2).getTotalDistance(), 0.0001);

        assertEquals(1, db.replacedCompetitionStates.size());
        FakeDBService.ReplacedCompetitionState replaced = db.replacedCompetitionStates.get(0);
        assertEquals(2, replaced.activityRows().size());
        assertEquals(2, replaced.weeklyRows().size());
        assertEquals(10.0, replaced.summaryRow().distanceToDate(), 0.0001);
        assertEquals(0.5222222222222221, replaced.summaryRow().scoreToDate(), 0.0001);
        assertEquals(2, replaced.summaryRow().currentWeek());
        assertEquals(22.22222222222222, replaced.summaryRow().latestWeekPercentOfGoal(), 0.0001);
        assertTrue(replaced.summarySportRows().containsKey("Run"));
        assertEquals(2, replaced.summarySportRows().get("Run").activityCount());
        assertEquals(10.0, replaced.summarySportRows().get("Run").calculatedDistanceTotal(), 0.0001);
    }

    @Test
    void prepareAthleteSummaryMatchesLegacyCalculationFlowForOverlappingScenario() throws Exception {
        BootcampAthlete athlete = createAthlete("athlete-legacy", "Jamie", "Trail", 25.0);
        athlete.setAccessToken("token");
        athlete.setExpiresAt(Instant.now().getEpochSecond() + 3600);

        List<StravaActivityResponse> activities = List.of(
                runActivity(201L, 8.0, BootcampConstants.START_TIMESTAMP + 1000L),
                runActivity(202L, 5.0, BootcampConstants.START_TIMESTAMP + (2L * BootcampConstants.WEEK_IN_SECONDS) + 120L)
        );

        FakeDBService db = new FakeDBService(List.of(athlete));
        PersistentActivityProcessService persistent = createService(db, activities, 3);
        ActivityProcessServiceTestLegacyHarness legacy = new ActivityProcessServiceTestLegacyHarness(activities, 3);

        persistent.prepareAthleteSummary(athlete);
        PerformanceResponse persistentPerformance = persistent.getPerformanceList().get(0);
        PerformanceResponse legacyPerformance = legacy.buildPerformanceForAthleteUsingOldLogic(athlete);

        assertEquals(legacyPerformance.getDistanceToDate(), persistentPerformance.getDistanceToDate(), 0.0001);
        assertEquals(legacyPerformance.getScoreToDate(), persistentPerformance.getScoreToDate(), 0.0001);
        assertEquals(legacyPerformance.getWeeklyPerformances().size(), persistentPerformance.getWeeklyPerformances().size());

        for (Map.Entry<Integer, WeeklyPerformance> entry : legacyPerformance.getWeeklyPerformances().entrySet()) {
            WeeklyPerformance actualWeek = persistentPerformance.getWeeklyPerformances().get(entry.getKey());
            assertNotNull(actualWeek, "Missing week " + entry.getKey());
            assertEquals(entry.getValue().getTotalDistance(), actualWeek.getTotalDistance(), 0.0001);
            assertEquals(entry.getValue().getWeekGoal(), actualWeek.getWeekGoal(), 0.0001);
            assertEquals(entry.getValue().getWeekScore(), actualWeek.getWeekScore(), 0.0001);
        }
    }

    @Test
    void addActivityEventRebuildsKnownAthleteAndReturnsTrue() throws Exception {
        BootcampAthlete athlete = createAthlete("strava-athlete", "Taylor", "Swift", 18.0);
        List<StravaActivityResponse> activities = List.of(runActivity(301L, 7.0, BootcampConstants.START_TIMESTAMP + 1000L));

        FakeDBService db = new FakeDBService(List.of(athlete));
        db.athletesByStravaId.put("strava-athlete", athlete);
        PersistentActivityProcessService service = createService(db, activities, 1);

        boolean added = service.addActivityEvent("strava-athlete", activities.get(0));

        assertTrue(added);
        assertEquals(1, db.findAthleteByStravaIdCalls);
        assertEquals(1, db.replacedCompetitionStates.size());
        assertEquals(1, service.getPerformanceList().size());
        assertEquals(7.0, service.getPerformanceList().get(0).getDistanceToDate(), 0.0001);
    }

    @Test
    void addActivityEventReturnsFalseWhenAthleteMissing() {
        FakeDBService db = new FakeDBService(List.of());
        PersistentActivityProcessService service = createService(db, List.of(), 1);

        boolean added = service.addActivityEvent("missing-athlete", runActivity(401L, 3.0, BootcampConstants.START_TIMESTAMP + 1000L));

        assertFalse(added);
        assertEquals(1, db.findAthleteByStravaIdCalls);
        assertTrue(service.getPerformanceList().isEmpty());
        assertTrue(db.replacedCompetitionStates.isEmpty());
    }

    @Test
    void honourRollAndLeaderboardReadsDelegateToDb() {
        FakeDBService db = new FakeDBService(List.of());
        HashMap<String, Double> scoreSummary = new LinkedHashMap<>();
        scoreSummary.put("Alex", 1.7);
        HashMap<String, Double> percentSummary = new LinkedHashMap<>();
        percentSummary.put("Alex", 80.0);
        db.leaderboardSummaries.put(BootcampConstants.currentYearlyScoreSummary, scoreSummary);
        db.leaderboardSummaries.put(BootcampConstants.currentWeekPercentageOfGoalSummary, percentSummary);

        HashMap<String, Double> distanceWeek = new LinkedHashMap<>();
        distanceWeek.put("Alex Runner", 12.0);
        db.honourRollDistance.put(1, distanceWeek);

        HashMap<String, Double> percentWeek = new LinkedHashMap<>();
        percentWeek.put("Alex Runner", 0.8);
        db.honourRollPercent.put(1, percentWeek);

        PersistentActivityProcessService service = createService(db, List.of(), 1);

        assertSame(db.leaderboardSummaries, service.getSortedSummaries());
        assertSame(db.honourRollDistance, service.getHonourRollTotalDistance());
        assertSame(db.honourRollPercent, service.getHonourRollPercentageOfGoal());
    }

    @Test
    void athleteHistoryReadsLatestWeeklyRowsFromDb() {
        FakeDBService db = new FakeDBService(List.of());
        WeeklyPerformance week = new WeeklyPerformance("Week4", 0L, 30.0, -1.0);
        week.setPersistedValues(12.0, 0.4, 0.4, 0.0, 0.4, 0.4, false);
        week.setPersistedSportTotals("Run", 2, 12.0, 12.0, null);
        db.historyByAthlete.put("athlete-1", Map.of(4, week));

        PersistentActivityProcessService service = createService(db, List.of(), 4);

        Map<Integer, WeeklyPerformance> history = service.getAthleteHistory("athlete-1");

        assertEquals(30.0, history.get(4).getWeekGoal(), 0.0001);
        assertEquals(12.0, history.get(4).getTotalDistance(), 0.0001);
        assertEquals(2, history.get(4).getSportsCount().get("Run"));
    }

    @Test
    void athleteSummaryReadsLatestValuesFromDb() throws Exception {
        FakeDBService db = new FakeDBService(List.of());
        WeeklyPerformance week = new WeeklyPerformance("Week4", 0L, 30.0, -1.0);
        week.setPersistedValues(12.0, 0.4, 0.4, 0.0, 0.4, 0.4, false);
        week.setPersistedSportTotals("Run", 2, 12.0, 12.0, null);
        db.historyByAthlete.put("athlete-1", Map.of(4, week));
        db.summarySnapshots.put("athlete-1", new DBService.PersistentAthleteSummarySnapshot("Alex", 48.0, 3.2, 20.0));

        HashMap<String, Double> scoreSummary = new LinkedHashMap<>();
        scoreSummary.put("Alex", 3.2);
        db.leaderboardSummaries.put(BootcampConstants.currentYearlyScoreSummary, scoreSummary);

        PersistentActivityProcessService service = createService(db, List.of(), 4);

        String summary = service.getLoggedInAthleteSummary("athlete-1");

        assertTrue(summary.contains("Distance this challenge: 48"));
        assertTrue(summary.contains("Goal this week: 30"));
        assertTrue(summary.contains("Run"));
    }

    @Test
    void zenBotStatsContextReadsLatestValuesFromDb() {
        FakeDBService db = new FakeDBService(List.of());
        WeeklyPerformance week = new WeeklyPerformance("Week4", 0L, 30.0, -1.0);
        week.setPersistedValues(12.0, 0.4, 0.4, 0.0, 0.4, 0.4, false);
        db.historyByAthlete.put("athlete-1", Map.of(4, week));
        db.summarySnapshots.put("athlete-1", new DBService.PersistentAthleteSummarySnapshot("Alex", 48.0, 3.2, 20.0));

        PersistentActivityProcessService service = createService(db, List.of(), 4);

        String context = service.getZenBotStatsContext("athlete-1");

        assertTrue(context.contains("Alex"));
        assertTrue(context.contains("48"));
        assertTrue(context.contains("40.0% of goal"));
    }

    private static PersistentActivityProcessService createService(FakeDBService db,
                                                                 List<StravaActivityResponse> activities,
                                                                 int weeksSinceStart) {
        return new TestPersistentActivityProcessService(db, new FakeStravaService(activities), weeksSinceStart);
    }

    private static BootcampAthlete createAthlete(String athleteId, String firstName, String lastName, double goal) {
        BootcampAthlete athlete = new BootcampAthlete();
        athlete.setId(athleteId);
        athlete.setFirstname(firstName);
        athlete.setLastname(lastName);
        athlete.setEmail(firstName.toLowerCase() + "@example.com");
        athlete.setGoal(goal);
        athlete.setAccessToken("token");
        athlete.setExpiresAt(Instant.now().getEpochSecond() + 3600);
        return athlete;
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

    private static final class TestPersistentActivityProcessService extends PersistentActivityProcessService {
        private final int weeksSinceStart;
        private final FakeDBService fakeDbService;

        private TestPersistentActivityProcessService(FakeDBService fakeDbService, StravaService stravaService, int weeksSinceStart) {
            super(null, stravaService);
            this.fakeDbService = fakeDbService;
            this.weeksSinceStart = weeksSinceStart;
        }

        @Override
        public int getNumberOfWeeksSinceStart() {
            return weeksSinceStart;
        }

        @Override
        protected Long getActiveCompetitionAthleteId(BootcampAthlete athlete) {
            return fakeDbService.hasActiveCompetitionMembership(athlete.getId())
                    ? fakeDbService.ensureCompetitionAthlete(athlete.getId(), athlete.getGoal())
                    : null;
        }

        @Override
        protected boolean hasActiveCompetitionMembership(String athleteId) {
            return fakeDbService.hasActiveCompetitionMembership(athleteId);
        }

        @Override
        protected void replacePersistentCompetitionState(long competitionAthleteId,
                                                         List<PersistentActivityDetailRow> activityRows,
                                                         List<PersistentWeeklyRow> weeklyRows,
                                                         PersistentSummaryRow summaryRow,
                                                         Map<String, PersistentSummarySportRow> summarySportRows) {
            fakeDbService.replacePersistentCompetitionState(competitionAthleteId, activityRows, weeklyRows, summaryRow, summarySportRows);
            fakeDbService.materializeAthleteState(competitionAthleteId, weeklyRows, summaryRow);
        }

        @Override
        protected void replaceCompetitionHonourRoll(long competitionId,
                                                    Map<Integer, PersistentHonourRollRow> honourRollRows) {
            fakeDbService.replaceCompetitionHonourRoll(competitionId, honourRollRows);
        }

        @Override
        protected HashMap<Integer, HashMap<String, Double>> calculatePersistentHonourRollMap(long competitionId, boolean distance) {
            return fakeDbService.calculatePersistentHonourRollMap(competitionId, distance);
        }

        @Override
        public Map<String, HashMap<String, Double>> getSortedSummaries() {
            return fakeDbService.getPersistentLeaderboardSummaries(1L);
        }

        @Override
        public Map<Integer, WeeklyPerformance> getAthleteHistory(String athleteId) {
            return fakeDbService.getPersistentAthleteHistory(athleteId);
        }

        @Override
        public String getLoggedInAthleteSummary(String athleteId) {
            DBService.PersistentAthleteSummarySnapshot snapshot = fakeDbService.getPersistentAthleteSummarySnapshot(athleteId);
            if (snapshot == null) {
                return "";
            }

            WeeklyPerformance currentWeek = fakeDbService.getPersistentAthleteHistory(athleteId).get(getNumberOfWeeksSinceStart());
            StringBuilder sports = new StringBuilder();
            if (currentWeek != null && currentWeek.getSports() != null) {
                for (Map.Entry<String, Double> entry : currentWeek.getSports().entrySet()) {
                    sports.append(entry.getKey()).append(" ").append(entry.getValue() % 1 == 0 ? String.valueOf(entry.getValue().intValue()) : String.valueOf(entry.getValue())).append("km\n");
                }
            }

            return "Liewe " + snapshot.athleteFirstName() + ",\n\n" +
                    "Distance this challenge: " + (snapshot.distanceToDate() % 1 == 0 ? (int) snapshot.distanceToDate() : snapshot.distanceToDate()) + "km\n" +
                    "Total points: " + snapshot.scoreToDate() + "\n" +
                    "Sports: \n" + sports +
                    "Original weekly goal: " + (snapshot.originalWeeklyGoal() % 1 == 0 ? (int) snapshot.originalWeeklyGoal() : snapshot.originalWeeklyGoal()) + "km\n\n" +
                    (currentWeek == null ? "" : currentWeek.toString()) +
                    "OVERALL SCORE COMPETITION:\n";
        }

        @Override
        public String getZenBotStatsContext(String athleteId) {
            DBService.PersistentAthleteSummarySnapshot snapshot = fakeDbService.getPersistentAthleteSummarySnapshot(athleteId);
            if (snapshot == null) {
                return "No athlete stats are currently available.";
            }
            WeeklyPerformance currentWeek = fakeDbService.getPersistentAthleteHistory(athleteId).get(getNumberOfWeeksSinceStart());
            if (currentWeek == null) {
                return "No athlete stats are currently available.";
            }
            return "Athlete " + snapshot.athleteFirstName() + " has " + (snapshot.distanceToDate() % 1 == 0 ? (int) snapshot.distanceToDate() : snapshot.distanceToDate()) + "km total, score " + snapshot.scoreToDate() + ", and current week progress " + (currentWeek.getTotalPercentOfGoal() * 100) + "% of goal.";
        }

        @Override
        public List<PerformanceResponse> getPerformanceList() {
            return fakeDbService.getPersistentPerformanceList(1L);
        }

        @Override
        public HashMap<Integer, HashMap<String, Double>> getHonourRollTotalDistance() {
            return fakeDbService.getPersistentHonourRollTotalDistance(1L);
        }

        @Override
        public HashMap<Integer, HashMap<String, Double>> getHonourRollPercentageOfGoal() {
            return fakeDbService.getPersistentHonourRollPercentageOfGoal(1L);
        }

        @Override
        public boolean addActivityEvent(String athleteId, StravaActivityResponse activity) {
            BootcampAthlete athlete = fakeDbService.findAthleteByStravaID(athleteId);
            if (athlete == null) {
                return false;
            }
            try {
                prepareAthleteSummary(athlete);
            } catch (Exception e) {
                throw new IllegalStateException("Unable to rebuild persistent athlete state after webhook create", e);
            }
            return true;
        }
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

    private static class FakeDBService {
        private final List<BootcampAthlete> allAthletes;
        private final Map<String, BootcampAthlete> athletesByStravaId = new HashMap<>();
        private final List<ReplacedCompetitionState> replacedCompetitionStates = new ArrayList<>();
        private final Map<String, HashMap<String, Double>> leaderboardSummaries = new HashMap<>();
        private final HashMap<Integer, HashMap<String, Double>> honourRollDistance = new HashMap<>();
        private final HashMap<Integer, HashMap<String, Double>> honourRollPercent = new HashMap<>();
        private final Map<String, Map<Integer, WeeklyPerformance>> historyByAthlete = new HashMap<>();
        private final Map<String, DBService.PersistentAthleteSummarySnapshot> summarySnapshots = new HashMap<>();
        private final Map<Long, String> athleteIdsByCompetitionAthleteId = new HashMap<>();
        private int findAthleteByStravaIdCalls;

        private FakeDBService(List<BootcampAthlete> allAthletes) {
            this.allAthletes = allAthletes;
            for (BootcampAthlete athlete : allAthletes) {
                athletesByStravaId.put(athlete.getId(), athlete);
            }
        }

        public List<BootcampAthlete> findAllAthletes() {
            return allAthletes;
        }

        public BootcampAthlete findAthleteByStravaID(String stravaId) {
            findAthleteByStravaIdCalls++;
            return athletesByStravaId.get(stravaId);
        }

        public long ensureCompetitionAthlete(String athleteId, Double startingGoal) {
            athleteIdsByCompetitionAthleteId.put(1L, athleteId);
            return 1L;
        }

        public boolean hasActiveCompetitionMembership(String athleteId) {
            return athletesByStravaId.containsKey(athleteId);
        }

        public void replacePersistentCompetitionState(long competitionAthleteId,
                                                      List<PersistentActivityProcessService.PersistentActivityDetailRow> activityRows,
                                                      List<PersistentActivityProcessService.PersistentWeeklyRow> weeklyRows,
                                                      PersistentActivityProcessService.PersistentSummaryRow summaryRow,
                                                      Map<String, PersistentActivityProcessService.PersistentSummarySportRow> summarySportRows) {
            replacedCompetitionStates.add(new ReplacedCompetitionState(
                    competitionAthleteId,
                    new ArrayList<>(activityRows),
                    new ArrayList<>(weeklyRows),
                    summaryRow,
                    new LinkedHashMap<>(summarySportRows)
            ));
        }

        public void replaceCompetitionHonourRoll(long competitionId,
                                                 Map<Integer, PersistentActivityProcessService.PersistentHonourRollRow> honourRollRows) {
            honourRollDistance.clear();
            honourRollPercent.clear();
            for (PersistentActivityProcessService.PersistentHonourRollRow row : honourRollRows.values()) {
                HashMap<String, Double> distanceWinner = new LinkedHashMap<>();
                distanceWinner.put(row.distanceWinnerName(), row.distanceWinnerValue());
                honourRollDistance.put(row.weekNumber(), distanceWinner);

                HashMap<String, Double> percentWinner = new LinkedHashMap<>();
                percentWinner.put(row.percentWinnerName(), row.percentWinnerValue());
                honourRollPercent.put(row.weekNumber(), percentWinner);
            }
        }

        public Map<String, HashMap<String, Double>> getPersistentLeaderboardSummaries(long competitionId) {
            return leaderboardSummaries;
        }

        public HashMap<Integer, HashMap<String, Double>> getPersistentHonourRollTotalDistance(long competitionId) {
            return honourRollDistance;
        }

        public HashMap<Integer, HashMap<String, Double>> getPersistentHonourRollPercentageOfGoal(long competitionId) {
            return honourRollPercent;
        }

        public Map<Integer, WeeklyPerformance> getPersistentAthleteHistory(String athleteId) {
            return historyByAthlete.getOrDefault(athleteId, Map.of());
        }

        public DBService.PersistentAthleteSummarySnapshot getPersistentAthleteSummarySnapshot(String athleteId) {
            return summarySnapshots.get(athleteId);
        }

        public List<PerformanceResponse> getPersistentPerformanceList(long competitionId) {
            List<PerformanceResponse> performances = new ArrayList<>();
            for (Map.Entry<String, DBService.PersistentAthleteSummarySnapshot> entry : summarySnapshots.entrySet()) {
                PerformanceResponse performance = new PerformanceResponse();
                performance.setAthlete(athletesByStravaId.get(entry.getKey()));
                performance.setDistanceToDate(entry.getValue().distanceToDate());
                performance.setScoreToDate(entry.getValue().scoreToDate());
                Map<Integer, WeeklyPerformance> history = historyByAthlete.getOrDefault(entry.getKey(), Map.of());
                for (Map.Entry<Integer, WeeklyPerformance> weeklyEntry : history.entrySet()) {
                    performance.addWeeklyPerformance(weeklyEntry.getValue(), weeklyEntry.getKey());
                }
                for (WeeklyPerformance week : history.values()) {
                    for (Map.Entry<String, Double> sportEntry : week.getSports().entrySet()) {
                        performance.getSports().merge(sportEntry.getKey(), sportEntry.getValue(), Double::sum);
                    }
                }
                performances.add(performance);
            }
            return performances;
        }

        public HashMap<Integer, HashMap<String, Double>> calculatePersistentHonourRollMap(long competitionId, boolean distance) {
            return distance ? honourRollDistance : honourRollPercent;
        }

        private void materializeAthleteState(long competitionAthleteId,
                                             List<PersistentActivityProcessService.PersistentWeeklyRow> weeklyRows,
                                             PersistentActivityProcessService.PersistentSummaryRow summaryRow) {
            String athleteId = athleteIdsByCompetitionAthleteId.get(competitionAthleteId);
            if (athleteId == null) {
                return;
            }

            Map<Integer, WeeklyPerformance> history = new LinkedHashMap<>();
            for (PersistentActivityProcessService.PersistentWeeklyRow row : weeklyRows) {
                WeeklyPerformance week = new WeeklyPerformance("Week" + row.weekNumber(), 0L, row.weekGoal(), -1.0);
                week.setPersistedValues(row.totalDistance(), row.totalPercentOfGoal(), row.weekGoalAchievementScore(), row.weekProgressionBonus(), row.weekScore(), row.averageWeeklyScore(), row.isSick());
                for (PersistentActivityProcessService.PersistentWeeklySportRow sportRow : row.sportRows().values()) {
                    week.setPersistedSportTotals(sportRow.sportType(), sportRow.activityCount(), sportRow.calculatedDistanceTotal(), sportRow.originalDistanceTotal(), sportRow.originalDurationTotal());
                }
                history.put(row.weekNumber(), week);
            }
            historyByAthlete.put(athleteId, history);

            BootcampAthlete athlete = athletesByStravaId.get(athleteId);
            summarySnapshots.put(athleteId, new DBService.PersistentAthleteSummarySnapshot(
                    athlete != null ? athlete.getFirstname() : athleteId,
                    summaryRow.distanceToDate(),
                    summaryRow.scoreToDate(),
                    athlete != null && athlete.getGoal() != null ? athlete.getGoal() : summaryRow.originalWeeklyGoal()
            ));
        }

        private record ReplacedCompetitionState(long competitionAthleteId,
                                                List<PersistentActivityProcessService.PersistentActivityDetailRow> activityRows,
                                                List<PersistentActivityProcessService.PersistentWeeklyRow> weeklyRows,
                                                PersistentActivityProcessService.PersistentSummaryRow summaryRow,
                                                Map<String, PersistentActivityProcessService.PersistentSummarySportRow> summarySportRows) {
        }

    }

    private static final class ActivityProcessServiceTestLegacyHarness extends ActivityProcessService {
        private final int weeksSinceStart;

        private ActivityProcessServiceTestLegacyHarness(List<StravaActivityResponse> activities, int weeksSinceStart) {
            this.weeksSinceStart = weeksSinceStart;
            try {
                setObjectField(this, "strava", new FakeStravaService(activities));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public int getNumberOfWeeksSinceStart() {
            return weeksSinceStart;
        }

        private PerformanceResponse buildPerformanceForAthleteUsingOldLogic(BootcampAthlete athlete)
                throws IOException, CredentialStoreException, NoSuchAlgorithmException, SQLException {
            FakeStravaService strava = getFakeStravaService();
            BootcampAthlete refreshedAthlete = strava.refreshToken(athlete);
            PerformanceResponse performance = new PerformanceResponse();
            performance.setAthlete(refreshedAthlete);

            List<StravaActivityResponse> stravaActivities = strava
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

        private FakeStravaService getFakeStravaService() {
            try {
                return (FakeStravaService) getObjectField(this, "strava");
            } catch (Exception e) {
                throw new RuntimeException(e);
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
    }
}
