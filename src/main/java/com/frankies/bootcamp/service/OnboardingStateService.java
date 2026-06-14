package com.frankies.bootcamp.service;

import com.frankies.bootcamp.model.AuthenticatedUser;
import com.frankies.bootcamp.model.BootcampAthlete;
import com.frankies.bootcamp.model.CompetitionSummaryView;
import com.frankies.bootcamp.model.OnboardingState;
import com.frankies.bootcamp.model.OnboardingStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.sql.SQLException;

@ApplicationScoped
public class OnboardingStateService {
    public interface CompetitionMembershipLookup {
        boolean hasActiveCompetitionMembership(String athleteId) throws SQLException;

        java.util.List<CompetitionSummaryView> listCurrentActiveCompetitions(String athleteId) throws SQLException;

        CompetitionSummaryView findUpcomingCompetition(String athleteId) throws SQLException;

        java.util.List<CompetitionSummaryView> listPastCompetitions(String athleteId) throws SQLException;
    }

    private CompetitionMembershipLookup competitionMembershipLookup;

    @Inject
    public OnboardingStateService(DBService dbService) {
        this.competitionMembershipLookup = new CompetitionMembershipLookup() {
            @Override
            public boolean hasActiveCompetitionMembership(String athleteId) throws SQLException {
                return dbService.hasActiveCompetitionMembership(athleteId);
            }

            @Override
            public java.util.List<CompetitionSummaryView> listCurrentActiveCompetitions(String athleteId) throws SQLException {
                return dbService.listCurrentActiveCompetitions(athleteId);
            }

            @Override
            public CompetitionSummaryView findUpcomingCompetition(String athleteId) throws SQLException {
                return dbService.findUpcomingCompetition(athleteId);
            }

            @Override
            public java.util.List<CompetitionSummaryView> listPastCompetitions(String athleteId) throws SQLException {
                return dbService.listPastCompetitions(athleteId);
            }
        };
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
        java.util.List<CompetitionSummaryView> activeCompetitions = stravaLinked ? competitionMembershipLookup.listCurrentActiveCompetitions(athlete.getId()) : java.util.List.of();
        CompetitionSummaryView upcomingCompetition = stravaLinked ? competitionMembershipLookup.findUpcomingCompetition(athlete.getId()) : null;
        java.util.List<CompetitionSummaryView> pastCompetitions = stravaLinked ? competitionMembershipLookup.listPastCompetitions(athlete.getId()) : java.util.List.of();
        CompetitionSummaryView selectedStatusCompetition = activeCompetitions.isEmpty() ? upcomingCompetition : activeCompetitions.get(0);

        OnboardingState state;
        if (!appUserReady) {
            state = OnboardingState.AUTHENTICATED;
        } else if (!stravaLinked) {
            state = OnboardingState.STRAVA_PENDING;
        } else if (activeCompetitions.size() > 1) {
            state = OnboardingState.COMPETITION_SELECTION_REQUIRED;
        } else if (hasCompetitionMembership) {
            state = OnboardingState.READY;
        } else if (upcomingCompetition != null) {
            state = OnboardingState.COMPETITION_STARTS_SOON;
        } else if (!pastCompetitions.isEmpty()) {
            state = OnboardingState.COMPETITION_HISTORY_ONLY;
        } else {
            state = OnboardingState.COMPETITION_PENDING;
        }

        return new OnboardingStatus(state, appUserReady, stravaLinked, hasCompetitionMembership, selectedStatusCompetition, activeCompetitions, pastCompetitions);
    }
}
