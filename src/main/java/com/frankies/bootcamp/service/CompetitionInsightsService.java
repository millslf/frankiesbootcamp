package com.frankies.bootcamp.service;

import com.frankies.bootcamp.model.BootcampAthlete;
import com.frankies.bootcamp.model.CompetitionInsights;
import com.frankies.bootcamp.model.PerformanceResponse;
import com.frankies.bootcamp.model.WeeklyPerformance;
import jakarta.inject.Inject;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@ApplicationScoped
public class CompetitionInsightsService {
    @Inject
    private DBService dbService;

    public CompetitionInsights buildInsights(List<PerformanceResponse> performances, String selectedAthleteId) {
        return buildInsights(performances, selectedAthleteId, false);
    }

    public CompetitionInsights buildInsights(List<PerformanceResponse> performances,
                                             String selectedAthleteId,
                                             boolean excludeLatestWeek) {
        List<PerformanceResponse> safePerformances = performances == null ? List.of() : performances;
        int latestWeek = latestWeek(safePerformances);
        int insightWeek = excludeLatestWeek && latestWeek > 1 ? latestWeek - 1 : latestWeek;
        Map<String, Integer> overallRanks = ranksByScore(scoreThroughWeekByAthlete(safePerformances, insightWeek));

        List<CompetitionInsights.AthleteProfileSummary> profiles = safePerformances.stream()
                .map(performance -> profile(performance, insightWeek, overallRanks.getOrDefault(athleteId(performance), 0)))
                .sorted(Comparator.comparingInt(CompetitionInsights.AthleteProfileSummary::overallRank))
                .toList();

        CompetitionInsights.AthleteProfileSummary selectedProfile = profiles.stream()
                .filter(profile -> Objects.equals(profile.athleteId(), selectedAthleteId))
                .findFirst()
                .orElse(profiles.isEmpty() ? null : profiles.get(0));

        return new CompetitionInsights(
                weeklyLeaderboards(safePerformances, insightWeek),
                rankTrends(safePerformances, insightWeek),
                sportStandings(safePerformances, insightWeek),
                profiles,
                selectedProfile
        );
    }

    private List<CompetitionInsights.WeeklyLeaderboard> weeklyLeaderboards(List<PerformanceResponse> performances,
                                                                           int latestWeek) {
        List<CompetitionInsights.WeeklyLeaderboard> leaderboards = new ArrayList<>();
        for (int weekNumber = 1; weekNumber <= latestWeek; weekNumber++) {
            int currentWeek = weekNumber;
            List<CompetitionInsights.RankedAthleteMetric> entries = rankedMetrics(
                    performances.stream()
                            .map(performance -> metric(performance, weekScore(performance, currentWeek)))
                            .toList(),
                    true
            );
            leaderboards.add(new CompetitionInsights.WeeklyLeaderboard(weekNumber, entries));
        }
        return leaderboards;
    }

    private List<CompetitionInsights.RankTrend> rankTrends(List<PerformanceResponse> performances, int latestWeek) {
        Map<String, List<CompetitionInsights.RankPosition>> positionsByAthlete = new LinkedHashMap<>();
        for (PerformanceResponse performance : performances) {
            positionsByAthlete.put(athleteId(performance), new ArrayList<>());
        }

        for (int weekNumber = 1; weekNumber <= latestWeek; weekNumber++) {
            Map<String, Integer> weeklyRanks = ranksByScore(scoreThroughWeekByAthlete(performances, weekNumber));
            for (PerformanceResponse performance : performances) {
                String athleteId = athleteId(performance);
                positionsByAthlete.get(athleteId).add(new CompetitionInsights.RankPosition(
                        weekNumber,
                        weeklyRanks.getOrDefault(athleteId, performances.size())
                ));
            }
        }

        return performances.stream()
                .map(performance -> new CompetitionInsights.RankTrend(
                        athleteId(performance),
                        athleteName(performance),
                        positionsByAthlete.getOrDefault(athleteId(performance), List.of())
                ))
                .sorted(Comparator.comparingInt(trend -> trend.positions().isEmpty()
                        ? Integer.MAX_VALUE
                        : trend.positions().get(trend.positions().size() - 1).rank()))
                .toList();
    }

    private List<CompetitionInsights.SportStanding> sportStandings(List<PerformanceResponse> performances,
                                                                   int latestInsightWeek) {
        Map<String, Map<PerformanceResponse, Double>> totalsBySport = new HashMap<>();
        for (PerformanceResponse performance : performances) {
            for (Map.Entry<String, Double> sportTotal : sportTotals(weeklyPerformancesThrough(performance, latestInsightWeek)).entrySet()) {
                totalsBySport.computeIfAbsent(sportTotal.getKey(), key -> new LinkedHashMap<>())
                        .put(performance, sportTotal.getValue());
            }
        }

        return totalsBySport.entrySet().stream()
                .sorted(Comparator.<Map.Entry<String, Map<PerformanceResponse, Double>>>comparingDouble(
                        entry -> entry.getValue().values().stream().mapToDouble(Double::doubleValue).sum()
                ).reversed().thenComparing(Map.Entry::getKey))
                .map(entry -> new CompetitionInsights.SportStanding(
                        entry.getKey(),
                        rankedMetrics(entry.getValue().entrySet().stream()
                                .map(total -> metric(total.getKey(), total.getValue()))
                                .toList(), false)
                ))
                .toList();
    }

    private CompetitionInsights.AthleteProfileSummary profile(PerformanceResponse performance,
                                                              int latestInsightWeek,
                                                              int overallRank) {
        Map<Integer, WeeklyPerformance> allWeeks = weeklyPerformances(performance);
        Map<Integer, WeeklyPerformance> weeks = weeklyPerformancesThrough(performance, latestInsightWeek);
        Map<String, Double> sports = sportTotals(weeks);
        int currentWeek = latestWeek(allWeeks);
        WeeklyPerformance currentWeekPerformance = currentWeek == 0 ? null : allWeeks.get(currentWeek);

        int activeWeeks = (int) weeks.values().stream()
                .filter(week -> week.getTotalDistance() > 0.0 || week.getWeekScore() > 0.0 || week.isSick())
                .count();
        int sickWeeks = (int) weeks.values().stream().filter(WeeklyPerformance::isSick).count();
        int goalCrushWeeks = (int) weeks.values().stream()
                .filter(week -> week.getTotalPercentOfGoal() >= 1.0)
                .count();
        Map.Entry<Integer, WeeklyPerformance> bestWeek = weeks.entrySet().stream()
                .max(Comparator.comparingDouble(entry -> entry.getValue().getWeekScore()))
                .orElse(null);
        String strongestSport = sports.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("None yet");
        double totalScoreToDate = allWeeks.values().stream().mapToDouble(WeeklyPerformance::getWeekScore).sum();

        return new CompetitionInsights.AthleteProfileSummary(
                athleteId(performance),
                athleteName(performance),
                profileMedium(performance),
                overallRank,
                "",
                totalScoreToDate,
                currentWeekPerformance == null ? 0.0 : currentWeekPerformance.getTotalPercentOfGoal(),
                activeWeeks,
                sickWeeks,
                goalCrushWeeks,
                bestWeek == null ? 0 : bestWeek.getKey(),
                bestWeek == null ? 0.0 : bestWeek.getValue().getWeekScore(),
                strongestSport,
                milestones(weeks, activeWeeks, goalCrushWeeks, bestWeek)
        );
    }

    private List<String> milestones(Map<Integer, WeeklyPerformance> weeks,
                                    int activeWeeks,
                                    int goalCrushWeeks,
                                    Map.Entry<Integer, WeeklyPerformance> bestWeek) {
        List<String> milestones = new ArrayList<>();
        if (bestWeek != null) {
            milestones.add("Best week: Week " + bestWeek.getKey());
        }
        if (goalCrushWeeks > 0) {
            milestones.add("Goal crusher weeks: " + goalCrushWeeks);
        }
        if (activeWeeks > 0 && activeWeeks == weeks.size()) {
            milestones.add("Active every completed week");
        }
        weeks.entrySet().stream()
                .filter(entry -> entry.getValue().getSports().size() >= 3)
                .findFirst()
                .ifPresent(entry -> milestones.add("Multi-sport week in Week " + entry.getKey()));
        return milestones;
    }

    private Map<String, Double> scoreThroughWeekByAthlete(List<PerformanceResponse> performances, int weekNumber) {
        Map<String, Double> scores = new HashMap<>();
        for (PerformanceResponse performance : performances) {
            double total = weeklyPerformances(performance).entrySet().stream()
                    .filter(entry -> entry.getKey() <= weekNumber)
                    .mapToDouble(entry -> entry.getValue().getWeekScore())
                    .sum();
            scores.put(athleteId(performance), total);
        }
        return scores;
    }

    private Map<String, Integer> ranksByScore(Map<String, Double> scores) {
        Map<String, Integer> ranks = new HashMap<>();
        List<Map.Entry<String, Double>> sorted = scores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed()
                        .thenComparing(Map.Entry::getKey))
                .toList();
        Double previousScore = null;
        int previousRank = 0;
        for (int i = 0; i < sorted.size(); i++) {
            Map.Entry<String, Double> entry = sorted.get(i);
            int rank = previousScore != null && Double.compare(previousScore, entry.getValue()) == 0
                    ? previousRank
                    : i + 1;
            ranks.put(entry.getKey(), rank);
            previousScore = entry.getValue();
            previousRank = rank;
        }
        return ranks;
    }

    private List<CompetitionInsights.RankedAthleteMetric> rankedMetrics(List<CompetitionInsights.RankedAthleteMetric> metrics,
                                                                        boolean exposeSortValue) {
        List<CompetitionInsights.RankedAthleteMetric> sorted = metrics.stream()
                .sorted(Comparator.comparingDouble(CompetitionInsights.RankedAthleteMetric::sortValue).reversed()
                        .thenComparing(CompetitionInsights.RankedAthleteMetric::athleteName))
                .toList();
        List<CompetitionInsights.RankedAthleteMetric> ranked = new ArrayList<>();
        Double previousValue = null;
        int previousRank = 0;
        for (int i = 0; i < sorted.size(); i++) {
            CompetitionInsights.RankedAthleteMetric metric = sorted.get(i);
            int rank = previousValue != null && Double.compare(previousValue, metric.sortValue()) == 0
                    ? previousRank
                    : i + 1;
            ranked.add(new CompetitionInsights.RankedAthleteMetric(
                    rank,
                    metric.athleteId(),
                    metric.athleteName(),
                    exposeSortValue ? metric.sortValue() : 0.0,
                    exposeSortValue ? metric.displayValue() : ""
            ));
            previousValue = metric.sortValue();
            previousRank = rank;
        }
        return ranked;
    }

    private CompetitionInsights.RankedAthleteMetric metric(PerformanceResponse performance, double value) {
        return new CompetitionInsights.RankedAthleteMetric(0, athleteId(performance), athleteName(performance), value, "");
    }

    private int latestWeek(List<PerformanceResponse> performances) {
        return performances.stream()
                .flatMap(performance -> weeklyPerformances(performance).keySet().stream())
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0);
    }

    private int latestWeek(Map<Integer, WeeklyPerformance> weeks) {
        return weeks.keySet().stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0);
    }

    private double weekScore(PerformanceResponse performance, int weekNumber) {
        WeeklyPerformance week = weeklyPerformances(performance).get(weekNumber);
        return week == null ? 0.0 : value(week.getWeekScore());
    }

    private Map<Integer, WeeklyPerformance> weeklyPerformances(PerformanceResponse performance) {
        return performance.getWeeklyPerformances() == null ? Map.of() : performance.getWeeklyPerformances();
    }

    private Map<Integer, WeeklyPerformance> weeklyPerformancesThrough(PerformanceResponse performance, int latestInsightWeek) {
        Map<Integer, WeeklyPerformance> weeks = new LinkedHashMap<>();
        weeklyPerformances(performance).entrySet().stream()
                .filter(entry -> entry.getKey() <= latestInsightWeek)
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> weeks.put(entry.getKey(), entry.getValue()));
        return weeks;
    }

    private Map<String, Double> sportTotals(Map<Integer, WeeklyPerformance> weeks) {
        Map<String, Double> totals = new HashMap<>();
        for (WeeklyPerformance week : weeks.values()) {
            for (Map.Entry<String, Double> entry : week.getSports().entrySet()) {
                totals.merge(entry.getKey(), value(entry.getValue()), Double::sum);
            }
        }
        return totals;
    }

    private String athleteId(PerformanceResponse performance) {
        BootcampAthlete athlete = performance.getAthlete();
        return athlete == null || athlete.getId() == null ? "" : athlete.getId();
    }

    private String athleteName(PerformanceResponse performance) {
        BootcampAthlete athlete = performance.getAthlete();
        if (athlete == null) {
            return "Athlete";
        }
        String name = ((athlete.getFirstname() == null ? "" : athlete.getFirstname()) + " "
                + (athlete.getLastname() == null ? "" : athlete.getLastname())).trim();
        return name.isBlank() ? "Athlete" : name;
    }

    private String profileMedium(PerformanceResponse performance) {
        BootcampAthlete athlete = performance.getAthlete();
        String storedProfileMedium = storedProfileMedium(athlete);
        if (storedProfileMedium != null) {
            return storedProfileMedium;
        }
        if (athlete != null && athlete.getProfileMedium() != null && !athlete.getProfileMedium().isBlank()) {
            return athlete.getProfileMedium();
        }
        return null;
    }

    private String storedProfileMedium(BootcampAthlete athlete) {
        if (athlete == null || athlete.getId() == null || athlete.getId().isBlank() || dbService == null) {
            return null;
        }
        try {
            BootcampAthlete storedAthlete = dbService.findAthleteByStravaID(athlete.getId());
            if (storedAthlete != null && storedAthlete.getProfileMedium() != null && !storedAthlete.getProfileMedium().isBlank()) {
                return storedAthlete.getProfileMedium();
            }
        } catch (Exception ignored) {
            // fall through to no avatar
        }
        return null;
    }

    private double value(Double value) {
        return value == null ? 0.0 : value;
    }
}
