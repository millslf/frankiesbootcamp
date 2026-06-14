package com.frankies.bootcamp.model;

import java.util.List;

public class CompetitionSetupView {
    private final String athleteId;
    private final String athleteDisplayName;
    private final Double suggestedStartingGoal;
    private final List<CompetitionSummaryView> activeCompetitions;

    public CompetitionSetupView(String athleteId,
                                String athleteDisplayName,
                                Double suggestedStartingGoal,
                                List<CompetitionSummaryView> activeCompetitions) {
        this.athleteId = athleteId;
        this.athleteDisplayName = athleteDisplayName;
        this.suggestedStartingGoal = suggestedStartingGoal;
        this.activeCompetitions = activeCompetitions;
    }

    public String getAthleteId() {
        return athleteId;
    }

    public String getAthleteDisplayName() {
        return athleteDisplayName;
    }

    public Double getSuggestedStartingGoal() {
        return suggestedStartingGoal;
    }

    public List<CompetitionSummaryView> getActiveCompetitions() {
        return activeCompetitions;
    }
}
