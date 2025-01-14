package com.frankies.bootcamp.sport;

import com.frankies.bootcamp.model.StravaActivityResponse;

public class Swim extends DistanceSport {
    public final static Double CALCULATED_DISTANCE_MULTIPLIER = 5.0;

    public Swim(StravaActivityResponse activity) {
        calculateDistance(activity, CALCULATED_DISTANCE_MULTIPLIER);
        super.setSportType(Swim.class.getSimpleName());
    }
}
