package com.frankies.bootcamp.service;

import com.frankies.bootcamp.model.AuthenticatedUser;
import com.frankies.bootcamp.model.BootcampAthlete;
import com.frankies.bootcamp.model.CompetitionSummaryView;
import com.frankies.bootcamp.model.OnboardingState;
import com.frankies.bootcamp.model.OnboardingStatus;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OnboardingStateServiceTest {

    @Test
    void resolveReturnsStravaPendingWhenNoLinkedAthleteExists() throws SQLException {
        OnboardingStateService service = new OnboardingStateService(new StubCompetitionMembershipLookup(false, null, List.of()));

        OnboardingStatus status = service.resolve(user("usr-1"), null);

        assertEquals(OnboardingState.STRAVA_PENDING, status.getState());
        assertTrue(status.isAppUserReady());
        assertFalse(status.isStravaLinked());
        assertFalse(status.hasCompetitionMembership());
    }

    @Test
    void resolveReturnsCompetitionPendingWhenStravaLinkedWithoutMembership() throws SQLException {
        OnboardingStateService service = new OnboardingStateService(new StubCompetitionMembershipLookup(false, null, List.of()));

        OnboardingStatus status = service.resolve(user("usr-1"), athlete("12345"));

        assertEquals(OnboardingState.COMPETITION_PENDING, status.getState());
        assertTrue(status.isStravaLinked());
        assertFalse(status.hasCompetitionMembership());
    }

    @Test
    void resolveReturnsReadyWhenStravaLinkedWithCompetitionMembership() throws SQLException {
        OnboardingStateService service = new OnboardingStateService(new StubCompetitionMembershipLookup(true, null, List.of()));

        OnboardingStatus status = service.resolve(user("usr-1"), athlete("12345"));

        assertEquals(OnboardingState.READY, status.getState());
        assertTrue(status.isStravaLinked());
        assertTrue(status.hasCompetitionMembership());
    }

    @Test
    void resolveReturnsCompetitionStartsSoonWhenUpcomingCompetitionExists() throws SQLException {
        CompetitionSummaryView upcoming = new CompetitionSummaryView(9L, "Winter Bootcamp", "Australia/Sydney", Instant.now().getEpochSecond() + 86400, null, "active");
        OnboardingStateService service = new OnboardingStateService(new StubCompetitionMembershipLookup(false, upcoming, List.of()));

        OnboardingStatus status = service.resolve(user("usr-1"), athlete("12345"));

        assertEquals(OnboardingState.COMPETITION_STARTS_SOON, status.getState());
        assertEquals(upcoming, status.getActiveCompetition());
    }

    @Test
    void resolveReturnsCompetitionHistoryOnlyWhenOnlyPastCompetitionsExist() throws SQLException {
        CompetitionSummaryView past = new CompetitionSummaryView(7L, "Autumn Bootcamp", "Australia/Sydney", Instant.now().getEpochSecond() - 86400 * 30, Instant.now().getEpochSecond() - 86400, "active");
        OnboardingStateService service = new OnboardingStateService(new StubCompetitionMembershipLookup(false, null, List.of(past)));

        OnboardingStatus status = service.resolve(user("usr-1"), athlete("12345"));

        assertEquals(OnboardingState.COMPETITION_HISTORY_ONLY, status.getState());
        assertEquals(1, status.getPastCompetitions().size());
    }

    private static AuthenticatedUser user(String userId) {
        AuthenticatedUser user = new AuthenticatedUser();
        user.setUserId(userId);
        user.setEmail("athlete@example.com");
        user.setDisplayName("Athlete Example");
        return user;
    }

    private static BootcampAthlete athlete(String athleteId) {
        BootcampAthlete athlete = new BootcampAthlete();
        athlete.setId(athleteId);
        athlete.setEmail("athlete@example.com");
        return athlete;
    }

    private static final class StubCompetitionMembershipLookup implements OnboardingStateService.CompetitionMembershipLookup {
        private final boolean hasMembership;
        private final CompetitionSummaryView upcomingCompetition;
        private final List<CompetitionSummaryView> pastCompetitions;

        private StubCompetitionMembershipLookup(boolean hasMembership, CompetitionSummaryView upcomingCompetition, List<CompetitionSummaryView> pastCompetitions) {
            this.hasMembership = hasMembership;
            this.upcomingCompetition = upcomingCompetition;
            this.pastCompetitions = pastCompetitions;
        }

        @Override
        public boolean hasActiveCompetitionMembership(String athleteId) {
            return hasMembership;
        }

        @Override
        public CompetitionSummaryView findUpcomingCompetition(String athleteId) {
            return upcomingCompetition;
        }

        @Override
        public List<CompetitionSummaryView> listPastCompetitions(String athleteId) {
            return pastCompetitions;
        }
    }
}
