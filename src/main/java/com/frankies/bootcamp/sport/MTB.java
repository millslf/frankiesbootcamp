package com.frankies.bootcamp.sport;

import com.frankies.bootcamp.model.StravaActivityResponse;

public class MTB extends DistanceSport {
    public final static Double CALCULATED_DISTANCE_MULTIPLIER = 0.5;
    public final static String SPORT = "MTB ride";

    public MTB(StravaActivityResponse activity) {
        calculateDistance(activity, CALCULATED_DISTANCE_MULTIPLIER);
        super.setSportType(SPORT);
    }
}
