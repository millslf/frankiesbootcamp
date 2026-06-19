package com.frankies.bootcamp.model;

import java.util.List;

public record CompetitionInsights(
        List<WeeklyLeaderboard> weeklyLeaderboards,
        List<RankTrend> rankTrends,
        List<SportStanding> sportStandings,
        List<AthleteProfileSummary> athleteProfiles,
        AthleteProfileSummary selectedAthleteProfile
) {
    public record WeeklyLeaderboard(
            int weekNumber,
            List<RankedAthleteMetric> entries
    ) {
    }

    public record SportStanding(
            String sportType,
            List<RankedAthleteMetric> entries
    ) {
    }

    public record RankedAthleteMetric(
            int rank,
            String athleteId,
            String athleteName,
            double sortValue,
            String displayValue
    ) {
    }

    public record RankTrend(
            String athleteId,
            String athleteName,
            List<RankPosition> positions
    ) {
    }

    public record RankPosition(
            int weekNumber,
            int rank
    ) {
    }

    public record AthleteProfileSummary(
            String athleteId,
            String athleteName,
            String profileMedium,
            int overallRank,
            String totalDistanceDisplay,
            double totalScore,
            int activeWeeks,
            int sickWeeks,
            int goalCrushWeeks,
            int bestWeekNumber,
            double bestWeekScore,
            String strongestSport,
            List<String> milestones
    ) {
    }
}
