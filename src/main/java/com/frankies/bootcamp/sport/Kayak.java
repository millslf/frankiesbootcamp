package com.frankies.bootcamp.sport;

import com.frankies.bootcamp.model.strava.StravaActivityResponse;

public class Kayak extends DistanceSport {
    public final static Double CALCULATED_DISTANCE_MULTIPLIER = 1.5;

    public Kayak(StravaActivityResponse activity) {
        calculateDistance(activity, CALCULATED_DISTANCE_MULTIPLIER);
        super.setSportType(Kayak.class.getSimpleName());
    }
}
