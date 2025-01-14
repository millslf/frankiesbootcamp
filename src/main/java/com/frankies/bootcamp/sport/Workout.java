package com.frankies.bootcamp.sport;

import com.frankies.bootcamp.model.StravaActivityResponse;

public class Workout extends DurationSport {
    public final static Double DISTANCE_PER_HOUR = 5.0;

    public Workout(StravaActivityResponse activity) {
        calculateDistance(activity, DISTANCE_PER_HOUR);
        super.setSportType(Workout.class.getSimpleName());
    }
}
