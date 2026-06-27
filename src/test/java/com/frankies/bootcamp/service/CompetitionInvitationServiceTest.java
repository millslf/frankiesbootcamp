package com.frankies.bootcamp.service;

import com.frankies.bootcamp.model.CompetitionInviteCandidateView;
import com.frankies.bootcamp.model.CompetitionInvitationView;
import com.frankies.bootcamp.model.CompetitionSummaryView;
import com.frankies.bootcamp.model.BootcampAthlete;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompetitionInvitationServiceTest {

    @Test
    void bulkInviteDedupesWithinSubmittedListAndSendsEmail() throws Exception {
        FakeRepository repository = new FakeRepository();
        repository.competitions.put(7L, new CompetitionSummaryView(7L, "Winter Bootcamp", "Australia/Sydney", Instant.now().getEpochSecond(), null, "active"));
        RecordingEmailService emailService = new RecordingEmailService();
        CompetitionInvitationService service = new CompetitionInvitationService(repository, emailService);

        CompetitionInvitationService.InviteSubmissionResult result = service.inviteByEmails(7L, "usr-admin", "ALICE@example.com, alice@example.com;bob@example.com", "Keep it up!", null);

        assertEquals(2, result.createdInvites().size());
        assertEquals(2, repository.invites.size());
        assertEquals(2, emailService.sentSubjects.size());
        assertTrue(emailService.sentBodies.get(0).contains("Keep it up!"));
    }

    @Test
    void inviteExistingUserRejectsAlreadyActiveMembers() throws Exception {
        FakeRepository repository = new FakeRepository();
        repository.activeMembers.add("athlete-1");
        CompetitionInvitationService service = new CompetitionInvitationService(repository, new RecordingEmailService());

        assertThrows(IllegalArgumentException.class,
                () -> service.inviteExistingUser(7L, "usr-admin", "athlete-1", "athlete@example.com", null, null));
    }

    @Test
    void acceptInvitationCreatesMembershipAndMarksInviteAccepted() throws Exception {
        FakeRepository repository = new FakeRepository();
        repository.competitions.put(7L, new CompetitionSummaryView(7L, "Winter Bootcamp", "Australia/Sydney", Instant.now().getEpochSecond(), null, "active"));
        CompetitionInvitationView invitation = repository.createStoredInvitation(7L, "athlete@example.com", "usr-1");
        CompetitionInvitationService service = new CompetitionInvitationService(repository, new RecordingEmailService());
        com.frankies.bootcamp.model.BootcampAthlete athlete = new com.frankies.bootcamp.model.BootcampAthlete();
        athlete.setId("athlete-1");

        CompetitionInvitationService.InvitationDecisionResult result = service.acceptInvitation(invitation.getId(), athlete, 24.5, "usr-1");

        assertTrue(result.success());
        assertTrue(repository.activeMembers.contains("athlete-1"));
        assertEquals("accepted", repository.invites.get(invitation.getId()).getStatus());
    }

    @Test
    void resolveInvitationTokenExpiresStaleInvites() throws Exception {
        FakeRepository repository = new FakeRepository();
        CompetitionInvitationView invitation = repository.createStoredInvitation(7L, "athlete@example.com", "usr-1");
        repository.invites.put(invitation.getId(), new CompetitionInvitationView(
                invitation.getId(),
                invitation.getCompetitionId(),
                invitation.getCompetitionName(),
                invitation.getInvitedEmail(),
                invitation.getInvitedUserId(),
                invitation.getToken(),
                "pending",
                invitation.getInvitedByUserId(),
                invitation.getCreatedAt(),
                invitation.getUpdatedAt(),
                Instant.now().minusSeconds(60),
                invitation.getAcceptedAt(),
                invitation.getDeclinedAt()
        ));
        CompetitionInvitationService service = new CompetitionInvitationService(repository, new RecordingEmailService());

        CompetitionInvitationView resolved = service.resolveInvitationToken(invitation.getToken());

        assertNotNull(resolved);
        assertEquals("expired", resolved.getStatus());
    }

    @Test
    void removePendingInvitationDeletesIt() throws Exception {
        FakeRepository repository = new FakeRepository();
        CompetitionInvitationView invitation = repository.createStoredInvitation(7L, "athlete@example.com", "usr-1");
        CompetitionInvitationService service = new CompetitionInvitationService(repository, new RecordingEmailService());

        service.removePendingInvitation(invitation.getId());

        assertTrue(repository.invites.isEmpty());
    }

    private static final class RecordingEmailService extends CompetitionInvitationEmailService {
        private final List<String> sentSubjects = new ArrayList<>();
        private final List<String> sentBodies = new ArrayList<>();

        private RecordingEmailService() {
            super();
        }

        @Override
        public boolean sendInvitation(String to, String subject, String body) {
            sentSubjects.add(subject);
            sentBodies.add(body);
            return true;
        }
    }

    private static final class FakeRepository implements CompetitionInvitationService.InvitationRepository {
        private final Map<Long, CompetitionInvitationView> invites = new LinkedHashMap<>();
        private final Map<Long, CompetitionSummaryView> competitions = new HashMap<>();
        private final Map<String, Boolean> admins = new HashMap<>();
        private final List<String> activeMembers = new ArrayList<>();
        private long nextId = 1L;

        private CompetitionInvitationView createStoredInvitation(long competitionId, String email, String invitedByUserId) {
            long id = nextId++;
            String token = "token-" + id;
            CompetitionInvitationView invitation = new CompetitionInvitationView(
                    id,
                    competitionId,
                    competitions.get(competitionId) == null ? "Competition" : competitions.get(competitionId).getName(),
                    email,
                    null,
                    token,
                    "pending",
                    invitedByUserId,
                    Instant.now(),
                    Instant.now(),
                    Instant.now().plusSeconds(3600),
                    null,
                    null
            );
            invites.put(id, invitation);
            return invitation;
        }

        @Override
        public boolean isCompetitionAdmin(long competitionId, String athleteId) {
            return admins.getOrDefault(competitionId + ":" + athleteId, Boolean.FALSE);
        }

        @Override
        public boolean athleteBelongsToCompetition(String athleteId, long competitionId) {
            return activeMembers.contains(athleteId);
        }

        @Override
        public boolean hasActiveInvitationForEmail(long competitionId, String normalizedEmail) {
            return invites.values().stream().anyMatch(inv -> inv.getCompetitionId() == competitionId
                    && normalizedEmail.equals(inv.getInvitedEmail())
                    && "pending".equals(inv.getStatus()));
        }

        @Override
        public boolean hasActiveInvitationForUser(long competitionId, String invitedUserId) {
            return invites.values().stream().anyMatch(inv -> inv.getCompetitionId() == competitionId
                    && invitedUserId != null
                    && invitedUserId.equals(inv.getInvitedUserId())
                    && "pending".equals(inv.getStatus()));
        }

        @Override
        public long createInvitation(long competitionId, String invitedEmail, String invitedUserId, String token, String invitedByUserId, Instant expiresAt) {
            long id = nextId++;
            CompetitionInvitationView invitation = new CompetitionInvitationView(
                    id,
                    competitionId,
                    competitions.get(competitionId) == null ? "Competition" : competitions.get(competitionId).getName(),
                    invitedEmail,
                    invitedUserId,
                    token,
                    "pending",
                    invitedByUserId,
                    Instant.now(),
                    Instant.now(),
                    expiresAt,
                    null,
                    null
            );
            invites.put(id, invitation);
            return id;
        }

        @Override
        public CompetitionInvitationView findInvitationByToken(String token) {
            return invites.values().stream().filter(inv -> token.equals(inv.getToken())).findFirst().orElse(null);
        }

        @Override
        public CompetitionInvitationView findInvitationById(long invitationId) {
            return invites.get(invitationId);
        }

        @Override
        public List<CompetitionInvitationView> listPendingInvitationsForUser(String userId, String email, String athleteId) {
            return invites.values().stream()
                    .filter(CompetitionInvitationView::isPending)
                    .filter(inv -> (userId != null && userId.equals(inv.getInvitedUserId()))
                            || (email != null && email.equals(inv.getInvitedEmail())))
                    .toList();
        }

        @Override
        public List<CompetitionInvitationView> listPendingInvitationsForCompetition(long competitionId) {
            return invites.values().stream()
                    .filter(inv -> inv.getCompetitionId() == competitionId)
                    .filter(CompetitionInvitationView::isPending)
                    .toList();
        }

        @Override
        public List<CompetitionInviteCandidateView> searchInviteCandidates(long competitionId, String query) {
            return List.of();
        }

        @Override
        public List<BootcampAthlete> listCompetitionAthletes(long competitionId) {
            return List.of();
        }

        @Override
        public void acceptInvitation(long invitationId, String athleteId, double startingGoal) {
            CompetitionInvitationView invitation = invites.get(invitationId);
            invites.put(invitationId, new CompetitionInvitationView(
                    invitation.getId(),
                    invitation.getCompetitionId(),
                    invitation.getCompetitionName(),
                    invitation.getInvitedEmail(),
                    invitation.getInvitedUserId(),
                    invitation.getToken(),
                    "accepted",
                    invitation.getInvitedByUserId(),
                    invitation.getCreatedAt(),
                    invitation.getUpdatedAt(),
                    invitation.getExpiresAt(),
                    Instant.now(),
                    invitation.getDeclinedAt()
            ));
            activeMembers.add(athleteId);
        }

        @Override
        public void declineInvitation(long invitationId) {
            CompetitionInvitationView invitation = invites.get(invitationId);
            invites.put(invitationId, new CompetitionInvitationView(
                    invitation.getId(),
                    invitation.getCompetitionId(),
                    invitation.getCompetitionName(),
                    invitation.getInvitedEmail(),
                    invitation.getInvitedUserId(),
                    invitation.getToken(),
                    "declined",
                    invitation.getInvitedByUserId(),
                    invitation.getCreatedAt(),
                    invitation.getUpdatedAt(),
                    invitation.getExpiresAt(),
                    invitation.getAcceptedAt(),
                    Instant.now()
            ));
        }

        @Override
        public boolean deleteInvitation(long invitationId) {
            return invites.remove(invitationId) != null;
        }

        @Override
        public void expireInvitation(long invitationId) {
            CompetitionInvitationView invitation = invites.get(invitationId);
            invites.put(invitationId, new CompetitionInvitationView(
                    invitation.getId(),
                    invitation.getCompetitionId(),
                    invitation.getCompetitionName(),
                    invitation.getInvitedEmail(),
                    invitation.getInvitedUserId(),
                    invitation.getToken(),
                    "expired",
                    invitation.getInvitedByUserId(),
                    invitation.getCreatedAt(),
                    invitation.getUpdatedAt(),
                    invitation.getExpiresAt(),
                    invitation.getAcceptedAt(),
                    invitation.getDeclinedAt()
            ));
        }

        @Override
        public CompetitionInvitationView getInvitationForCompetitionAndToken(long competitionId, String token) {
            return findInvitationByToken(token);
        }

        @Override
        public CompetitionSummaryView findCompetition(long competitionId) {
            return competitions.get(competitionId);
        }
    }
}
