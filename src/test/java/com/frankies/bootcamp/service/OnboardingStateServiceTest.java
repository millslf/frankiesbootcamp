package com.frankies.bootcamp.service;

import com.frankies.bootcamp.model.AuthenticatedUser;
import com.frankies.bootcamp.model.BootcampAthlete;
import com.frankies.bootcamp.model.OnboardingState;
import com.frankies.bootcamp.model.OnboardingStatus;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OnboardingStateServiceTest {

    @Test
    void resolveReturnsStravaPendingWhenNoLinkedAthleteExists() throws SQLException {
        OnboardingStateService service = new OnboardingStateService(athleteId -> false);

        OnboardingStatus status = service.resolve(user("usr-1"), null);

        assertEquals(OnboardingState.STRAVA_PENDING, status.getState());
        assertTrue(status.isAppUserReady());
        assertFalse(status.isStravaLinked());
        assertFalse(status.hasCompetitionMembership());
    }

    @Test
    void resolveReturnsCompetitionPendingWhenStravaLinkedWithoutMembership() throws SQLException {
        OnboardingStateService service = new OnboardingStateService(athleteId -> false);

        OnboardingStatus status = service.resolve(user("usr-1"), athlete("12345"));

        assertEquals(OnboardingState.COMPETITION_PENDING, status.getState());
        assertTrue(status.isStravaLinked());
        assertFalse(status.hasCompetitionMembership());
    }

    @Test
    void resolveReturnsReadyWhenStravaLinkedWithCompetitionMembership() throws SQLException {
        OnboardingStateService service = new OnboardingStateService(athleteId -> true);

        OnboardingStatus status = service.resolve(user("usr-1"), athlete("12345"));

        assertEquals(OnboardingState.READY, status.getState());
        assertTrue(status.isStravaLinked());
        assertTrue(status.hasCompetitionMembership());
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
}
