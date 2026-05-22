package com.frankies.bootcamp.model;

import com.frankies.bootcamp.model.strava.StravaActivityResponse;
import com.frankies.bootcamp.sport.Run;
import com.frankies.bootcamp.sport.Workout;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class PerformanceResponseTest {

    @Test
    void addSportTracksTotalsAndActivityDetails() {
        PerformanceResponse performance = new PerformanceResponse();
        Run run = new Run(runActivity(7001001L, 8.0));
        Workout workout = new Workout(workoutActivity(7001002L, 1.0));

        performance.addSport(7001001L, 1, run);
        performance.addSport(7001002L, 1, workout);

        PerformanceResponse.StravaActivityDetails runDetails = performance.getStravaActivityDetailsByStravaID(7001001L);
        PerformanceResponse.StravaActivityDetails workoutDetails = performance.getStravaActivityDetailsByStravaID(7001002L);

        assertNotNull(runDetails);
        assertNotNull(workoutDetails);
        assertEquals(1, runDetails.getWeek());
        assertEquals("Run", runDetails.getSport().getSportType());
        assertEquals("Workout", workoutDetails.getSport().getSportType());
    }

    @Test
    void removeSportReducesButDoesNotDeleteActivityHistoryEntry() {
        PerformanceResponse performance = new PerformanceResponse();
        Run run = new Run(runActivity(7002001L, 5.0));

        performance.addSport(7002001L, 2, run);
        performance.removeSport(performance.getStravaActivityDetailsByStravaID(7002001L));

        assertEquals(0.0, performance.getStravaActivityDetailsByStravaID(7002001L).getSport().getCalculatedDistance(), 5.0);
    }

    @Test
    void getStravaActivityDetailsByStravaIdReturnsNullWhenMissing() {
        PerformanceResponse performance = new PerformanceResponse();

        assertNull(performance.getStravaActivityDetailsByStravaID(999999L));
    }

    private static StravaActivityResponse runActivity(long id, double kilometers) {
        StravaActivityResponse activity = new StravaActivityResponse();
        activity.setId(id);
        activity.setDistance(kilometers * 1000);
        return activity;
    }

    private static StravaActivityResponse workoutActivity(long id, double hours) {
        StravaActivityResponse activity = new StravaActivityResponse();
        activity.setId(id);
        activity.setMoving_time((int) Math.round(hours * 3600));
        return activity;
    }
}
