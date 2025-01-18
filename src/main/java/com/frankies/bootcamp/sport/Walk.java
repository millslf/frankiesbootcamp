package com.frankies.bootcamp.sport;

import com.frankies.bootcamp.model.strava.StravaActivityResponse;

public class Walk extends DistanceSport {
    public final static Double CALCULATED_DISTANCE_MULTIPLIER = 0.75;

    public Walk(StravaActivityResponse activity) {
        calculateDistance(activity, CALCULATED_DISTANCE_MULTIPLIER);
        super.setSportType(Walk.class.getSimpleName());
    }
}
