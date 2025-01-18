package com.frankies.bootcamp.sport;

import com.frankies.bootcamp.model.strava.StravaActivityResponse;

public class Golf extends DistanceSport {
    public final static Double CALCULATED_DISTANCE_MULTIPLIER = 0.75;

    public Golf(StravaActivityResponse activity) {
        calculateDistance(activity, CALCULATED_DISTANCE_MULTIPLIER);
        super.setSportType(Golf.class.getSimpleName());
    }
}
