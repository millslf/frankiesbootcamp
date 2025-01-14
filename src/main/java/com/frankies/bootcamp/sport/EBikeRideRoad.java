package com.frankies.bootcamp.sport;

import com.frankies.bootcamp.model.StravaActivityResponse;

public class EBikeRideRoad extends DistanceSport {
    public final static Double CALCULATED_DISTANCE_MULTIPLIER = 0.33;
    public final static String SPORT = "EBike ride";

    public EBikeRideRoad(StravaActivityResponse activity) {
        calculateDistance(activity, CALCULATED_DISTANCE_MULTIPLIER);
        super.setSportType(SPORT);
    }
}
