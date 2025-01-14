package com.frankies.bootcamp.sport;

import com.frankies.bootcamp.model.StravaActivityResponse;

public class EMountainBikeRide extends DistanceSport {
    public final static Double CALCULATED_DISTANCE_MULTIPLIER = 0.4;
    public final static String SPORT = "EBike MTB ride";

    public EMountainBikeRide(StravaActivityResponse activity) {
        calculateDistance(activity, CALCULATED_DISTANCE_MULTIPLIER);
        super.setSportType(SPORT);
    }
}
