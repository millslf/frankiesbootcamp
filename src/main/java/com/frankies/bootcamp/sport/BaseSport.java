package com.frankies.bootcamp.sport;

import com.frankies.bootcamp.model.strava.StravaActivityResponse;

public abstract class BaseSport {
    private Double calculatedDistance;
    private String sportType;

    public abstract void calculateDistance(StravaActivityResponse activity, Double multiplier);

    public Double getCalculatedDistance() {
        return calculatedDistance;
    }

    public void setCalculatedDistance(Double calculatedDistance) {
        this.calculatedDistance = calculatedDistance;
    }

    public String getSportType() {
        return sportType;
    }

    public void setSportType(String sportType) {
        this.sportType = sportType;
    }

}
