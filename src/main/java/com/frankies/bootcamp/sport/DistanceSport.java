package com.frankies.bootcamp.sport;

import com.frankies.bootcamp.model.StravaActivityResponse;

public abstract class DistanceSport extends BaseSport {
    private Double originalDistance;

    public Double getOriginalDistance() {
        return originalDistance;
    }

    public void setOriginalDistance(Double originalDistance) {
        this.originalDistance = originalDistance;
    }

    @Override
    public void calculateDistance(StravaActivityResponse activity, Double multiplier) {
        super.setCalculatedDistance(activity.getDistance()/1000 * multiplier);
        setOriginalDistance(activity.getDistance()/1000);
    }

}
