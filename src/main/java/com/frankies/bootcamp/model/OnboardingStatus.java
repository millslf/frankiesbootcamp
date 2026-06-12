package com.frankies.bootcamp.model;

public class OnboardingStatus {
    private final OnboardingState state;
    private final boolean appUserReady;
    private final boolean stravaLinked;
    private final boolean hasCompetitionMembership;

    public OnboardingStatus(OnboardingState state,
                            boolean appUserReady,
                            boolean stravaLinked,
                            boolean hasCompetitionMembership) {
        this.state = state;
        this.appUserReady = appUserReady;
        this.stravaLinked = stravaLinked;
        this.hasCompetitionMembership = hasCompetitionMembership;
    }

    public OnboardingState getState() {
        return state;
    }

    public boolean isAppUserReady() {
        return appUserReady;
    }

    public boolean isStravaLinked() {
        return stravaLinked;
    }

    public boolean hasCompetitionMembership() {
        return hasCompetitionMembership;
    }
}
