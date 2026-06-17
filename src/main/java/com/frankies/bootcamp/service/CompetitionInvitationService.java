package com.frankies.bootcamp.service;

import com.frankies.bootcamp.model.AuthenticatedUser;
import com.frankies.bootcamp.model.BootcampAthlete;
import com.frankies.bootcamp.model.CompetitionInviteCandidateView;
import com.frankies.bootcamp.model.CompetitionInvitationPageView;
import com.frankies.bootcamp.model.CompetitionInvitationStatus;
import com.frankies.bootcamp.model.CompetitionInvitationView;
import com.frankies.bootcamp.model.CompetitionSummaryView;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;

import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@ApplicationScoped
public class CompetitionInvitationService {
    private static final Pattern EMAIL_SPLIT = Pattern.compile("[,;\\n\\r\\t ]+");

    interface InvitationRepository {
        boolean isCompetitionAdmin(long competitionId, String athleteId) throws SQLException;
        boolean athleteBelongsToCompetition(String athleteId, long competitionId) throws SQLException;
        boolean hasActiveInvitationForEmail(long competitionId, String normalizedEmail) throws SQLException;
        boolean hasActiveInvitationForUser(long competitionId, String invitedUserId) throws SQLException;
        long createInvitation(long competitionId, String invitedEmail, String invitedUserId, String token, String invitedByUserId, Instant expiresAt) throws SQLException;
        CompetitionInvitationView findInvitationByToken(String token) throws SQLException;
        CompetitionInvitationView findInvitationById(long invitationId) throws SQLException;
        List<CompetitionInvitationView> listPendingInvitationsForUser(String userId, String email, String athleteId) throws SQLException;
        List<CompetitionInvitationView> listPendingInvitationsForCompetition(long competitionId) throws SQLException;
        List<BootcampAthlete> listCompetitionAthletes(long competitionId) throws SQLException;
        List<CompetitionInviteCandidateView> searchInviteCandidates(long competitionId, String query) throws SQLException;
        void acceptInvitation(long invitationId, String athleteId, double startingGoal) throws SQLException;
        void declineInvitation(long invitationId) throws SQLException;
        void expireInvitation(long invitationId) throws SQLException;
        CompetitionInvitationView getInvitationForCompetitionAndToken(long competitionId, String token) throws SQLException;
        CompetitionSummaryView findCompetition(long competitionId) throws SQLException;
    }

    private final InvitationRepository repository;
    private final CompetitionInvitationEmailService emailService;

    @Inject
    public CompetitionInvitationService(DBService dbService, CompetitionInvitationEmailService emailService) {
        this.repository = new InvitationRepository() {
            @Override
            public boolean isCompetitionAdmin(long competitionId, String athleteId) throws SQLException {
                return dbService.isCompetitionAdmin(competitionId, athleteId);
            }

            @Override
            public boolean athleteBelongsToCompetition(String athleteId, long competitionId) throws SQLException {
                return dbService.athleteBelongsToCompetition(athleteId, competitionId);
            }

            @Override
            public boolean hasActiveInvitationForEmail(long competitionId, String normalizedEmail) throws SQLException {
                return dbService.hasActiveCompetitionInvitationForEmail(competitionId, normalizedEmail);
            }

            @Override
            public boolean hasActiveInvitationForUser(long competitionId, String invitedUserId) throws SQLException {
                return dbService.hasActiveCompetitionInvitationForUser(competitionId, invitedUserId);
            }

            @Override
            public long createInvitation(long competitionId, String invitedEmail, String invitedUserId, String token, String invitedByUserId, Instant expiresAt) throws SQLException {
                return dbService.createCompetitionInvitation(competitionId, invitedEmail, invitedUserId, token, invitedByUserId, expiresAt);
            }

            @Override
            public CompetitionInvitationView findInvitationByToken(String token) throws SQLException {
                return dbService.findCompetitionInvitationByToken(token);
            }

            @Override
            public CompetitionInvitationView findInvitationById(long invitationId) throws SQLException {
                return dbService.findCompetitionInvitationById(invitationId);
            }

            @Override
            public List<CompetitionInvitationView> listPendingInvitationsForUser(String userId, String email, String athleteId) throws SQLException {
                return dbService.listPendingCompetitionInvitationsForUser(userId, email, athleteId);
            }

            @Override
            public List<CompetitionInvitationView> listPendingInvitationsForCompetition(long competitionId) throws SQLException {
                return dbService.listPendingCompetitionInvitationsForCompetition(competitionId);
            }

            @Override
            public List<BootcampAthlete> listCompetitionAthletes(long competitionId) throws SQLException {
                return dbService.listCompetitionAthletes(competitionId);
            }

            @Override
            public List<CompetitionInviteCandidateView> searchInviteCandidates(long competitionId, String query) throws SQLException {
                return dbService.searchCompetitionInviteCandidates(competitionId, query);
            }

            @Override
            public void acceptInvitation(long invitationId, String athleteId, double startingGoal) throws SQLException {
                dbService.acceptCompetitionInvitation(invitationId, athleteId, startingGoal);
            }

            @Override
            public void declineInvitation(long invitationId) throws SQLException {
                dbService.declineCompetitionInvitation(invitationId);
            }

            @Override
            public void expireInvitation(long invitationId) throws SQLException {
                dbService.expireCompetitionInvitation(invitationId);
            }

            @Override
            public CompetitionInvitationView getInvitationForCompetitionAndToken(long competitionId, String token) throws SQLException {
                return dbService.findCompetitionInvitationByCompetitionAndToken(competitionId, token);
            }

            @Override
            public com.frankies.bootcamp.model.CompetitionSummaryView findCompetition(long competitionId) throws SQLException {
                return dbService.findCompetitionSummary(competitionId);
            }
        };
        this.emailService = emailService;
    }

    protected CompetitionInvitationService() {
        this.repository = null;
        this.emailService = null;
    }

    CompetitionInvitationService(InvitationRepository repository, CompetitionInvitationEmailService emailService) {
        this.repository = repository;
        this.emailService = emailService;
    }

    public boolean isAdmin(long competitionId, String athleteId) throws SQLException {
        return repository.isCompetitionAdmin(competitionId, athleteId);
    }

    public CompetitionInvitationPageView loadAdminPage(long competitionId, String query) throws SQLException {
        CompetitionSummaryView competition = repository.findCompetition(competitionId);
        List<BootcampAthlete> accepted = repository.listCompetitionAthletes(competitionId);
        List<CompetitionInvitationView> pending = repository.listPendingInvitationsForCompetition(competitionId);
        List<CompetitionInviteCandidateView> candidates = query == null || query.isBlank()
                ? List.of()
                : repository.searchInviteCandidates(competitionId, query.trim());
        return new CompetitionInvitationPageView(
                competitionId,
                competition == null ? "Competition" : competition.getName(),
                accepted,
                pending,
                candidates,
                query,
                null,
                null
        );
    }

    public List<CompetitionInvitationView> listPendingForUser(AuthenticatedUser user, BootcampAthlete athlete) throws SQLException {
        String userId = user == null ? null : user.getUserId();
        String email = user == null ? null : user.getEmail();
        String athleteId = athlete == null ? null : athlete.getId();
        return repository.listPendingInvitationsForUser(userId, email, athleteId);
    }

    public CompetitionInvitationView resolveInvitationToken(String token) throws SQLException {
        if (token == null || token.isBlank()) {
            return null;
        }
        CompetitionInvitationView invitation = repository.findInvitationByToken(token.trim());
        if (invitation == null) {
            return null;
        }
        if (!CompetitionInvitationStatus.PENDING.dbValue().equalsIgnoreCase(invitation.getStatus())) {
            return invitation;
        }
        if (invitation.getExpiresAt() != null && invitation.getExpiresAt().isBefore(Instant.now())) {
            repository.expireInvitation(invitation.getId());
            return repository.findInvitationById(invitation.getId());
        }
        return invitation;
    }

    public InviteSubmissionResult inviteByEmails(long competitionId, String invitedByUserId, String rawEmails, HttpServletRequest request) throws SQLException {
        Map<String, String> normalizedToOriginal = splitEmails(rawEmails);
        List<InviteRecord> created = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        for (Map.Entry<String, String> entry : normalizedToOriginal.entrySet()) {
            String normalizedEmail = entry.getKey();
            if (!isValidEmail(normalizedEmail)) {
                errors.add(entry.getValue() + ": invalid email");
                continue;
            }
            if (repository.hasActiveInvitationForEmail(competitionId, normalizedEmail)) {
                errors.add(entry.getValue() + ": already invited");
                continue;
            }
            long invitationId = createSingleInvitation(competitionId, normalizedEmail, null, invitedByUserId);
            created.add(new InviteRecord(invitationId, normalizedEmail, null));
            sendInvitationEmail(competitionId, invitationId, normalizedEmail, request);
        }
        return new InviteSubmissionResult(created, errors);
    }

    public InviteRecord inviteExistingUser(long competitionId, String invitedByUserId, String invitedUserId, String invitedEmail, HttpServletRequest request) throws SQLException {
        String normalizedEmail = normalizeEmail(invitedEmail);
        if (normalizedEmail == null) {
            throw new IllegalArgumentException("Selected athlete must have an email address.");
        }
        if (invitedUserId != null && repository.athleteBelongsToCompetition(invitedUserId, competitionId)) {
            throw new IllegalArgumentException("Selected athlete is already active in this competition.");
        }
        if (repository.hasActiveInvitationForEmail(competitionId, normalizedEmail) || (invitedUserId != null && repository.hasActiveInvitationForUser(competitionId, invitedUserId))) {
            throw new IllegalArgumentException("Selected athlete already has a pending invite.");
        }
        long invitationId = createSingleInvitation(competitionId, normalizedEmail, invitedUserId, invitedByUserId);
        sendInvitationEmail(competitionId, invitationId, normalizedEmail, request);
        return new InviteRecord(invitationId, normalizedEmail, invitedUserId);
    }

    public InvitationDecisionResult acceptInvitation(long invitationId, BootcampAthlete athlete, double startingGoal, String userId) throws SQLException {
        CompetitionInvitationView invitation = repository.findInvitationById(invitationId);
        if (invitation == null) {
            return InvitationDecisionResult.error("Invitation not found.");
        }
        if (!CompetitionInvitationStatus.PENDING.dbValue().equalsIgnoreCase(invitation.getStatus())) {
            return InvitationDecisionResult.error("Invitation is already " + invitation.getStatus() + ".");
        }
        if (invitation.getExpiresAt() != null && invitation.getExpiresAt().isBefore(Instant.now())) {
            repository.expireInvitation(invitationId);
            return InvitationDecisionResult.error("Invitation has expired.");
        }
        if (athlete == null || athlete.getId() == null || athlete.getId().isBlank()) {
            return InvitationDecisionResult.error("Strava link required.");
        }
        repository.acceptInvitation(invitationId, athlete.getId(), startingGoal);
        return InvitationDecisionResult.success("Invitation accepted.");
    }

    public InvitationDecisionResult declineInvitation(long invitationId) throws SQLException {
        CompetitionInvitationView invitation = repository.findInvitationById(invitationId);
        if (invitation == null) {
            return InvitationDecisionResult.error("Invitation not found.");
        }
        if (CompetitionInvitationStatus.PENDING.dbValue().equalsIgnoreCase(invitation.getStatus())) {
            repository.declineInvitation(invitationId);
        }
        return InvitationDecisionResult.success("Invitation declined.");
    }

    public long createInvitationAndNotify(long competitionId,
                                          String invitedByUserId,
                                          String invitedEmail,
                                          String invitedUserId,
                                          String baseUrl,
                                          String competitionName) throws SQLException {
        long invitationId = createSingleInvitation(competitionId, normalizeEmail(invitedEmail), invitedUserId, invitedByUserId);
        CompetitionInvitationView invitation = repository.findInvitationById(invitationId);
        if (invitation != null && invitation.getInvitedEmail() != null) {
            String inviteUrl = buildInvitationUrl(baseUrl, invitation.getToken());
            emailService.sendInvitation(invitation.getInvitedEmail(), "You're invited to " + competitionName, buildBody(competitionName, inviteUrl));
        }
        return invitationId;
    }

    private void sendInvitationEmail(long competitionId, long invitationId, String invitedEmail, HttpServletRequest request) throws SQLException {
        CompetitionInvitationView invitation = repository.findInvitationById(invitationId);
        CompetitionSummaryView competition = repository.findCompetition(competitionId);
        if (invitation == null || competition == null || invitedEmail == null) {
            return;
        }
        String inviteUrl = buildInvitationUrl(baseUrl(request), invitation.getToken());
        emailService.sendInvitation(invitedEmail, "You're invited to " + competition.getName(), buildBody(competition.getName(), inviteUrl));
    }

    public String buildInvitationUrl(String baseUrl, String token) {
        String root = baseUrl == null ? "" : baseUrl.replaceAll("/+$", "");
        return root + "/invite?token=" + token;
    }

    private String baseUrl(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        builder.append(request.getScheme()).append("://").append(request.getServerName());
        int port = request.getServerPort();
        if (!("http".equalsIgnoreCase(request.getScheme()) && port == 80) && !("https".equalsIgnoreCase(request.getScheme()) && port == 443)) {
            builder.append(":").append(port);
        }
        builder.append(request.getContextPath());
        return builder.toString();
    }

    private long createSingleInvitation(long competitionId, String invitedEmail, String invitedUserId, String invitedByUserId) throws SQLException {
        String token = randomToken();
        Instant expiresAt = Instant.now().plus(14, ChronoUnit.DAYS);
        return repository.createInvitation(competitionId, invitedEmail, invitedUserId, token, invitedByUserId, expiresAt);
    }

    private Map<String, String> splitEmails(String rawEmails) {
        Map<String, String> emails = new LinkedHashMap<>();
        if (rawEmails == null) {
            return emails;
        }
        for (String part : EMAIL_SPLIT.split(rawEmails.trim())) {
            String trimmed = part == null ? null : part.trim();
            if (trimmed == null || trimmed.isBlank()) {
                continue;
            }
            String normalized = normalizeEmail(trimmed);
            if (normalized != null) {
                emails.putIfAbsent(normalized, trimmed);
            }
        }
        return emails;
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isValidEmail(String email) {
        return email != null && email.contains("@") && email.indexOf('@') > 0 && email.indexOf('@') < email.length() - 1;
    }

    private String randomToken() {
        return UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
    }

    private String buildBody(String competitionName, String inviteUrl) {
        return "You have been invited to join " + competitionName + ".\n\nOpen this link to continue: " + inviteUrl;
    }

    public record InviteRecord(long invitationId, String invitedEmail, String invitedUserId) {}

    public record InviteSubmissionResult(List<InviteRecord> createdInvites, List<String> errors) {}

    public record InvitationDecisionResult(boolean success, String message) {
        public static InvitationDecisionResult success(String message) { return new InvitationDecisionResult(true, message); }
        public static InvitationDecisionResult error(String message) { return new InvitationDecisionResult(false, message); }
    }
}
