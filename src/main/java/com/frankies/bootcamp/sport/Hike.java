package com.frankies.bootcamp.sport;

import com.frankies.bootcamp.model.strava.StravaActivityResponse;

public class Hike extends DistanceSport {
    public final static Double CALCULATED_DISTANCE_MULTIPLIER = 0.85;

    public Hike(StravaActivityResponse activity) {
        calculateDistance(activity, CALCULATED_DISTANCE_MULTIPLIER);
        super.setSportType(Hike.class.getSimpleName());
    }
}
