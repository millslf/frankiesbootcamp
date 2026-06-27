package com.frankies.bootcamp.model;

public class CompetitionInviteCandidateView {
    private final String userId;
    private final String athleteId;
    private final String displayName;
    private final String email;
    private final String profileMedium;
    private final String city;
    private final String state;
    private final String country;
    private final boolean alreadyInCompetition;

    public CompetitionInviteCandidateView(String userId,
                                          String athleteId,
                                          String displayName,
                                          String email,
                                          String profileMedium,
                                          String city,
                                          String state,
                                          String country,
                                          boolean alreadyInCompetition) {
        this.userId = userId;
        this.athleteId = athleteId;
        this.displayName = displayName;
        this.email = email;
        this.profileMedium = profileMedium;
        this.city = city;
        this.state = state;
        this.country = country;
        this.alreadyInCompetition = alreadyInCompetition;
    }

    public String getUserId() { return userId; }
    public String getAthleteId() { return athleteId; }
    public String getDisplayName() { return displayName; }
    public String getEmail() { return email; }
    public String getProfileMedium() { return profileMedium; }
    public String getCity() { return city; }
    public String getState() { return state; }
    public String getCountry() { return country; }
    public boolean isAlreadyInCompetition() { return alreadyInCompetition; }
}
