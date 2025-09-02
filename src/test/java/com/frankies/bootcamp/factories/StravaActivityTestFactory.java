package com.frankies.bootcamp.factories;

import com.frankies.bootcamp.model.strava.StravaActivityResponse;

import java.time.Instant;

public final class StravaActivityTestFactory {
    private StravaActivityTestFactory() {}

    public static StravaActivityResponse activity(
            String type, String sportType, double distanceKm, long startEpochSec, int elapsedSeconds) {

        StravaActivityResponse a = new StravaActivityResponse();
        a.setType(type);
        a.setSport_type(sportType);
        a.setDistance(distanceKm * 1000.0);
        a.setElapsed_time(elapsedSeconds);
        a.setMoving_time(elapsedSeconds);
        a.setStart_date(Instant.ofEpochSecond(startEpochSec).toString());
        a.setName(type + " " + sportType + " @ " + startEpochSec);
        a.setId(startEpochSec); // just unique-ish
        return a;
    }

    public static StravaActivityResponse activity(
            String type, String sportType, double distanceKm, long startEpochSec) {
        return activity(type, sportType, distanceKm, startEpochSec, 1800);
    }

    public static StravaActivityResponse dist(String type, String sportType, double km) {
        StravaActivityResponse a = new StravaActivityResponse();
        a.setType(type);
        a.setSport_type(sportType);
        a.setDistance(km * 1000.0);
        a.setElapsed_time(3600);
        return a;
    }

    public static StravaActivityResponse dur(String type, String sportType, int seconds) {
        StravaActivityResponse a = new StravaActivityResponse();
        a.setType(type);
        a.setSport_type(sportType);
        a.setElapsed_time(seconds);
        a.setDistance(0.0);
        return a;
    }
}
