package com.frankies.bootcamp.sport;

import com.frankies.bootcamp.model.strava.StravaActivityResponse;

public class VirtualRide extends DistanceSport {
    public final static Double CALCULATED_DISTANCE_MULTIPLIER = 0.33;
    public final static String SPORT = "VirtualRide";

    public VirtualRide(StravaActivityResponse activity) {
        calculateDistance(activity, CALCULATED_DISTANCE_MULTIPLIER);
        super.setSportType(SPORT);
    }
}
