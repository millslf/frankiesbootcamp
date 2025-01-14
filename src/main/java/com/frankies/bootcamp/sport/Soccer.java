package com.frankies.bootcamp.sport;

import com.frankies.bootcamp.model.StravaActivityResponse;

public class Soccer extends DistanceSport {
    public final static Double CALCULATED_DISTANCE_MULTIPLIER = 1.5;
    public final static String SPORT = "Soccer/Hockey";

    public Soccer(StravaActivityResponse activity) {
        calculateDistance(activity, CALCULATED_DISTANCE_MULTIPLIER);
        super.setSportType(SPORT);
    }
}
