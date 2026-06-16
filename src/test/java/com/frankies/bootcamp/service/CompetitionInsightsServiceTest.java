package com.frankies.bootcamp.service;

import com.frankies.bootcamp.model.BootcampAthlete;
import com.frankies.bootcamp.model.CompetitionInsights;
import com.frankies.bootcamp.model.PerformanceResponse;
import com.frankies.bootcamp.model.WeeklyPerformance;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompetitionInsightsServiceTest {

    private final CompetitionInsightsService service = new CompetitionInsightsService();

    @Test
    void derivesInsightsFromExistingPerformanceData() {
        CompetitionInsights insights = service.buildInsights(List.of(
                performance("athlete-1", "Alex", 30.0, 3.2, List.of(
                        week(1, 10.0, 1.0, false, sport("Run", 10.0)),
                        week(2, 12.0, 1.2, false, sport("Ride", 12.0)),
                        week(3, 8.0, 1.6, false, sport("Run", 8.0), sport("Walk", 2.0), sport("Ride", 3.0))
                )),
                performance("athlete-2", "Blake", 24.0, 2.7, List.of(
                        week(1, 15.0, 1.5, false, sport("Ride", 15.0)),
                        week(2, 4.0, 0.4, true, sport("Walk", 4.0)),
                        week(3, 9.0, 0.9, false, sport("Ride", 9.0))
                ))
        ), "athlete-1", true);

        assertEquals(2, insights.weeklyLeaderboards().size());
        assertEquals("Blake", insights.weeklyLeaderboards().get(0).entries().get(0).athleteName());
        assertEquals("Alex", insights.weeklyLeaderboards().get(1).entries().get(0).athleteName());

        CompetitionInsights.RankTrend alexTrend = insights.rankTrends().stream()
                .filter(trend -> trend.athleteName().equals("Alex"))
                .findFirst()
                .orElseThrow();
        assertEquals(List.of(2, 1), alexTrend.positions().stream()
                .map(CompetitionInsights.RankPosition::rank)
                .toList());
        assertEquals("Ride", insights.sportStandings().get(0).sportType());
        assertEquals("Blake", insights.sportStandings().get(0).entries().get(0).athleteName());
        assertEquals(0.0, insights.sportStandings().get(0).entries().get(0).sortValue());
        assertEquals("", insights.sportStandings().get(0).entries().get(0).displayValue());

        CompetitionInsights.AthleteProfileSummary profile = insights.selectedAthleteProfile();
        assertEquals("Alex", profile.athleteName());
        assertEquals(1, profile.overallRank());
        assertEquals("", profile.totalDistanceDisplay());
        assertEquals(2, profile.goalCrushWeeks());
        assertEquals(2, profile.activeWeeks());
        assertEquals("Ride", profile.strongestSport());
        assertTrue(profile.milestones().contains("Active every completed week"));

        CompetitionInsights otherAthleteInsights = service.buildInsights(List.of(
                performance("athlete-1", "Alex", 30.0, 3.2, List.of(
                        week(1, 10.0, 1.0, false, sport("Run", 10.0))
                )),
                performance("athlete-2", "Blake", 24.0, 2.7, List.of(
                        week(1, 15.0, 1.5, false, sport("Ride", 15.0))
                ))
        ), "athlete-2", false);
        assertEquals("Blake", otherAthleteInsights.selectedAthleteProfile().athleteName());
    }

    @Test
    void rankTrendShowsSharedRankWhenLeaderboardPointsAreTied() {
        CompetitionInsights insights = service.buildInsights(List.of(
                performance("athlete-donovan", "Donovan", 19.0, 1.25, List.of(
                        week(1, 19.0, 1.25, false, sport("Run", 19.0))
                )),
                performance("athlete-candice", "Candice", 17.0, 1.25, List.of(
                        week(1, 17.0, 1.25, false, sport("Run", 17.0))
                ))
        ), "athlete-candice", false);

        CompetitionInsights.RankTrend candice = insights.rankTrends().stream()
                .filter(trend -> trend.athleteName().equals("Candice"))
                .findFirst()
                .orElseThrow();

        CompetitionInsights.RankTrend donovan = insights.rankTrends().stream()
                .filter(trend -> trend.athleteName().equals("Donovan"))
                .findFirst()
                .orElseThrow();

        assertEquals(1, candice.positions().get(0).rank());
        assertEquals(1, donovan.positions().get(0).rank());
    }

    private static PerformanceResponse performance(String athleteId,
                                                   String firstName,
                                                   double distanceToDate,
                                                   double scoreToDate,
                                                   List<WeeklyPerformance> weeks) {
        BootcampAthlete athlete = new BootcampAthlete();
        athlete.setId(athleteId);
        athlete.setFirstname(firstName);

        PerformanceResponse performance = new PerformanceResponse();
        performance.setAthlete(athlete);
        performance.setDistanceToDate(distanceToDate);
        performance.setScoreToDate(scoreToDate);
        for (int i = 0; i < weeks.size(); i++) {
            performance.addWeeklyPerformance(weeks.get(i), i + 1);
        }
        return performance;
    }

    private static WeeklyPerformance week(int weekNumber,
                                          double totalDistance,
                                          double weekScore,
                                          boolean sick,
                                          SportTotal... sports) {
        WeeklyPerformance week = new WeeklyPerformance("Week" + weekNumber, 0L, 10.0, -1.0);
        week.setPersistedValues(totalDistance, totalDistance / 10.0, weekScore, 0.0, weekScore, weekScore, sick);
        for (SportTotal sport : sports) {
            week.setPersistedSportTotals(sport.sportType(), 1, sport.distance(), null, null);
        }
        return week;
    }

    private static SportTotal sport(String sportType, double distance) {
        return new SportTotal(sportType, distance);
    }

    private record SportTotal(String sportType, double distance) {
    }
}
