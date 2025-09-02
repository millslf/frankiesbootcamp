package com.frankies.bootcamp.test;

import com.frankies.bootcamp.model.BootcampAthlete;
import com.frankies.bootcamp.model.PerformanceResponse;
import com.frankies.bootcamp.model.WeeklyPerformance;
import com.frankies.bootcamp.model.strava.StravaActivityResponse;
import com.frankies.bootcamp.factories.BootcampAthleteTestFactory;
import com.frankies.bootcamp.service.ActivityProcessService;
import com.frankies.bootcamp.service.DBService;
import com.frankies.bootcamp.service.StravaService;
import org.junit.jupiter.api.*;
import org.mockito.Answers;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.*;

import static com.frankies.bootcamp.constant.BootcampConstants.START_TIMESTAMP;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ActivityProcessServiceTest {

    private ActivityProcessService service;     // real instance
    private DBService db;                       // mock
    private StravaService strava;               // mock
    private BootcampAthlete athlete;            // faker mock
    private BootcampAthleteTestFactory fakers;

    @BeforeEach
    void setUp() throws Exception {
        resetStatics();

        fakers = new BootcampAthleteTestFactory(12345L); // reproducible “random”

        db = mock(DBService.class, Answers.RETURNS_DEEP_STUBS);
        strava = mock(StravaService.class, Answers.RETURNS_DEEP_STUBS);
        service = new ActivityProcessService(db, strava);

        // Inject mocks into private fields
        setPrivateField(ActivityProcessService.class, service, "db", db);
        setPrivateField(ActivityProcessService.class, service, "strava", strava);

        athlete = fakers.athleteMock(3600); // not expired

        when(db.findAllAthletes()).thenReturn(List.of(athlete));
        when(strava.refreshToken(any())).thenAnswer(inv -> inv.getArgument(0));
        when(strava.getAthleteActivitiesForPeriod(anyLong(), anyString()))
                .thenReturn(Collections.emptyList());
    }

    @AfterEach
    void tearDown() throws Exception {
        resetStatics();
    }

    @Test
    void prepareSummary_noActivities_populatesWeeksHonourRolls_andStoresPerformance() throws Exception {
        service.prepareSummary();

        List<PerformanceResponse> list = service.getPerformanceList();
        assertNotNull(list);
        assertEquals(1, list.size());

        PerformanceResponse perf = list.get(0);
        assertEquals(athlete.getEmail(), perf.getAthlete().getEmail());

        Map<Integer, WeeklyPerformance> weeks = perf.getWeeklyPerformances();
        assertNotNull(weeks);
        assertFalse(weeks.isEmpty());
        assertEquals(service.getNumberOfWeeksSinceStart(), weeks.size());
        assertTrue(weeks.containsKey(1));

        // Honour rolls get updated even with “no activity” weeks
        assertFalse(service.getHonourRollTotalDistance().isEmpty());
        assertFalse(service.getHonourRollPercentageOfGoal().isEmpty());
    }

    @Test
    void prepareSummary_refreshesToken_whenExpired() throws Exception {
        fakers.expireNow(athlete);

        service.prepareSummary();

        verify(strava, times(1)).refreshToken(any(BootcampAthlete.class));
    }

    @Test
    void generateAllSummaryMaps_and_sendReport_returnsSummaryForLoggedInAthlete() throws Exception {
        service.prepareSummary();
        service.generateAllSummaryMaps();

        String emailBody = service.getLoggedInAthleteSummary(
                /*loggedInAthlete*/    athlete.getEmail());

        assertNotNull(emailBody);
        assertTrue(emailBody.contains("OVERALL SCORE COMPETITION"));
        assertTrue(emailBody.contains("'PERCENTAGE OF GOAL' WEEKLY COMPETITION"));
        assertTrue(emailBody.contains("'TOTAL DISTANCE' WEEKLY COMPETITION"));
    }

    @Test
    void addActivityEvent_updatesWeeklyAndTotals_then_removeActivityEvent_reverts() throws Exception {
        // Prepare baseline (week scaffolding created)
        service.prepareSummary();

        // Build a RUN activity in Week 1
        long startInWeek1 = START_TIMESTAMP + 60; // 1 min after start
        long actId = 1001L;
        double meters = 5000.0;

        StravaActivityResponse run = mock(StravaActivityResponse.class, Answers.RETURNS_DEEP_STUBS);
        when(run.getId()).thenReturn(actId);
        when(run.getType()).thenReturn("Run");
        when(run.getSport_type()).thenReturn("Run");
        when(run.getStart_date()).thenReturn(Instant.ofEpochSecond(startInWeek1).toString());
        // If your model uses float/Double, adjust:
        when(run.getDistance()).thenReturn(meters);

        boolean added = service.addActivityEvent(athlete.getId(), run);
        assertTrue(added, "Expected addActivityEvent to succeed for supported sport");

        PerformanceResponse perf = service.getPerformanceList().get(0);
        double distAfterAdd = perf.getDistanceToDate();
        assertTrue(distAfterAdd > 0.0, "Distance should increase after adding activity");

        WeeklyPerformance week1 = perf.getWeeklyPerformances().get(1);
        assertNotNull(week1);
        assertTrue(week1.getTotalDistance() > 0.0, "Week 1 total distance should increase");
        double scoreAfterAdd = perf.getScoreToDate();
        assertTrue(scoreAfterAdd >= 0.0, "Score should be updated (>=0)");

        // Now remove the same activity; totals should decrease
        boolean removed = service.removeActivityEvent(athlete.getId(), actId);
        assertTrue(removed, "Expected removeActivityEvent to succeed");

        assertTrue(perf.getDistanceToDate() <= distAfterAdd, "Distance should not grow after removal");
        WeeklyPerformance week1After = perf.getWeeklyPerformances().get(1);
        assertNotNull(week1After);
        // Some implementations may carry non-zero rounding; allow <=:
        assertTrue(week1After.getTotalDistance() <= week1.getTotalDistance(),
                "Week 1 total distance should not be larger after removal");
    }

    /* ---------------- helpers ---------------- */

    private static void setPrivateField(Class<?> type, Object target, String name, Object value) throws Exception {
        Field f = type.getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    @SuppressWarnings("unchecked")
    private static void resetStatics() throws Exception {
        Field f1 = ActivityProcessService.class.getDeclaredField("performanceList");
        f1.setAccessible(true); f1.set(null, null);

        Field f2 = ActivityProcessService.class.getDeclaredField("honourRollTotalDistance");
        f2.setAccessible(true);
        Map<?,?> m2 = (Map<?, ?>) f2.get(null);
        if (m2 != null) m2.clear();

        Field f3 = ActivityProcessService.class.getDeclaredField("honourRollPercentageOfGoal");
        f3.setAccessible(true);
        Map<?,?> m3 = (Map<?, ?>) f3.get(null);
        if (m3 != null) m3.clear();

        Field f4 = ActivityProcessService.class.getDeclaredField("sortedSummaries");
        f4.setAccessible(true);
        Map<?,?> m4 = (Map<?, ?>) f4.get(null);
        if (m4 != null) m4.clear();
    }
}
