package com.frankies.bootcamp.test;

import com.frankies.bootcamp.model.BootcampAthlete;
import com.frankies.bootcamp.model.PerformanceResponse;
import com.frankies.bootcamp.model.WeeklyPerformance;
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

class ActivityProcessServicePrepareSummaryTest {

    private ActivityProcessService service;   // created via Mockito (no ctor/initializers)

    private DBService db;
    private StravaService strava;

    private BootcampAthlete athlete;

    @BeforeEach
    void setUp() throws Exception {
        resetStatics();

        //Create a mock that delegates to real methods but DOES NOT run the constructor
        service = Mockito.mock(
                ActivityProcessService.class,
                Mockito.withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS)
        );

        db = mock(DBService.class, RETURNS_DEEP_STUBS);
        strava = mock(StravaService.class, RETURNS_DEEP_STUBS);

        // Inject mocks into private fields (constructor never ran, so fields are null)
        setPrivateField(ActivityProcessService.class, service, "db", db);
        setPrivateField(ActivityProcessService.class, service, "strava", strava);

        // athlete fixture
        athlete = mock(BootcampAthlete.class, RETURNS_DEEP_STUBS);
        when(athlete.getId()).thenReturn("1");
        when(athlete.getFirstname()).thenReturn("Testy");
        when(athlete.getLastname()).thenReturn("McAthlete");
        when(athlete.getEmail()).thenReturn("test@bootcamp.local");
        when(athlete.getGoal()).thenReturn(42.0);
        when(athlete.isSick(anyInt())).thenReturn(false);
        when(athlete.getExpiresAt()).thenReturn((System.currentTimeMillis() / 1000L) + 3600); // not expired
        when(athlete.getAccessToken()).thenReturn("token");

        when(db.findAllAthletes()).thenReturn(List.of(athlete));
        when(strava.getAthleteActivitiesForPeriod(anyLong(), anyString()))
                .thenReturn(Collections.emptyList());
        when(strava.refreshToken(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void tearDown() throws Exception {
        resetStatics();
    }

    @Test
    void prepareSummary_createsPerformanceAndWeeks_whenNoActivities() throws Exception {
        service.prepareSummary();

        List<PerformanceResponse> list = service.getPerformanceList();
        assertNotNull(list);
        assertEquals(1, list.size());

        PerformanceResponse perf = list.get(0);
        assertEquals("test@bootcamp.local", perf.getAthlete().getEmail());

        Map<Integer, WeeklyPerformance> weeks = perf.getWeeklyPerformances();
        assertNotNull(weeks);
        assertEquals(service.getNumberOfWeeksSinceStart(), weeks.size());
        assertTrue(weeks.containsKey(1));
    }

    @Test
    void prepareSummary_refreshesToken_whenExpired() throws Exception {
        when(athlete.getExpiresAt()).thenReturn((System.currentTimeMillis() / 1000L) - 10);

        service.prepareSummary();

        verify(strava, times(1)).refreshToken(any(BootcampAthlete.class));
    }

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
        f2.setAccessible(true); ((Map<?, ?>) f2.get(null)).clear();

        Field f3 = ActivityProcessService.class.getDeclaredField("honourRollPercentageOfGoal");
        f3.setAccessible(true); ((Map<?, ?>) f3.get(null)).clear();

        Field f4 = ActivityProcessService.class.getDeclaredField("sortedSummaries");
        f4.setAccessible(true); ((Map<?, ?>) f4.get(null)).clear();
    }
}
