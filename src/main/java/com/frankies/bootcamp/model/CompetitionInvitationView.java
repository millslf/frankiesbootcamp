package com.frankies.bootcamp.model;

import java.time.Instant;

public class CompetitionInvitationView {
    private final long id;
    private final long competitionId;
    private final String competitionName;
    private final String invitedEmail;
    private final String invitedUserId;
    private final String token;
    private final String status;
    private final String invitedByUserId;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final Instant expiresAt;
    private final Instant acceptedAt;
    private final Instant declinedAt;

    public CompetitionInvitationView(long id,
                                     long competitionId,
                                     String competitionName,
                                     String invitedEmail,
                                     String invitedUserId,
                                     String token,
                                     String status,
                                     String invitedByUserId,
                                     Instant createdAt,
                                     Instant updatedAt,
                                     Instant expiresAt,
                                     Instant acceptedAt,
                                     Instant declinedAt) {
        this.id = id;
        this.competitionId = competitionId;
        this.competitionName = competitionName;
        this.invitedEmail = invitedEmail;
        this.invitedUserId = invitedUserId;
        this.token = token;
        this.status = status;
        this.invitedByUserId = invitedByUserId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.expiresAt = expiresAt;
        this.acceptedAt = acceptedAt;
        this.declinedAt = declinedAt;
    }

    public long getId() { return id; }
    public long getCompetitionId() { return competitionId; }
    public String getCompetitionName() { return competitionName; }
    public String getInvitedEmail() { return invitedEmail; }
    public String getInvitedUserId() { return invitedUserId; }
    public String getToken() { return token; }
    public String getStatus() { return status; }
    public String getInvitedByUserId() { return invitedByUserId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getAcceptedAt() { return acceptedAt; }
    public Instant getDeclinedAt() { return declinedAt; }

    public boolean isPending() {
        return CompetitionInvitationStatus.PENDING.dbValue().equalsIgnoreCase(status);
    }
}
