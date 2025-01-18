package com.frankies.bootcamp.sport;

import com.frankies.bootcamp.model.strava.StravaActivityResponse;

public class GravelRide extends DistanceSport {
    public final static Double CALCULATED_DISTANCE_MULTIPLIER = 0.4;
    public final static String SPORT = "Gravel ride";

    public GravelRide(StravaActivityResponse activity) {
        calculateDistance(activity, CALCULATED_DISTANCE_MULTIPLIER);
        super.setSportType(SPORT);
    }
}
