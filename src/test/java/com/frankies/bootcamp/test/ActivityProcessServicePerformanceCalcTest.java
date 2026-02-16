package com.frankies.bootcamp.test;

import com.frankies.bootcamp.constant.BootcampConstants;
import com.frankies.bootcamp.factories.StravaActivityTestFactory;
import com.frankies.bootcamp.model.BootcampAthlete;
import com.frankies.bootcamp.model.PerformanceResponse;
import com.frankies.bootcamp.model.WeeklyPerformance;
import com.frankies.bootcamp.model.strava.StravaActivityResponse;
import com.frankies.bootcamp.service.ActivityProcessService;
import com.frankies.bootcamp.service.DBService;
import com.frankies.bootcamp.service.StravaService;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ActivityProcessServicePerformanceCalcTest {

    private ActivityProcessService svc; // proxy: real methods, no ctor run
    private DBService db;
    private StravaService strava;
    private BootcampAthlete athlete;

    private final long START = BootcampConstants.START_TIMESTAMP;
    private final long WEEK  = BootcampConstants.WEEK_IN_SECONDS;

    @BeforeEach
    void setup() throws Exception {
        resetStatics();

        // Create proxy that calls real methods but DOES NOT run the constructor
        svc = Mockito.mock(ActivityProcessService.class,
                Mockito.withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));

        db = mock(DBService.class);
        strava = mock(StravaService.class);
        setPrivate(ActivityProcessService.class, svc, "db", db);
        setPrivate(ActivityProcessService.class, svc, "strava", strava);

        // Athlete
        athlete = mock(BootcampAthlete.class, RETURNS_DEEP_STUBS);
        when(athlete.getId()).thenReturn("101");
        when(athlete.getFirstname()).thenReturn("Frankie");
        when(athlete.getLastname()).thenReturn("Bootcamp");
        when(athlete.getEmail()).thenReturn("frankie@example.com");
        when(athlete.getGoal()).thenReturn(50.0);           // weekly goal = 50 km
        when(athlete.isSick(anyInt())).thenReturn(false);   // keep scoring simple
        when(athlete.getExpiresAt()).thenReturn((System.currentTimeMillis()/1000L) + 3600);
        when(athlete.getAccessToken()).thenReturn("tok");

        when(db.findAllAthletes()).thenReturn(List.of(athlete));
        when(strava.refreshToken(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void tearDown() throws Exception {
        resetStatics();
    }

    @Test
    void prepareSummary_aggregatesDistanceAndScore_acrossWeeks() throws Exception {
        // Week 1: RUN 30 km (Run multiplier = 1.0 → 30km). Score vs goal(50): 30 -> 0.5
        StravaActivityResponse wk1_run30 =
                StravaActivityTestFactory.activity("Run", "Run", 30.0, START + 100);

        // Week 3: RUN 110 km (calc 110). Score vs goal(50): > 2*goal -> 1.75
        StravaActivityResponse wk3_run110 =
                StravaActivityTestFactory.activity("Run", "Run", 110.0, START + 2*WEEK + 100);

        when(strava.getAthleteActivitiesForPeriod(eq(START), anyString()))
                .thenReturn(List.of(wk1_run30, wk3_run110)); // ascending order

        // Act
        svc.prepareSummary();

        // Assert
        List<PerformanceResponse> list = svc.getPerformanceList();
        assertNotNull(list);
        assertEquals(1, list.size());

        PerformanceResponse p = list.get(0);
        assertEquals("frankie@example.com", p.getAthlete().getEmail());

        // Distances
        assertEquals(140.0, p.getDistanceToDate(), 1e-9, "distanceToDate must sum calculated distances");

        Map<Integer, WeeklyPerformance> weeks = p.getWeeklyPerformances();
        assertNotNull(weeks);
        assertTrue(weeks.size() >= 3, "should include at least 3 weeks by placement");

        WeeklyPerformance w1 = weeks.get(1);
        WeeklyPerformance w3 = weeks.get(3);
        assertNotNull(w1);
        assertNotNull(w3);

        assertEquals(30.0, w1.getTotalDistance(), 1e-9);
        assertEquals(110.0, w3.getTotalDistance(), 1e-9);

        // Scores: 0.5 (week1) + 1.75 (week3) = 2.25
        assertEquals(2.25, p.getScoreToDate(), 1e-9, "scoreToDate must aggregate week scores correctly");

        // (Optional) Verify sports map totals via reflection (since PerformanceResponse hides it)
        Map<String, Double> sportsMap = getSportsMap(p);
        assertNotNull(sportsMap);
        assertEquals(140.0, sportsMap.getOrDefault("Run", 0.0), 1e-9);
    }

    @Test
    void prepareSummary_handlesDurationSports() throws Exception {
        // Surfing is a duration sport: distance = hours * 7.5
        // 2 hours → 15 km => below 0.5 * goal(25) => weekScore 0.0
        StravaActivityResponse wk1_surf2h =
                StravaActivityTestFactory.activity("Surfing", "Surfing", 0.0, START + 50, 7200);

        when(strava.getAthleteActivitiesForPeriod(eq(START), anyString()))
                .thenReturn(List.of(wk1_surf2h));

        svc.prepareSummary();

        PerformanceResponse p = svc.getPerformanceList().get(0);
        Map<Integer, WeeklyPerformance> weeks = p.getWeeklyPerformances();
        WeeklyPerformance w1 = weeks.get(1);

        assertEquals(15.0, w1.getTotalDistance(), 1e-9);
        assertEquals(0.0, p.getScoreToDate(), 1e-9, "score should be 0 when distance < 0.5*goal");
    }

    /* ---------- helpers ---------- */

    private static void setPrivate(Class<?> type, Object target, String name, Object value) throws Exception {
        Field f = type.getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    @SuppressWarnings("unchecked")
    private static void resetStatics() throws Exception {
        Field f1 = ActivityProcessService.class.getDeclaredField("performanceList");
        f1.setAccessible(true); f1.set(null, null);

        Field f2 = ActivityProcessService.class.getDeclaredField("honourRollTotalDistance");
        f2.setAccessible(true); ((Map<?, ?>) f2.get(null)).clear();

        Field f3 = ActivityProcessService.class.getDeclaredField("honourRollPercentageOfGoal");
        f3.setAccessible(true); ((Map<?, ?>) f3.get(null)).clear();

        Field f4 = ActivityProcessService.class.getDeclaredField("sortedSummaries");
        f4.setAccessible(true); ((Map<?, ?>) f4.get(null)).clear();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Double> getSportsMap(PerformanceResponse p) throws Exception {
        Field sf = PerformanceResponse.class.getDeclaredField("sports");
        sf.setAccessible(true);
        return (Map<String, Double>) sf.get(p);
    }
}

