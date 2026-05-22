package com.frankies.bootcamp.sport;

import com.frankies.bootcamp.model.strava.StravaActivityResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

class SportFactoryTest {

    @Test
    void mapsRunToRunSport() {
        BaseSport sport = SportFactory.getSport(activity("Run", "Run", 5000.0, null));

        assertInstanceOf(Run.class, sport);
        assertEquals(5.0, sport.getCalculatedDistance(), 0.0001);
    }

    @Test
    void mapsTrailRunToTrailRunSport() {
        BaseSport sport = SportFactory.getSport(activity("Run", "TrailRun", 5000.0, null));

        assertInstanceOf(TrailRun.class, sport);
        assertEquals(6.0, sport.getCalculatedDistance(), 0.0001);
    }

    @Test
    void mapsWalkUsingWalkMultiplier() {
        BaseSport sport = SportFactory.getSport(activity("Walk", "Walk", 4000.0, null));

        assertInstanceOf(Walk.class, sport);
        assertEquals(3.0, sport.getCalculatedDistance(), 0.0001);
    }

    @Test
    void mapsWorkoutUsingDurationRule() {
        BaseSport sport = SportFactory.getSport(activity("Workout", "Workout", null, 3600));

        assertInstanceOf(Workout.class, sport);
        assertEquals(5.0, sport.getCalculatedDistance(), 0.0001);
    }

    @Test
    void returnsNullForUnsupportedSport() {
        BaseSport sport = SportFactory.getSport(activity("Badminton", "Badminton", 1000.0, null));

        assertNull(sport);
    }

    private static StravaActivityResponse activity(String type, String sportType, Double distanceMeters, Integer movingTimeSeconds) {
        StravaActivityResponse activity = new StravaActivityResponse();
        activity.setType(type);
        activity.setSport_type(sportType);
        if (distanceMeters != null) {
            activity.setDistance(distanceMeters);
        }
        if (movingTimeSeconds != null) {
            activity.setMoving_time(movingTimeSeconds);
        }
        return activity;
    }
}
