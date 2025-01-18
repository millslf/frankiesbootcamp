package com.frankies.bootcamp.sport;

import com.frankies.bootcamp.model.strava.StravaActivityResponse;

public class TrailRun extends DistanceSport {
    public final static Double CALCULATED_DISTANCE_MULTIPLIER = 1.2;
    public final static String SPORT = "Trail run";

    public TrailRun(StravaActivityResponse activity) {
        calculateDistance(activity, CALCULATED_DISTANCE_MULTIPLIER);
        super.setSportType(SPORT);
    }
}
