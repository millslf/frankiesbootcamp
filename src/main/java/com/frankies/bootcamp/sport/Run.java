package com.frankies.bootcamp.sport;

import com.frankies.bootcamp.model.StravaActivityResponse;

public class Run extends DistanceSport {
    public final static Double CALCULATED_DISTANCE_MULTIPLIER = 1.0;

    public Run(StravaActivityResponse activity) {
        calculateDistance(activity, CALCULATED_DISTANCE_MULTIPLIER);
        super.setSportType(Run.class.getSimpleName());
    }
}
