package com.frankies.bootcamp.sport;

import com.frankies.bootcamp.model.strava.StravaActivityResponse;

public class Surf extends DurationSport {
    public final static Double DISTANCE_PER_HOUR = 7.5;

    public Surf(StravaActivityResponse activity) {
        calculateDistance(activity, DISTANCE_PER_HOUR);
        super.setSportType(Surf.class.getSimpleName());
    }
}
