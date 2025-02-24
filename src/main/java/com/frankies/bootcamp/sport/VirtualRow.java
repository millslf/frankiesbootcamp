package com.frankies.bootcamp.sport;

import com.frankies.bootcamp.model.strava.StravaActivityResponse;

public class VirtualRow extends DistanceSport {
    public final static Double CALCULATED_DISTANCE_MULTIPLIER = 1.0;

    public VirtualRow(StravaActivityResponse activity) {
        calculateDistance(activity, CALCULATED_DISTANCE_MULTIPLIER);
        super.setSportType("Virtual Row");
    }
}
