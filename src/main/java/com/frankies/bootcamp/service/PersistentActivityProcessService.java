package com.frankies.bootcamp.service;

import com.frankies.bootcamp.constant.BootcampConstants;
import com.frankies.bootcamp.model.BootcampAthlete;
import com.frankies.bootcamp.model.PerformanceResponse;
import com.frankies.bootcamp.model.WeeklyPerformance;
import com.frankies.bootcamp.model.strava.StravaActivityResponse;
import com.frankies.bootcamp.sport.BaseSport;
import com.frankies.bootcamp.sport.DistanceSport;
import com.frankies.bootcamp.sport.DurationSport;
import com.frankies.bootcamp.sport.SportFactory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.wildfly.security.credential.store.CredentialStoreException;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class PersistentActivityProcessService {

    private static final Logger LOG = Logger.getLogger(PersistentActivityProcessService.class);

    private final DBService dbService;
    private final StravaService stravaService;
    private final DecimalFormat df = new DecimalFormat("#.##");

    private volatile List<PerformanceResponse> performanceList = new ArrayList<>();
    private volatile HashMap<Integer, HashMap<String, Double>> honourRollTotalDistance = new HashMap<>();
    private volatile HashMap<Integer, HashMap<String, Double>> honourRollPercentageOfGoal = new HashMap<>();
    private volatile Map<String, HashMap<String, Double>> sortedSummaries = new HashMap<>();

    @Inject
    public PersistentActivityProcessService(DBService dbService, StravaService stravaService) {
        this.dbService = dbService;
        this.stravaService = stravaService;
    }

    protected PersistentActivityProcessService() {
        this.dbService = null;
        this.stravaService = null;
    }

    public void prepareSummary() throws SQLException, CredentialStoreException, NoSuchAlgorithmException, IOException {
        List<BootcampAthlete> athletes = dbService.findAllAthletes();
        List<PerformanceResponse> rebuilt = new ArrayList<>();
        for (BootcampAthlete athlete : athletes) {
            if (athlete.getId() == null || athlete.getId().isBlank() || athlete.getId().startsWith("local-")) {
                continue;
            }
            rebuilt.add(rebuildAthleteState(athlete));
        }
        this.performanceList = rebuilt;
        persistHonourRollRows();
        regenerateSummaryMaps();
    }

    public void prepareAthleteSummary(BootcampAthlete athlete) throws SQLException, CredentialStoreException, NoSuchAlgorithmException, IOException {
        if (athlete == null || athlete.getId() == null || athlete.getId().isBlank() || athlete.getId().startsWith("local-")) {
            return;
        }
        PerformanceResponse rebuilt = rebuildAthleteState(athlete);
        List<PerformanceResponse> updated = new ArrayList<>(performanceList);
        updated.removeIf(existing -> existing.getAthlete() != null && athlete.getId().equals(existing.getAthlete().getId()));
        updated.add(rebuilt);
        this.performanceList = updated;
        persistHonourRollRows();
        regenerateSummaryMaps();
    }

    protected PerformanceResponse rebuildAthleteState(BootcampAthlete athlete) throws SQLException, CredentialStoreException, NoSuchAlgorithmException, IOException {
        BootcampAthlete refreshedAthlete = stravaService.refreshToken(athlete);
        LOG.info("Busy with athlete: " + refreshedAthlete.getFirstname() + " " + refreshedAthlete.getLastname());
        long competitionAthleteId = ensureCompetitionAthlete(refreshedAthlete);
        List<StravaActivityResponse> activities = stravaService.getAthleteActivitiesForPeriod(getStartTimeStamp(), refreshedAthlete.getAccessToken());

        PerformanceResponse performance = new PerformanceResponse();
        performance.setAthlete(refreshedAthlete);

        Map<String, PersistentSummarySportRowBuilder> summarySportBuilders = new LinkedHashMap<>();
        List<PersistentActivityDetailRow> activityRows = new ArrayList<>();
        List<PersistentWeeklyRow> weeklyRows = new ArrayList<>();

        double distanceToDate = 0.0;
        double scoreToDate = 0.0;
        int numberOfWeeksSinceStart = getNumberOfWeeksSinceStart();
        int week = 1;
        long weekEnding = getStartTimeStamp() + BootcampConstants.WEEK_IN_SECONDS;
        WeeklyPerformance weeklyPerformance = new WeeklyPerformance("Week" + week, weekEnding, refreshedAthlete.getGoal(), -1.0);

        for (StravaActivityResponse activity : activities) {
            int loopCount = 0;
            while (Instant.parse(activity.getStart_date()).getEpochSecond() > weekEnding) {
                weeklyPerformance.setAverageWeeklyScore(scoreToDate, week - 1);
                weeklyPerformance.setIsSick(refreshedAthlete.isSick(week));
                scoreToDate += weeklyPerformance.getWeekScore();
                performance.addWeeklyPerformance(weeklyPerformance, week);
                weeklyRows.add(buildWeeklyRow(week, weeklyPerformance, weekEnding));
                week++;
                weekEnding += BootcampConstants.WEEK_IN_SECONDS;
                weeklyPerformance = new WeeklyPerformance("Week" + week, weekEnding, weeklyPerformance.getWeekGoal(), loopCount == 0 ? weeklyPerformance.getTotalDistance() : 0.0);
                loopCount++;
            }

            BaseSport sport = SportFactory.getSport(activity);
            if (sport == null) {
                continue;
            }

            performance.addSport(activity.getId(), week, sport);
            weeklyPerformance.addSports(sport);
            distanceToDate += sport.getCalculatedDistance();
            activityRows.add(toActivityRow(week, activity.getId(), sport));
            summarySportBuilders.computeIfAbsent(sport.getSportType(), key -> new PersistentSummarySportRowBuilder(key)).add(sport);
        }

        int loopCount = 0;
        if (performance.getWeeklyPerformances() == null) {
            performance.addWeeklyPerformance(weeklyPerformance, week);
        }
        while (performance.getWeeklyPerformances().size() < numberOfWeeksSinceStart) {
            weeklyPerformance.setAverageWeeklyScore(scoreToDate, week - 1);
            weeklyPerformance.setIsSick(refreshedAthlete.isSick(week));
            scoreToDate += weeklyPerformance.getWeekScore();
            performance.addWeeklyPerformance(weeklyPerformance, week);
            weeklyRows.add(buildWeeklyRow(week, weeklyPerformance, weekEnding));
            week++;
            weekEnding += BootcampConstants.WEEK_IN_SECONDS;
            weeklyPerformance = new WeeklyPerformance("Week" + week, weekEnding, weeklyPerformance.getWeekGoal(), loopCount == 0 ? weeklyPerformance.getTotalDistance() : 0.0);
            loopCount++;
        }

        performance.setDistanceToDate(distanceToDate);
        performance.setScoreToDate(scoreToDate);

        WeeklyPerformance latestWeek = performance.getWeeklyPerformances().get(numberOfWeeksSinceStart);
        PersistentSummaryRow summaryRow = new PersistentSummaryRow(
                distanceToDate,
                scoreToDate,
                numberOfWeeksSinceStart,
                latestWeek != null ? latestWeek.getWeekGoal() : refreshedAthlete.getGoal(),
                Math.max(0, numberOfWeeksSinceStart - 1),
                refreshedAthlete.getGoal() == null ? 0.0 : refreshedAthlete.getGoal(),
                latestWeek != null ? latestWeek.getWeekScore() : 0.0,
                latestWeek != null ? latestWeek.getTotalPercentOfGoal() * 100 : 0.0
        );

        Map<String, PersistentSummarySportRow> summarySportRows = summarySportBuilders.values().stream()
                .map(PersistentSummarySportRowBuilder::build)
                .collect(Collectors.toMap(PersistentSummarySportRow::sportType, row -> row, (left, right) -> left, LinkedHashMap::new));

        replacePersistentCompetitionState(competitionAthleteId, activityRows, weeklyRows, summaryRow, summarySportRows);
        return performance;
    }

    protected long ensureCompetitionAthlete(BootcampAthlete athlete) throws SQLException {
        return dbService.ensureCompetitionAthlete(athlete.getId(), athlete.getGoal());
    }

    protected void replacePersistentCompetitionState(long competitionAthleteId,
                                                     List<PersistentActivityDetailRow> activityRows,
                                                     List<PersistentWeeklyRow> weeklyRows,
                                                     PersistentSummaryRow summaryRow,
                                                     Map<String, PersistentSummarySportRow> summarySportRows) throws SQLException {
        dbService.replacePersistentCompetitionState(competitionAthleteId, activityRows, weeklyRows, summaryRow, summarySportRows);
    }

    protected void replaceCompetitionHonourRoll(long competitionId,
                                                Map<Integer, PersistentHonourRollRow> honourRollRows) throws SQLException {
        dbService.replaceCompetitionHonourRoll(competitionId, honourRollRows);
    }

    private PersistentWeeklyRow buildWeeklyRow(int weekNumber, WeeklyPerformance weeklyPerformance, long weekEndingTimestamp) {
        LocalDate weekEnd = Instant.ofEpochSecond(weekEndingTimestamp).atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate weekStart = weekEnd.minusDays(6);
        Map<String, PersistentWeeklySportRow> sportRows = new LinkedHashMap<>();
        for (String sportType : weeklyPerformance.getSports().keySet()) {
            sportRows.put(sportType, new PersistentWeeklySportRow(
                    sportType,
                    weeklyPerformance.getSportsCount().getOrDefault(sportType, 0),
                    weeklyPerformance.getSports().getOrDefault(sportType, 0.0),
                    weeklyPerformance.getSportsOriginalDistance().get(sportType),
                    weeklyPerformance.getSportsOriginalDuration().get(sportType)
            ));
        }

        String activitiesSummaryText = sportRows.values().stream()
                .map(row -> {
                    StringBuilder line = new StringBuilder(row.sportType())
                            .append("(x")
                            .append(row.activityCount())
                            .append(") ")
                            .append(df.format(row.calculatedDistanceTotal()))
                            .append("km");
                    if (row.originalDistanceTotal() != null) {
                        line.append(" (").append(df.format(row.originalDistanceTotal())).append("km)");
                    } else if (row.originalDurationTotal() != null) {
                        line.append(" (").append(df.format(row.originalDurationTotal())).append("h)");
                    }
                    return line.toString();
                })
                .collect(Collectors.joining("\n"));

        return new PersistentWeeklyRow(
                weekNumber,
                weekStart,
                weekEnd,
                weeklyPerformance.getWeekGoal(),
                weeklyPerformance.getTotalDistance(),
                weeklyPerformance.getTotalPercentOfGoal(),
                Math.max(0.0, weeklyPerformance.getWeekGoal() - weeklyPerformance.getTotalDistance()),
                weeklyPerformance.getWeekGoalAchievementScore(),
                weeklyPerformance.getWeekProgressionBonus(),
                weeklyPerformance.getWeekScore(),
                weeklyPerformance.isSick(),
                weeklyPerformance.getWeekScore() == null ? 0.0 : weeklyPerformance.getWeekScore(),
                activitiesSummaryText,
                sportRows
        );
    }

    private PersistentActivityDetailRow toActivityRow(int weekNumber, Long activityId, BaseSport sport) {
        Double originalDistance = sport instanceof DistanceSport distanceSport ? distanceSport.getOriginalDistance() : null;
        Double originalDuration = sport instanceof DurationSport durationSport ? durationSport.getOriginalDuration() : null;
        return new PersistentActivityDetailRow(
                weekNumber,
                activityId,
                sport.getSportType(),
                originalDistance,
                originalDuration,
                sport.getCalculatedDistance()
        );
    }

    public List<PerformanceResponse> getPerformanceList() {
        return performanceList;
    }

    public Map<Integer, WeeklyPerformance> getAthleteHistory(String athleteId) {
        try {
            return dbService.getPersistentAthleteHistory(athleteId);
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to load persistent athlete history", e);
        }
    }

    public HashMap<Integer, HashMap<String, Double>> getHonourRollTotalDistance() {
        try {
            return dbService.getPersistentHonourRollTotalDistance(1L);
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to load persistent honour roll distance results", e);
        }
    }

    public HashMap<Integer, HashMap<String, Double>> getHonourRollPercentageOfGoal() {
        try {
            return dbService.getPersistentHonourRollPercentageOfGoal(1L);
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to load persistent honour roll goal results", e);
        }
    }

    public String getLoggedInAthleteSummary(String athleteId)
            throws IOException, CredentialStoreException, NoSuchAlgorithmException, SQLException {
        DBService.PersistentAthleteSummarySnapshot snapshot = dbService.getPersistentAthleteSummarySnapshot(athleteId);
        if (snapshot == null) {
            return "";
        }

        Map<Integer, WeeklyPerformance> history = dbService.getPersistentAthleteHistory(athleteId);
        WeeklyPerformance currentWeek = history.get(getNumberOfWeeksSinceStart());

        StringBuilder sports = new StringBuilder();
        if (currentWeek != null && currentWeek.getSports() != null) {
            for (Map.Entry<String, Double> entry : currentWeek.getSports().entrySet()) {
                sports.append(entry.getKey()).append(" ").append(df.format(entry.getValue())).append("km\n");
            }
        }

        return "Liewe " + snapshot.athleteFirstName() + ",\n\n" +
                "Distance this challenge: " + df.format(snapshot.distanceToDate()) + "km\n" +
                "Total points: " + df.format(snapshot.scoreToDate()) + "\n" +
                "Sports: \n" + sports +
                "Original weekly goal: " + df.format(snapshot.originalWeeklyGoal()) + "km\n\n" +
                (currentWeek == null ? "" : currentWeek.toString()) +
                "OVERALL SCORE COMPETITION:\n" +
                getScoreSummary(getSortedSummaries().get(BootcampConstants.currentYearlyScoreSummary));
    }

    private String buildSummaryBody(PerformanceResponse performance) {
        WeeklyPerformance currentWeek = performance.getWeeklyPerformances().get(getNumberOfWeeksSinceStart());
        StringBuilder sports = new StringBuilder();
        for (Map.Entry<String, Double> entry : performance.getSports().entrySet()) {
            sports.append(entry.getKey()).append(" ").append(df.format(entry.getValue())).append("km\n");
        }
        return "Liewe " + performance.getAthlete().getFirstname() + ",\n\n" +
                "Distance this challenge: " + df.format(performance.getDistanceToDate()) + "km\n" +
                "Total points: " + df.format(performance.getScoreToDate()) + "\n" +
                "Sports: \n" + sports +
                "Original weekly goal: " + df.format(performance.getAthlete().getGoal()) + "km\n\n" +
                (currentWeek == null ? "" : currentWeek.toString()) +
                "OVERALL SCORE COMPETITION:\n" +
                getScoreSummary(sortedSummaries.get(BootcampConstants.currentYearlyScoreSummary));
    }

    private String getScoreSummary(HashMap<String, Double> currentWeekSummary) {
        if (currentWeekSummary == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Double> entry : currentWeekSummary.entrySet()) {
            sb.append(entry.getKey()).append(": ").append(df.format(entry.getValue())).append("\n");
        }
        return sb.toString();
    }

    public int getNumberOfWeeksSinceStart() {
        return (int) Math.round(Math.ceil((double) (System.currentTimeMillis() - (getStartTimeStamp() * 1000)) /
                (BootcampConstants.WEEK_IN_SECONDS * 1000)));
    }

    public Map<String, HashMap<String, Double>> getSortedSummaries() {
        try {
            return dbService.getPersistentLeaderboardSummaries(1L);
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to load persistent leaderboard summaries", e);
        }
    }

    public String getZenBotStatsContext(String athleteId) {
        try {
            DBService.PersistentAthleteSummarySnapshot snapshot = dbService.getPersistentAthleteSummarySnapshot(athleteId);
            if (snapshot == null) {
                return "No athlete stats are currently available.";
            }

            WeeklyPerformance currentWeek = dbService.getPersistentAthleteHistory(athleteId).get(getNumberOfWeeksSinceStart());
            if (currentWeek == null) {
                return "No athlete stats are currently available.";
            }

            return "Athlete " + snapshot.athleteFirstName() + " has " + df.format(snapshot.distanceToDate()) + "km total, score " + df.format(snapshot.scoreToDate()) + ", and current week progress " + df.format(currentWeek.getTotalPercentOfGoal() * 100) + "% of goal.";
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to load persistent ZenBot stats context", e);
        }
    }

    public boolean addActivityEvent(String athleteId, StravaActivityResponse activity) {
        try {
            BootcampAthlete athlete = dbService.findAthleteByStravaID(athleteId);
            if (athlete == null) {
                return false;
            }
            prepareAthleteSummary(athlete);
            return true;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to rebuild persistent athlete state after webhook create", e);
        }
    }

    public boolean removeActivityEvent(String athleteId, Long activityId) {
        try {
            BootcampAthlete athlete = dbService.findAthleteByStravaID(athleteId);
            if (athlete == null) {
                return false;
            }
            prepareAthleteSummary(athlete);
            return true;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to rebuild persistent athlete state after webhook delete/update", e);
        }
    }

    public void run() {
        try {
            prepareSummary();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to build persistent summary state", e);
        }
    }

    private void regenerateSummaryMaps() {
        HashMap<String, Double> currentWeekPercentageOfGoalSummary = new HashMap<>();
        HashMap<String, Double> currentYearlyScoreSummary = new HashMap<>();

        int currentWeek = getNumberOfWeeksSinceStart();
        for (PerformanceResponse performance : performanceList) {
            WeeklyPerformance week = performance.getWeeklyPerformances().get(currentWeek);
            if (week != null) {
                currentWeekPercentageOfGoalSummary.put(performance.getAthlete().getFirstname(), week.getTotalPercentOfGoal() * 100);
            }
            currentYearlyScoreSummary.put(performance.getAthlete().getFirstname(), performance.getScoreToDate());
        }

        Map<String, HashMap<String, Double>> rebuilt = new HashMap<>();
        rebuilt.put(BootcampConstants.currentWeekPercentageOfGoalSummary, sortByValue(currentWeekPercentageOfGoalSummary));
        rebuilt.put(BootcampConstants.currentYearlyScoreSummary, sortByValue(currentYearlyScoreSummary));
        this.sortedSummaries = rebuilt;
        this.honourRollTotalDistance = buildHonourRollMap(true);
        this.honourRollPercentageOfGoal = buildHonourRollMap(false);
    }


    private void persistHonourRollRows() throws SQLException {
        Map<Integer, PersistentHonourRollRow> honourRollRows = new LinkedHashMap<>();
        HashMap<Integer, HashMap<String, Double>> distanceMap = buildHonourRollMap(true);
        HashMap<Integer, HashMap<String, Double>> percentMap = buildHonourRollMap(false);

        for (Map.Entry<Integer, HashMap<String, Double>> distanceEntry : distanceMap.entrySet()) {
            Map.Entry<String, Double> distanceWinner = distanceEntry.getValue().entrySet().iterator().next();
            Map.Entry<String, Double> percentWinner = percentMap.getOrDefault(distanceEntry.getKey(), new HashMap<>()).entrySet().stream().findFirst()
                    .orElse(Map.entry("", 0.0));
            honourRollRows.put(distanceEntry.getKey(), new PersistentHonourRollRow(
                    distanceEntry.getKey(),
                    distanceWinner.getKey(),
                    distanceWinner.getValue(),
                    percentWinner.getKey(),
                    percentWinner.getValue()
            ));
        }

        replaceCompetitionHonourRoll(1L, honourRollRows);
    }

    private HashMap<Integer, HashMap<String, Double>> buildHonourRollMap(boolean distance) {
        HashMap<Integer, HashMap<String, Double>> results = new HashMap<>();
        for (PerformanceResponse performance : performanceList) {
            for (Map.Entry<Integer, WeeklyPerformance> weeklyEntry : performance.getWeeklyPerformances().entrySet()) {
                WeeklyPerformance week = weeklyEntry.getValue();
                double value = distance ? week.getTotalDistance() : week.getTotalPercentOfGoal();
                results.computeIfAbsent(weeklyEntry.getKey(), ignored -> new HashMap<>());
                HashMap<String, Double> existing = results.get(weeklyEntry.getKey());
                if (existing.isEmpty() || value > existing.values().iterator().next()) {
                    existing.clear();
                    existing.put(performance.getAthlete().getFirstname() + " " + performance.getAthlete().getLastname(), value);
                }
            }
        }
        return results;
    }

    private HashMap<String, Double> sortByValue(HashMap<String, Double> map) {
        return ActivityProcessService.entriesSortedByValues(map).stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (left, right) -> left, LinkedHashMap::new));
    }

    public long getStartTimeStamp() {
        return BootcampConstants.START_TIMESTAMP;
    }

    public record PersistentActivityDetailRow(int weekNumber,
                                              long stravaActivityId,
                                              String sportType,
                                              Double originalDistance,
                                              Double originalDuration,
                                              double calculatedDistance) {
    }

    public record PersistentWeeklySportRow(String sportType,
                                           int activityCount,
                                           double calculatedDistanceTotal,
                                           Double originalDistanceTotal,
                                           Double originalDurationTotal) {
    }

    public record PersistentWeeklyRow(int weekNumber,
                                      LocalDate weekStartDate,
                                      LocalDate weekEndDate,
                                      double weekGoal,
                                      double totalDistance,
                                      double totalPercentOfGoal,
                                      double distanceLeft,
                                      double weekGoalAchievementScore,
                                      double weekProgressionBonus,
                                      double weekScore,
                                      boolean isSick,
                                      double averageWeeklyScore,
                                      String activitiesSummaryText,
                                      Map<String, PersistentWeeklySportRow> sportRows) {
    }

    public record PersistentSummaryRow(double distanceToDate,
                                       double scoreToDate,
                                       int currentWeek,
                                       double currentWeekGoal,
                                       int lastCompletedWeek,
                                       double originalWeeklyGoal,
                                       double latestWeekScore,
                                       double latestWeekPercentOfGoal) {
    }

    public record PersistentSummarySportRow(String sportType,
                                            int activityCount,
                                            double calculatedDistanceTotal,
                                            Double originalDistanceTotal,
                                            Double originalDurationTotal) {
    }

    public record PersistentHonourRollRow(int weekNumber,
                                          String distanceWinnerName,
                                          double distanceWinnerValue,
                                          String percentWinnerName,
                                          double percentWinnerValue) {
    }

    private static final class PersistentSummarySportRowBuilder {
        private final String sportType;
        private int activityCount;
        private double calculatedDistanceTotal;
        private Double originalDistanceTotal;
        private Double originalDurationTotal;

        private PersistentSummarySportRowBuilder(String sportType) {
            this.sportType = sportType;
        }

        private void add(BaseSport sport) {
            activityCount++;
            calculatedDistanceTotal += sport.getCalculatedDistance();
            if (sport instanceof DistanceSport distanceSport) {
                originalDistanceTotal = (originalDistanceTotal == null ? 0.0 : originalDistanceTotal) + distanceSport.getOriginalDistance();
            }
            if (sport instanceof DurationSport durationSport) {
                originalDurationTotal = (originalDurationTotal == null ? 0.0 : originalDurationTotal) + durationSport.getOriginalDuration();
            }
        }

        private PersistentSummarySportRow build() {
            return new PersistentSummarySportRow(sportType, activityCount, calculatedDistanceTotal, originalDistanceTotal, originalDurationTotal);
        }
    }
}
