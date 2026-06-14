package com.frankies.bootcamp.model;

public class OnboardingStatus {
    private final OnboardingState state;
    private final boolean appUserReady;
    private final boolean stravaLinked;
    private final boolean hasCompetitionMembership;
    private final CompetitionSummaryView activeCompetition;
    private final java.util.List<CompetitionSummaryView> pastCompetitions;

    public OnboardingStatus(OnboardingState state,
                            boolean appUserReady,
                            boolean stravaLinked,
                            boolean hasCompetitionMembership,
                            CompetitionSummaryView activeCompetition,
                            java.util.List<CompetitionSummaryView> pastCompetitions) {
        this.state = state;
        this.appUserReady = appUserReady;
        this.stravaLinked = stravaLinked;
        this.hasCompetitionMembership = hasCompetitionMembership;
        this.activeCompetition = activeCompetition;
        this.pastCompetitions = pastCompetitions;
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

    public CompetitionSummaryView getActiveCompetition() {
        return activeCompetition;
    }

    public java.util.List<CompetitionSummaryView> getPastCompetitions() {
        return pastCompetitions;
    }
}
