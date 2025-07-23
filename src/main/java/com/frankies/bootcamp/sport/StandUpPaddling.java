package com.frankies.bootcamp.sport;

import com.frankies.bootcamp.model.strava.StravaActivityResponse;

public class StandUpPaddling extends DurationSport {
    public final static Double DISTANCE_PER_HOUR = 7.5;

    public StandUpPaddling(StravaActivityResponse activity) {
        calculateDistance(activity, DISTANCE_PER_HOUR);
        super.setSportType(StandUpPaddling.class.getSimpleName());
    }
}
