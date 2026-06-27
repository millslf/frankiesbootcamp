package com.frankies.bootcamp.model;

import java.util.List;

public class CompetitionInvitationPageView {
    private final long competitionId;
    private final String competitionName;
    private final List<BootcampAthlete> acceptedAthletes;
    private final List<CompetitionInvitationView> pendingInvitations;
    private final List<CompetitionInviteCandidateView> candidates;
    private final String searchQuery;
    private final String feedbackMessage;
    private final String errorMessage;

    public CompetitionInvitationPageView(long competitionId,
                                         String competitionName,
    List<BootcampAthlete> acceptedAthletes,
                                         List<CompetitionInvitationView> pendingInvitations,
                                         List<CompetitionInviteCandidateView> candidates,
                                         String searchQuery,
                                         String feedbackMessage,
                                         String errorMessage) {
        this.competitionId = competitionId;
        this.competitionName = competitionName;
        this.acceptedAthletes = acceptedAthletes;
        this.pendingInvitations = pendingInvitations;
        this.candidates = candidates;
        this.searchQuery = searchQuery;
        this.feedbackMessage = feedbackMessage;
        this.errorMessage = errorMessage;
    }

    public long getCompetitionId() { return competitionId; }
    public String getCompetitionName() { return competitionName; }
    public List<BootcampAthlete> getAcceptedAthletes() { return acceptedAthletes; }
    public List<CompetitionInvitationView> getPendingInvitations() { return pendingInvitations; }
    public List<CompetitionInviteCandidateView> getCandidates() { return candidates; }
    public String getSearchQuery() { return searchQuery; }
    public String getFeedbackMessage() { return feedbackMessage; }
    public String getErrorMessage() { return errorMessage; }
}
