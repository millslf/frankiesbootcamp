package com.frankies.bootcamp.sport;

import com.frankies.bootcamp.model.StravaActivityResponse;

public class WeightTraining extends DurationSport {
    public final static Double DISTANCE_PER_HOUR = 5.0;
    public final static String SPORT = "Weight training";

    public WeightTraining(StravaActivityResponse activity) {
        calculateDistance(activity, DISTANCE_PER_HOUR);
        super.setSportType(SPORT);
    }
}
