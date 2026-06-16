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
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class PersistentActivityProcessService {

    private static final Logger LOG = Logger.getLogger(PersistentActivityProcessService.class);

    private final DBService dbService;
    private final StravaService stravaService;
    private final DecimalFormat df = new DecimalFormat("#.##");

    private volatile HashMap<Integer, HashMap<String, Double>> honourRollTotalDistance = new HashMap<>();
    private volatile HashMap<Integer, HashMap<String, Double>> honourRollPercentageOfGoal = new HashMap<>();

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
        for (BootcampAthlete athlete : athletes) {
            if (athlete.getId() == null || athlete.getId().isBlank() || athlete.getId().startsWith("local-")) {
                continue;
            }
            if (!hasActiveCompetitionMembership(athlete.getId())) {
                continue;
            }
            rebuildCurrentCompetitionStates(athlete);
        }
        persistHonourRollRows();
        regenerateSummaryMaps();
    }

    public void prepareAthleteSummary(BootcampAthlete athlete) throws SQLException, CredentialStoreException, NoSuchAlgorithmException, IOException {
        if (athlete == null || athlete.getId() == null || athlete.getId().isBlank() || athlete.getId().startsWith("local-")) {
            return;
        }
        if (!hasActiveCompetitionMembership(athlete.getId())) {
            return;
        }
        rebuildCurrentCompetitionStates(athlete);
        persistHonourRollRows();
        regenerateSummaryMaps();
    }

    public void prepareAthleteSummaryForCompetition(BootcampAthlete athlete, long competitionId) throws SQLException, CredentialStoreException, NoSuchAlgorithmException, IOException {
        if (athlete == null || athlete.getId() == null || athlete.getId().isBlank() || athlete.getId().startsWith("local-")) {
            return;
        }

        Long competitionAthleteId = dbService.findCompetitionAthleteId(athlete.getId(), competitionId);
        if (competitionAthleteId == null) {
            return;
        }

        rebuildAthleteStateForCompetition(athlete, competitionAthleteId);
        persistCompetitionHonourRollRows(competitionId);
        regenerateSummaryMaps();
    }

    public void prepareCompetitionSummary(long competitionId) throws SQLException, CredentialStoreException, NoSuchAlgorithmException, IOException {
        List<BootcampAthlete> athletes = dbService.listCompetitionAthletes(competitionId);
        for (BootcampAthlete athlete : athletes) {
            if (athlete.getId() == null || athlete.getId().isBlank() || athlete.getId().startsWith("local-")) {
                continue;
            }
            Long competitionAthleteId = dbService.findCompetitionAthleteId(athlete.getId(), competitionId);
            if (competitionAthleteId == null) {
                continue;
            }
            rebuildAthleteStateForCompetition(athlete, competitionAthleteId);
        }
        persistCompetitionHonourRollRows(competitionId);
        regenerateSummaryMaps();
    }

    protected PerformanceResponse rebuildAthleteState(BootcampAthlete athlete) throws SQLException, CredentialStoreException, NoSuchAlgorithmException, IOException {
        BootcampAthlete refreshedAthlete = stravaService.refreshToken(athlete);
        LOG.info("Busy with athlete: " + refreshedAthlete.getFirstname() + " " + refreshedAthlete.getLastname());
        Long competitionAthleteId = getActiveCompetitionAthleteId(refreshedAthlete);
        if (competitionAthleteId == null) {
            LOG.info("Skipping persistent rebuild for athlete without active competition membership: " + refreshedAthlete.getId());
            PerformanceResponse performance = new PerformanceResponse();
            performance.setAthlete(refreshedAthlete);
            return performance;
        }
        return rebuildAthleteStateForCompetition(refreshedAthlete, competitionAthleteId);
    }

    protected void rebuildCurrentCompetitionStates(BootcampAthlete athlete) throws SQLException, CredentialStoreException, NoSuchAlgorithmException, IOException {
        BootcampAthlete refreshedAthlete = stravaService.refreshToken(athlete);
        List<Long> competitionAthleteIds = listCurrentCompetitionAthleteIds(refreshedAthlete.getId());
        if (competitionAthleteIds.isEmpty()) {
            LOG.info("Skipping persistent rebuild for athlete without active competition membership: " + refreshedAthlete.getId());
            return;
        }
        LOG.info("Busy with athlete: " + refreshedAthlete.getFirstname() + " " + refreshedAthlete.getLastname());
        for (Long competitionAthleteId : competitionAthleteIds) {
            rebuildAthleteStateForCompetition(refreshedAthlete, competitionAthleteId);
        }
    }

    protected List<Long> listCurrentCompetitionAthleteIds(String athleteId) throws SQLException {
        return dbService.listCurrentCompetitionAthleteIds(athleteId);
    }

    protected PerformanceResponse rebuildAthleteStateForCompetition(BootcampAthlete athlete,
                                                                    long competitionAthleteId) throws SQLException, CredentialStoreException, NoSuchAlgorithmException, IOException {
        BootcampAthlete refreshedAthlete = athlete.getAccessToken() == null || athlete.getAccessToken().isBlank()
                ? athlete
                : stravaService.refreshToken(athlete);
        DBService.CompetitionAthleteConfig competitionConfig = getCompetitionAthleteConfig(competitionAthleteId);
        long startTimestamp = competitionConfig.startTimestamp();
        Long endTimestamp = competitionConfig.endTimestamp();
        double startingGoal = competitionConfig.startingGoal();
        Set<Integer> sickWeeks = getCompetitionSickWeeks(competitionAthleteId);
        List<StravaActivityResponse> activities = stravaService.getAthleteActivitiesForPeriod(startTimestamp, endTimestamp, refreshedAthlete.getAccessToken());
        activities = activities.stream()
                .sorted(Comparator.comparing(activity -> Instant.parse(activity.getStart_date())))
                .toList();

        PerformanceResponse performance = new PerformanceResponse();
        performance.setAthlete(refreshedAthlete);

        Map<String, PersistentSummarySportRowBuilder> summarySportBuilders = new LinkedHashMap<>();
        List<PersistentActivityDetailRow> activityRows = new ArrayList<>();
        List<PersistentWeeklyRow> weeklyRows = new ArrayList<>();

        double distanceToDate = 0.0;
        double scoreToDate = 0.0;
        int numberOfWeeksSinceStart = getNumberOfWeeksSinceStart(startTimestamp, endTimestamp);
        int week = 1;
        long weekEnding = startTimestamp + BootcampConstants.WEEK_IN_SECONDS;
        WeeklyPerformance weeklyPerformance = new WeeklyPerformance("Week" + week, weekEnding, startingGoal, -1.0);

        for (StravaActivityResponse activity : activities) {
            long activityTimestamp = Instant.parse(activity.getStart_date()).getEpochSecond();
            if (activityTimestamp < startTimestamp || (endTimestamp != null && activityTimestamp > endTimestamp)) {
                continue;
            }
            int loopCount = 0;
            while (activityTimestamp > weekEnding) {
                weeklyPerformance.setAverageWeeklyScore(scoreToDate, week - 1);
                weeklyPerformance.setIsSick(sickWeeks.contains(week));
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
            weeklyPerformance.setAverageWeeklyScore(scoreToDate, week - 1);
            weeklyPerformance.setIsSick(sickWeeks.contains(week));
            scoreToDate += weeklyPerformance.getWeekScore();
            performance.addWeeklyPerformance(weeklyPerformance, week);
            weeklyRows.add(buildWeeklyRow(week, weeklyPerformance, weekEnding));
            week++;
            weekEnding += BootcampConstants.WEEK_IN_SECONDS;
            weeklyPerformance = new WeeklyPerformance("Week" + week, weekEnding, weeklyPerformance.getWeekGoal(), loopCount == 0 ? weeklyPerformance.getTotalDistance() : 0.0);
            loopCount++;
        }
        while (performance.getWeeklyPerformances().size() < numberOfWeeksSinceStart) {
            weeklyPerformance.setAverageWeeklyScore(scoreToDate, week - 1);
            weeklyPerformance.setIsSick(sickWeeks.contains(week));
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
                latestWeek != null ? latestWeek.getWeekGoal() : startingGoal,
                Math.max(0, numberOfWeeksSinceStart - 1),
                startingGoal,
                latestWeek != null ? latestWeek.getWeekScore() : 0.0,
                latestWeek != null ? latestWeek.getTotalPercentOfGoal() * 100 : 0.0
        );

        Map<String, PersistentSummarySportRow> summarySportRows = summarySportBuilders.values().stream()
                .map(PersistentSummarySportRowBuilder::build)
                .collect(Collectors.toMap(PersistentSummarySportRow::sportType, row -> row, (left, right) -> left, LinkedHashMap::new));

        replacePersistentCompetitionState(competitionAthleteId, activityRows, weeklyRows, summaryRow, summarySportRows);
        return performance;
    }

    protected Long getActiveCompetitionAthleteId(BootcampAthlete athlete) throws SQLException {
        return dbService.findActiveCompetitionAthleteId(athlete.getId());
    }

    protected boolean hasActiveCompetitionMembership(String athleteId) throws SQLException {
        return dbService.hasActiveCompetitionMembership(athleteId);
    }

    protected DBService.CompetitionAthleteConfig getCompetitionAthleteConfig(long competitionAthleteId) throws SQLException {
        return dbService.getCompetitionAthleteConfig(competitionAthleteId);
    }

    protected Set<Integer> getCompetitionSickWeeks(long competitionAthleteId) throws SQLException {
        return dbService.listCompetitionSickWeeks(competitionAthleteId);
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

    protected List<Long> listActiveCompetitionIds() throws SQLException {
        return dbService.listActiveCompetitionIds();
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
        throw new IllegalStateException("Athlete competition context is required for persistent performance-list reads");
    }

    public List<PerformanceResponse> getPerformanceListForCompetition(long competitionId) {
        try {
            return dbService.getPersistentPerformanceList(competitionId);
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to load persistent performance list", e);
        }
    }

    public Map<Integer, WeeklyPerformance> getAthleteHistory(String athleteId) {
        try {
            return dbService.getPersistentAthleteHistory(athleteId);
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to load persistent athlete history", e);
        }
    }

    public Map<Integer, WeeklyPerformance> getAthleteHistoryForCompetition(long competitionId, String athleteId) {
        try {
            return dbService.getPersistentAthleteHistory(athleteId, competitionId);
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to load persistent athlete history for competition", e);
        }
    }

    public HashMap<Integer, HashMap<String, Double>> getHonourRollTotalDistance() {
        throw new IllegalStateException("Athlete competition context is required for persistent honour-roll reads");
    }

    public HashMap<Integer, HashMap<String, Double>> getHonourRollTotalDistanceForCompetition(long competitionId) {
        try {
            return dbService.getPersistentHonourRollTotalDistance(competitionId);
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to load persistent honour roll distance results", e);
        }
    }

    public HashMap<Integer, HashMap<String, Double>> getHonourRollPercentageOfGoal() {
        throw new IllegalStateException("Athlete competition context is required for persistent honour-roll reads");
    }

    public HashMap<Integer, HashMap<String, Double>> getHonourRollPercentageOfGoalForCompetition(long competitionId) {
        try {
            return dbService.getPersistentHonourRollPercentageOfGoal(competitionId);
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to load persistent honour roll goal results", e);
        }
    }

    public String getLoggedInAthleteSummary(String athleteId)
            throws IOException, CredentialStoreException, NoSuchAlgorithmException, SQLException {
        DBService.PersistentAthleteSummarySnapshot snapshot = loadPersistentAthleteSummarySnapshot(athleteId);
        if (snapshot == null) {
            return "";
        }

        Map<Integer, WeeklyPerformance> history = loadPersistentAthleteHistory(athleteId);
        WeeklyPerformance currentWeek = history.get(latestPersistedWeek(history));

        StringBuilder sports = new StringBuilder();
        Map<String, Double> sportTotals = loadPersistentSummarySportTotals(athleteId);
        if (sportTotals != null) {
            for (Map.Entry<String, Double> entry : sportTotals.entrySet()) {
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
                getScoreSummary(getSortedSummariesForCompetition(getCurrentActiveCompForAthlete(athleteId)).get(BootcampConstants.currentYearlyScoreSummary));
    }

    public String getLoggedInAthleteSummaryForCompetition(long competitionId, String athleteId)
            throws IOException, CredentialStoreException, NoSuchAlgorithmException, SQLException {
        DBService.PersistentAthleteSummarySnapshot snapshot = loadPersistentAthleteSummarySnapshot(athleteId, competitionId);
        if (snapshot == null) {
            return "";
        }

        Map<Integer, WeeklyPerformance> history = loadPersistentAthleteHistory(athleteId, competitionId);
        WeeklyPerformance currentWeek = history.get(latestPersistedWeek(history));

        StringBuilder sports = new StringBuilder();
        Map<String, Double> sportTotals = loadPersistentSummarySportTotals(athleteId, competitionId);
        if (sportTotals != null) {
            for (Map.Entry<String, Double> entry : sportTotals.entrySet()) {
                sports.append("\t").append(entry.getKey()).append(" ").append(df.format(entry.getValue())).append("km\n");
            }
        }

        return "Liewe " + snapshot.athleteFirstName() + ",\n\n" +
                "Distance this challenge: " + df.format(snapshot.distanceToDate()) + "km\n" +
                "Total points: " + df.format(snapshot.scoreToDate()) + "\n" +
                "Sports: \n" + sports +
                "Original weekly goal: " + df.format(snapshot.originalWeeklyGoal()) + "km\n\n" +
                (currentWeek == null ? "" : currentWeek.toString()) +
                "OVERALL SCORE COMPETITION:\n" +
                getScoreSummary(getSortedSummariesForCompetition(competitionId).get(BootcampConstants.currentYearlyScoreSummary));
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

    protected DBService.PersistentAthleteSummarySnapshot loadPersistentAthleteSummarySnapshot(String athleteId) throws SQLException {
        return dbService.getPersistentAthleteSummarySnapshot(athleteId);
    }

    protected DBService.PersistentAthleteSummarySnapshot loadPersistentAthleteSummarySnapshot(String athleteId, long competitionId) throws SQLException {
        return dbService.getPersistentAthleteSummarySnapshot(athleteId, competitionId);
    }

    protected Map<Integer, WeeklyPerformance> loadPersistentAthleteHistory(String athleteId) throws SQLException {
        return dbService.getPersistentAthleteHistory(athleteId);
    }

    protected Map<Integer, WeeklyPerformance> loadPersistentAthleteHistory(String athleteId, long competitionId) throws SQLException {
        return dbService.getPersistentAthleteHistory(athleteId, competitionId);
    }

    protected Map<String, Double> loadPersistentSummarySportTotals(String athleteId) throws SQLException {
        return dbService.getPersistentSummarySportTotals(athleteId);
    }

    protected Map<String, Double> loadPersistentSummarySportTotals(String athleteId, long competitionId) throws SQLException {
        return dbService.getPersistentSummarySportTotals(athleteId, competitionId);
    }

    private int latestPersistedWeek(Map<Integer, WeeklyPerformance> history) {
        return history.keySet().stream().mapToInt(Integer::intValue).max().orElse(getNumberOfWeeksSinceStart());
    }

    public int getNumberOfWeeksSinceStart() {
        return getNumberOfWeeksSinceStart(getStartTimeStamp(), null);
    }

    public int getNumberOfWeeksSinceStart(long startTimestamp, Long endTimestamp) {
        long effectiveEndMillis = endTimestamp == null
                ? System.currentTimeMillis()
                : Math.min(System.currentTimeMillis(), endTimestamp * 1000);
        int weeks = (int) Math.ceil((double) (effectiveEndMillis - (startTimestamp * 1000)) /
                (BootcampConstants.WEEK_IN_SECONDS * 1000));
        return Math.max(1, weeks);
    }

    public Map<String, HashMap<String, Double>> getSortedSummaries() {
        throw new IllegalStateException("Athlete competition context is required for persistent leaderboard reads");
    }

    public Map<String, HashMap<String, Double>> getSortedSummariesForCompetition(long competitionId) {
        try {
            return dbService.getPersistentLeaderboardSummaries(competitionId);
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to load persistent leaderboard summaries", e);
        }
    }

    public long getCurrentActiveCompForAthlete(String athleteId) {
        try {
            return resolveCompetitionId(athleteId);
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to resolve active competition for athlete " + athleteId, e);
        }
    }

    protected long resolveCompetitionId(String athleteId) throws SQLException {
        if (athleteId == null || athleteId.isBlank()) {
            throw new IllegalStateException("Athlete competition context is required for persistent scoped reads");
        }

        Long competitionId = dbService.findActiveCompetitionId(athleteId);
        if (competitionId == null) {
            throw new IllegalStateException("No active competition membership found for athlete " + athleteId);
        }
        return competitionId;
    }

    public String getZenBotStatsContext(String athleteId) {
        try {
            DBService.PersistentAthleteSummarySnapshot snapshot = dbService.getPersistentAthleteSummarySnapshot(athleteId);
            if (snapshot == null) {
                return "No athlete stats are currently available.";
            }

            Map<Integer, WeeklyPerformance> history = dbService.getPersistentAthleteHistory(athleteId);
            WeeklyPerformance currentWeek = history.get(latestPersistedWeek(history));
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
        this.honourRollTotalDistance = new HashMap<>();
        this.honourRollPercentageOfGoal = new HashMap<>();
    }


    private void persistHonourRollRows() throws SQLException {
        for (Long competitionId : listActiveCompetitionIds()) {
            persistCompetitionHonourRollRows(competitionId);
        }
    }

    private void persistCompetitionHonourRollRows(long competitionId) throws SQLException {
        Map<Integer, PersistentHonourRollRow> honourRollRows = new LinkedHashMap<>();
        HashMap<Integer, HashMap<String, Double>> distanceMap = calculatePersistentHonourRollMap(competitionId, true);
        HashMap<Integer, HashMap<String, Double>> percentMap = calculatePersistentHonourRollMap(competitionId, false);

        for (Map.Entry<Integer, HashMap<String, Double>> distanceEntry : distanceMap.entrySet()) {
            if (distanceEntry.getValue().isEmpty()) {
                continue;
            }
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

        replaceCompetitionHonourRoll(competitionId, honourRollRows);
    }

    protected HashMap<Integer, HashMap<String, Double>> calculatePersistentHonourRollMap(long competitionId, boolean distance) throws SQLException {
        return dbService.calculatePersistentHonourRollMap(competitionId, distance);
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
