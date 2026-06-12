package com.frankies.bootcamp.service;

import com.frankies.bootcamp.model.AuthenticatedUser;
import com.frankies.bootcamp.model.BootcampAthlete;
import com.frankies.bootcamp.model.OnboardingState;
import com.frankies.bootcamp.model.OnboardingStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.sql.SQLException;

@ApplicationScoped
public class OnboardingStateService {
    @FunctionalInterface
    public interface CompetitionMembershipLookup {
        boolean hasActiveCompetitionMembership(String athleteId) throws SQLException;
    }

    private CompetitionMembershipLookup competitionMembershipLookup;

    @Inject
    public OnboardingStateService(DBService dbService) {
        this.competitionMembershipLookup = dbService::hasActiveCompetitionMembership;
    }

    OnboardingStateService(CompetitionMembershipLookup competitionMembershipLookup) {
        this.competitionMembershipLookup = competitionMembershipLookup;
    }

    protected OnboardingStateService() {
    }

    public OnboardingStatus resolve(AuthenticatedUser user, BootcampAthlete athlete) throws SQLException {
        boolean appUserReady = user != null && user.getUserId() != null && !user.getUserId().isBlank();
        boolean stravaLinked = athlete != null && athlete.getId() != null && !athlete.getId().isBlank() && !athlete.getId().startsWith("local-");
        boolean hasCompetitionMembership = stravaLinked && competitionMembershipLookup.hasActiveCompetitionMembership(athlete.getId());

        OnboardingState state;
        if (!appUserReady) {
            state = OnboardingState.AUTHENTICATED;
        } else if (!stravaLinked) {
            state = OnboardingState.STRAVA_PENDING;
        } else if (!hasCompetitionMembership) {
            state = OnboardingState.COMPETITION_PENDING;
        } else {
            state = OnboardingState.READY;
        }

        return new OnboardingStatus(state, appUserReady, stravaLinked, hasCompetitionMembership);
    }
}
