package com.frankies.bootcamp.sport;

import com.frankies.bootcamp.model.StravaActivityResponse;

public abstract class DurationSport extends BaseSport {
    private Double originalDuration;

    public Double getOriginalDuration() {
        return originalDuration;
    }

    public void setOriginalDuration(Double originalDuration) {
        this.originalDuration = originalDuration;
    }

    @Override
    public void calculateDistance(StravaActivityResponse activity, Double multiplier) {
        super.setCalculatedDistance((double) activity.getElapsed_time() / 3600 * multiplier);
        setOriginalDuration((double) activity.getElapsed_time() / 3600);
    }

}
