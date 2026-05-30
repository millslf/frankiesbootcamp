package com.frankies.bootcamp.service;

import com.frankies.bootcamp.constant.BootcampConstants;
import com.frankies.bootcamp.model.BootcampAthlete;
import com.frankies.bootcamp.model.PerformanceResponse;
import com.frankies.bootcamp.model.WeeklyPerformance;
import com.frankies.bootcamp.model.strava.StravaActivityResponse;
import com.frankies.bootcamp.sport.BaseSport;
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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static com.frankies.bootcamp.constant.BootcampConstants.START_TIMESTAMP;

@ApplicationScoped
public class ActivityProcessService {

    private DBService db;
    private StravaService strava;

    private static final Logger log = Logger.getLogger(ActivityProcessService.class);
    DecimalFormat df = new DecimalFormat("#.##");
    private static List<PerformanceResponse> performanceList;
    private static HashMap<Integer, HashMap<String, Double>> honourRollTotalDistance = new HashMap<>();
    private static HashMap<Integer, HashMap<String, Double>> honourRollPercentageOfGoal = new HashMap<>();
    private static Map<String, HashMap<String, Double>> sortedSummaries = new HashMap<>();

    @Inject
    public ActivityProcessService(DBService db, StravaService strava) {
        this.db = db;
        this.strava = strava;
    }

    protected ActivityProcessService() {
        // for CDI proxying
    }

    public void prepareSummary() throws SQLException, CredentialStoreException, NoSuchAlgorithmException, IOException {
        log.info("Prepare New Summary");

        List<PerformanceResponse> performanceList = new ArrayList<>();
        List<BootcampAthlete> athleteList;
        athleteList = db.findAllAthletes();
        List<String> sports = new ArrayList<>();
        List<StravaActivityResponse> stravaActivities;
        for (BootcampAthlete athlete : athleteList) {
            athlete = strava.refreshToken(athlete);
            PerformanceResponse performance = new PerformanceResponse();
            performance.setAthlete(athlete);
            log.info("Busy with athlete:" + athlete.getFirstname() + " Token Expiry at:" +
                    LocalDateTime.ofInstant(Instant.ofEpochMilli(athlete.getExpiresAt() * 1000), ZoneId.systemDefault()));
            stravaActivities = strava.getAthleteActivitiesForPeriod(getStartTimeStamp(), athlete.getAccessToken());
            double distance = 0;
            double score = 0;
            int week = 1;
            long weekEnding = getStartTimeStamp() + BootcampConstants.WEEK_IN_SECONDS;
            WeeklyPerformance weeklyPerformance = new WeeklyPerformance("Week" + week, weekEnding, athlete.getGoal(), -1.0);
            for (StravaActivityResponse activity : stravaActivities) {
                if (!sports.contains(activity.getType() + " " + activity.getSport_type())) {
                    sports.add(activity.getType() + " " + activity.getSport_type());
                }
                //Keep adding weeks in case an athlete did nothing for a week or more between activities.
                int loopCount = 0;
                while (Instant.parse(activity.getStart_date()).getEpochSecond() > weekEnding) {
                    updateHonourRolls(week, weeklyPerformance, athlete);
                    weeklyPerformance.setAverageWeeklyScore(score, week - 1);
                    weeklyPerformance.setIsSick(athlete.isSick(week));
                    score += weeklyPerformance.getWeekScore();
                    performance.addWeeklyPerformance(weeklyPerformance, week);
                    week++;
                    weekEnding = weekEnding + BootcampConstants.WEEK_IN_SECONDS;
                    weeklyPerformance = new WeeklyPerformance("Week" + week, weekEnding, weeklyPerformance.getWeekGoal(),
                            loopCount == 0 ? weeklyPerformance.getTotalDistance() : 0.0);
                    loopCount++;
                }
                BaseSport sport = SportFactory.getSport(activity);
                if (sport != null) {
                    performance.addSport(activity.getId(), week, sport);
                    weeklyPerformance.addSports(sport);
                    distance += sport.getCalculatedDistance();
                }
            }
            //Keep adding weeks in case an athlete did nothing for a week or more after last activity.
            int loopCount = 0;
            if (performance.getWeeklyPerformances() == null) {
                performance.addWeeklyPerformance(weeklyPerformance, week);
            }
            while (performance.getWeeklyPerformances().size() < getNumberOfWeeksSinceStart()) {
                updateHonourRolls(week, weeklyPerformance, athlete);
                weeklyPerformance.setAverageWeeklyScore(score, week - 1);
                weeklyPerformance.setIsSick(athlete.isSick(week));
                score += weeklyPerformance.getWeekScore();
                performance.addWeeklyPerformance(weeklyPerformance, week);
                week++;
                weekEnding = weekEnding + BootcampConstants.WEEK_IN_SECONDS;
                weeklyPerformance = new WeeklyPerformance("Week" + week, weekEnding, weeklyPerformance.getWeekGoal(),
                        loopCount == 0 ? weeklyPerformance.getTotalDistance() : 0.0);
                loopCount++;
            }
            performance.setDistanceToDate(distance);
            performance.setScoreToDate(score);
            performanceList.add(performance);
        }
        ActivityProcessService.performanceList = performanceList;
        log.info("All sports: " + sports);
    }

    public void prepareAthleteSummary(BootcampAthlete athlete) throws SQLException, CredentialStoreException, NoSuchAlgorithmException, IOException {
        if (athlete == null) {
            return;
        }

        List<PerformanceResponse> updatedPerformanceList = performanceList == null
                ? new ArrayList<>()
                : new ArrayList<>(performanceList);

        updatedPerformanceList.removeIf(existing -> existing.getAthlete() != null
                && athlete.getId() != null
                && athlete.getId().equals(existing.getAthlete().getId()));

        updatedPerformanceList.add(buildPerformanceForAthlete(athlete));
        ActivityProcessService.performanceList = updatedPerformanceList;
        generateAllSummaryMaps();
    }

    public List<PerformanceResponse> getPerformanceList() {
        return performanceList;
    }

    protected PerformanceResponse buildPerformanceForAthlete(BootcampAthlete athlete) throws CredentialStoreException, NoSuchAlgorithmException, IOException, SQLException {
        BootcampAthlete refreshedAthlete = strava.refreshToken(athlete);
        PerformanceResponse performance = new PerformanceResponse();
        performance.setAthlete(refreshedAthlete);
        log.info("Busy with athlete:" + refreshedAthlete.getFirstname() + " Token Expiry at:" +
                LocalDateTime.ofInstant(Instant.ofEpochMilli(refreshedAthlete.getExpiresAt() * 1000), ZoneId.systemDefault()));

        List<StravaActivityResponse> stravaActivities = strava.getAthleteActivitiesForPeriod(getStartTimeStamp(), refreshedAthlete.getAccessToken());
        double distance = 0;
        double score = 0;
        int week = 1;
        long weekEnding = getStartTimeStamp() + BootcampConstants.WEEK_IN_SECONDS;
        WeeklyPerformance weeklyPerformance = new WeeklyPerformance("Week" + week, weekEnding, refreshedAthlete.getGoal(), -1.0);

        for (StravaActivityResponse activity : stravaActivities) {
            int loopCount = 0;
            while (Instant.parse(activity.getStart_date()).getEpochSecond() > weekEnding) {
                updateHonourRolls(week, weeklyPerformance, refreshedAthlete);
                weeklyPerformance.setAverageWeeklyScore(score, week - 1);
                weeklyPerformance.setIsSick(refreshedAthlete.isSick(week));
                score += weeklyPerformance.getWeekScore();
                performance.addWeeklyPerformance(weeklyPerformance, week);
                week++;
                weekEnding = weekEnding + BootcampConstants.WEEK_IN_SECONDS;
                weeklyPerformance = new WeeklyPerformance("Week" + week, weekEnding, weeklyPerformance.getWeekGoal(),
                        loopCount == 0 ? weeklyPerformance.getTotalDistance() : 0.0);
                loopCount++;
            }
            BaseSport sport = SportFactory.getSport(activity);
            if (sport != null) {
                performance.addSport(activity.getId(), week, sport);
                weeklyPerformance.addSports(sport);
                distance += sport.getCalculatedDistance();
            }
        }

        int loopCount = 0;
        if (performance.getWeeklyPerformances() == null) {
            performance.addWeeklyPerformance(weeklyPerformance, week);
        }
        while (performance.getWeeklyPerformances().size() < getNumberOfWeeksSinceStart()) {
            updateHonourRolls(week, weeklyPerformance, refreshedAthlete);
            weeklyPerformance.setAverageWeeklyScore(score, week - 1);
            weeklyPerformance.setIsSick(refreshedAthlete.isSick(week));
            score += weeklyPerformance.getWeekScore();
            performance.addWeeklyPerformance(weeklyPerformance, week);
            week++;
            weekEnding = weekEnding + BootcampConstants.WEEK_IN_SECONDS;
            weeklyPerformance = new WeeklyPerformance("Week" + week, weekEnding, weeklyPerformance.getWeekGoal(),
                    loopCount == 0 ? weeklyPerformance.getTotalDistance() : 0.0);
            loopCount++;
        }

        performance.setDistanceToDate(distance);
        performance.setScoreToDate(score);
        return performance;
    }

    public Map<Integer, WeeklyPerformance> getAthleteHistory(String athleteId) {
        if (athleteId == null || performanceList == null) {
            return null;
        }
        for (PerformanceResponse performance : performanceList) {
            if (performance.getAthlete() != null && athleteId.equals(performance.getAthlete().getId())) {
                return performance.getWeeklyPerformances();
            }
        }
        return null;
    }

    public HashMap<Integer, HashMap<String, Double>> getHonourRollTotalDistance() {
        return honourRollTotalDistance;
    }

    public HashMap<Integer, HashMap<String, Double>> getHonourRollPercentageOfGoal() {
        return honourRollPercentageOfGoal;
    }

    public String getLoggedInAthleteSummary(String athleteId)
            throws IOException, CredentialStoreException, NoSuchAlgorithmException, SQLException {
        String loggedInAthleteSummary = "";
        if (athleteId == null || performanceList == null) {
            return loggedInAthleteSummary;
        }
        try {
            for (PerformanceResponse performance : performanceList) {
                String mailBody = getMailBody(sortedSummaries.get(BootcampConstants.currentYearlyScoreSummary),
                        sortedSummaries.get(BootcampConstants.currentWeekPercentageOfGoalSummary),
                        sortedSummaries.get(BootcampConstants.currentWeekTotalDistanceSummary), performance);
                if (performance.getAthlete() != null && athleteId.equals(performance.getAthlete().getId())) {
                    loggedInAthleteSummary = mailBody;
                }
            }
        } catch (Exception e) {
            log.error("AthletesResource, allAthleteSummary", e);
            //Continue with processing, only email sending fails.
        }
        return loggedInAthleteSummary;
    }

    public int getNumberOfWeeksSinceStart() {
        return (int) Math.round(Math.ceil((double) (System.currentTimeMillis() - (getStartTimeStamp() * 1000)) /
                (BootcampConstants.WEEK_IN_SECONDS * 1000)));
    }

    public Map<String, HashMap<String, Double>> getSortedSummaries() {
        return sortedSummaries;
    }

    public String getZenBotStatsContext(String athleteId) {
        if (athleteId == null || performanceList == null) {
            return "No athlete stats are currently available.";
        }

        PerformanceResponse performance = null;
        for (PerformanceResponse candidate : performanceList) {
            if (candidate.getAthlete() != null && athleteId.equals(candidate.getAthlete().getId())) {
                performance = candidate;
                break;
            }
        }

        if (performance == null || performance.getWeeklyPerformances() == null || performance.getWeeklyPerformances().isEmpty()) {
            return "No athlete stats are currently available.";
        }

        int currentWeek = getNumberOfWeeksSinceStart();
        WeeklyPerformance current = performance.getWeeklyPerformances().get(currentWeek);
        if (current == null) {
            return "No athlete stats are currently available.";
        }

        DecimalFormat shortDf = new DecimalFormat("#.##");
        StringBuilder context = new StringBuilder();
        context.append("Current week: ").append(current.getWeek()).append(". ");
        context.append("Distance this week: ").append(shortDf.format(current.getTotalDistance())).append(" km. ");
        context.append("Goal this week: ").append(shortDf.format(current.getWeekGoal())).append(" km. ");
        context.append("Distance left this week: ")
                .append(shortDf.format(Math.max(current.getWeekGoal() - current.getTotalDistance(), 0.0))).append(" km. ");
        context.append("Percent of goal: ").append(shortDf.format(current.getTotalPercentOfGoal() * 100)).append("%. ");

        List<String> recentWeeks = new ArrayList<>();
        double lastFiveWeeksDistance = 0.0;
        for (int week = currentWeek; week >= Math.max(1, currentWeek - 4); week--) {
            WeeklyPerformance weekPerformance = performance.getWeeklyPerformances().get(week);
            if (weekPerformance == null) {
                continue;
            }
            lastFiveWeeksDistance += weekPerformance.getTotalDistance();
            recentWeeks.add(weekPerformance.getWeek() + ": " + shortDf.format(weekPerformance.getTotalDistance()) + " km");
        }

        context.append("Distance across the last ").append(recentWeeks.size()).append(" weeks: ")
                .append(shortDf.format(lastFiveWeeksDistance)).append(" km. ");
        context.append("Recent weekly distances: ").append(String.join(", ", recentWeeks)).append(". ");

        Integer leaderboardRank = null;
        HashMap<String, Double> leaderboard = sortedSummaries.get(BootcampConstants.currentYearlyScoreSummary);
        if (leaderboard != null) {
            int rank = 1;
            for (Map.Entry<String, Double> entry : leaderboard.entrySet()) {
                String athleteFirstName = performance.getAthlete().getFirstname();
                if (entry.getKey().equalsIgnoreCase(athleteFirstName)) {
                    leaderboardRank = rank;
                    break;
                }
                rank++;
            }
        }

        if (leaderboardRank != null) {
            context.append("Current leaderboard rank by total challenge score: #").append(leaderboardRank).append(". ");
        }

        return context.toString().trim();
    }

    private long getStartTimeStamp() {
        return START_TIMESTAMP;
    }

    public void generateAllSummaryMaps() {
        HashMap<String, Double> currentWeekTotalDistanceSummary = new HashMap<>();
        for (PerformanceResponse performance : performanceList) {
            if (performance.getWeeklyPerformances().get(getNumberOfWeeksSinceStart()) != null) {
                currentWeekTotalDistanceSummary.put(performance.getAthlete().getFirstname(),
                        performance.getWeeklyPerformances().get(getNumberOfWeeksSinceStart()).getTotalDistance());
            }
        }
        HashMap<String, Double> currentWeekPercentageOfGoalSummary = new HashMap<>();
        for (PerformanceResponse performance : performanceList) {
            if (performance.getWeeklyPerformances().get(getNumberOfWeeksSinceStart()) != null) {
                currentWeekPercentageOfGoalSummary.put(performance.getAthlete().getFirstname(),
                        performance.getWeeklyPerformances().get(getNumberOfWeeksSinceStart()).getTotalPercentOfGoal() * 100);
            }
        }
        HashMap<String, Double> currentYearlyScoreSummary = new HashMap<>();
        for (PerformanceResponse performance : performanceList) {
            currentYearlyScoreSummary.put(performance.getAthlete().getFirstname(), performance.getScoreToDate());
        }
        sortedSummaries.put(BootcampConstants.currentWeekTotalDistanceSummary, sortByValue(currentWeekTotalDistanceSummary));
        sortedSummaries.put(BootcampConstants.currentWeekPercentageOfGoalSummary, sortByValue(currentWeekPercentageOfGoalSummary));
        sortedSummaries.put(BootcampConstants.currentYearlyScoreSummary, sortByValue(currentYearlyScoreSummary));
    }

    private String getMailBody(HashMap<String, Double> currentYearlyScoreSummary,
                               HashMap<String, Double> currentWeekPercentageOfGoalSummary,
                               HashMap<String, Double> currentWeekTotalDistanceSummary,
                               PerformanceResponse performance) {
        return performance +
                "OVERALL SCORE COMPETITION:\n"
                + getScoreSummary(currentYearlyScoreSummary) + "\n" +
                "'PERCENTAGE OF GOAL' WEEKLY COMPETITION:"
                + getSummary(currentWeekPercentageOfGoalSummary, "%") +
                "'TOTAL DISTANCE' WEEKLY COMPETITION:"
                + getSummary(currentWeekTotalDistanceSummary, "km");
    }

    private String getSummary(HashMap<String, Double> currentWeekSummary, String suffix) {
        return "\nOns gewaardeerde voorloper vir die week is honourable " + currentWeekSummary.keySet().stream().findFirst().get()
                + " met 'n totaal van " + df.format(currentWeekSummary.values().stream().findFirst().get()) + suffix + "\n" +
                "Dan, in BAIE spesifieke volgorde het ons: " + currentWeekSummary.keySet().stream().skip(1).collect(Collectors.joining(", ")) + "\n\n";
    }

    private String getScoreSummary(HashMap<String, Double> currentWeekSummary) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Double> entry : currentWeekSummary.entrySet()) {
            sb.append(entry.getKey()).append(": ").append(df.format(entry.getValue())).append("\n");
        }
        return sb.toString();
    }

    static <K, V extends Comparable<? super V>> List<Map.Entry<K, V>> entriesSortedByValues(Map<K, V> map) {

        List<Map.Entry<K, V>> sortedEntries = new ArrayList<Map.Entry<K, V>>(map.entrySet());

        Collections.sort(sortedEntries,
                new Comparator<Map.Entry<K, V>>() {
                    @Override
                    public int compare(Map.Entry<K, V> e1, Map.Entry<K, V> e2) {
                        return e2.getValue().compareTo(e1.getValue());
                    }
                }
        );

        return sortedEntries;
    }


    private static HashMap<String, Double> sortByValue(HashMap<String, Double> hm) {
        // Create a list from elements of HashMap
        List<Map.Entry<String, Double>> list =
                new LinkedList<>(hm.entrySet());

        // Sort the list
        Collections.sort(list,
                new Comparator<Map.Entry<String, Double>>() {
                    @Override
                    public int compare(Map.Entry<String, Double> e1, Map.Entry<String, Double> e2) {
                        return e2.getValue().compareTo(e1.getValue());
                    }
                }
        );
        // put data from sorted list to hashmap
        HashMap<String, Double> temp = new LinkedHashMap<>();
        for (Map.Entry<String, Double> aa : list) {
            temp.put(aa.getKey(), aa.getValue());
        }
        return temp;
    }

    private void updateHonourRolls(int week, WeeklyPerformance weeklyPerformance, BootcampAthlete athlete) {
        if (honourRollTotalDistance.get(week) == null) {
            honourRollTotalDistance.put(week, new HashMap<>());
            honourRollTotalDistance.get(week).put(athlete.getFirstname() + " " + athlete.getLastname(), weeklyPerformance.getTotalDistance());
        } else {
            double currentMaxDist = honourRollTotalDistance.get(week).values().iterator().next();
            if (currentMaxDist < weeklyPerformance.getTotalDistance()) {
                honourRollTotalDistance.get(week).clear();
                honourRollTotalDistance.get(week).put(athlete.getFirstname() + " " + athlete.getLastname(), weeklyPerformance.getTotalDistance());
            }
        }

        if (honourRollPercentageOfGoal.get(week) == null) {
            honourRollPercentageOfGoal.put(week, new HashMap<>());
            honourRollPercentageOfGoal.get(week).put(athlete.getFirstname() + " " + athlete.getLastname(), weeklyPerformance.getTotalPercentOfGoal());
        } else {
            double currentMaxPerc = honourRollPercentageOfGoal.get(week).values().iterator().next();
            if (currentMaxPerc < weeklyPerformance.getTotalPercentOfGoal()) {
                honourRollPercentageOfGoal.get(week).clear();
                honourRollPercentageOfGoal.get(week).put(athlete.getFirstname() + " " + athlete.getLastname(), weeklyPerformance.getTotalPercentOfGoal());
            }
        }
    }

    public boolean addActivityEvent(String athleteId, StravaActivityResponse activity) {
        PerformanceResponse perf = findPerfByAthleteId(athleteId);
        if (perf == null || activity == null || activity.getStart_date() == null) return false;

        final long startTsSec = getStartTimeStamp();
        final long weekSecs = BootcampConstants.WEEK_IN_SECONDS;
        final long actStart = java.time.Instant.parse(activity.getStart_date()).getEpochSecond();
        final int targetWeek = (int) Math.max(1, ((actStart - startTsSec) / weekSecs) + 1);

        ensureWeeksUpTo(perf, perf.getAthlete(), targetWeek, startTsSec, weekSecs);

        BaseSport sport = SportFactory.getSport(activity);
        if (sport == null) return false;

        WeeklyPerformance weekPerf = perf.getWeeklyPerformances().get(targetWeek);

        double beforeWeekScore = weekPerf.getWeekScore();
        weekPerf.addSports(sport);

        // overall per-sport totals + distance
        perf.addSport(activity.getId(), targetWeek, sport);
        perf.setDistanceToDate(perf.getDistanceToDate() + sport.getCalculatedDistance());

        double afterWeekScore = weekPerf.getWeekScore();
        perf.setScoreToDate(perf.getScoreToDate() + (afterWeekScore - beforeWeekScore));
        generateAllSummaryMaps();
        return true;
    }

    public boolean removeActivityEvent(String athleteId, Long activityId) {
        PerformanceResponse perf = findPerfByAthleteId(athleteId);
        if (perf == null || activityId == null) return false;

        PerformanceResponse.StravaActivityDetails stravaActivityDetails = perf.getStravaActivityDetailsByStravaID(activityId);
        if (stravaActivityDetails == null || stravaActivityDetails.getWeek() == null) return false;

        final int targetWeek = stravaActivityDetails.getWeek();

        if (perf.getWeeklyPerformances() == null) return false;

        WeeklyPerformance weekPerf = perf.getWeeklyPerformances().get(targetWeek);
        if (weekPerf == null) return false;

        BaseSport sport = stravaActivityDetails.getSport();
        if (sport == null) return false;

        double beforeWeekScore = weekPerf.getWeekScore();
        weekPerf.removeSports(sport);

        perf.removeSport(stravaActivityDetails);
        double newDistToDate = perf.getDistanceToDate() - sport.getCalculatedDistance();
        perf.setDistanceToDate(newDistToDate < 0 ? 0.0 : newDistToDate);

        double afterWeekScore = weekPerf.getWeekScore();
        double newScore = perf.getScoreToDate() + (afterWeekScore - beforeWeekScore);
        perf.setScoreToDate(newScore < 0 ? 0.0 : newScore);
        generateAllSummaryMaps();
        return true;
    }

    private PerformanceResponse findPerfByAthleteId(String athleteId) {
        if (ActivityProcessService.performanceList == null) return null;
        for (PerformanceResponse p : ActivityProcessService.performanceList) {
            if (p.getAthlete() != null && p.getAthlete().getId().equals(athleteId)) {
                return p;
            }
        }
        return null;
    }

    private void ensureWeeksUpTo(PerformanceResponse perf,
                                 BootcampAthlete athlete,
                                 int upToWeek,
                                 long startTsSec,
                                 long weekSecs) {
        if (perf.getWeeklyPerformances() == null || perf.getWeeklyPerformances().isEmpty()) {
            long weekOneEnding = startTsSec + weekSecs;
            WeeklyPerformance weekOne = new WeeklyPerformance("Week1", weekOneEnding, athlete.getGoal(), -1.0);
            perf.addWeeklyPerformance(weekOne, 1);
        }

        int highestExistingWeek = perf.getWeeklyPerformances().keySet().stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(1);

        for (int weekNumber = 2; weekNumber <= upToWeek; weekNumber++) {
            if (perf.getWeeklyPerformances().containsKey(weekNumber)) {
                continue;
            }

            WeeklyPerformance previousWeek = perf.getWeeklyPerformances().get(weekNumber - 1);
            if (previousWeek == null) {
                throw new IllegalStateException("Missing weekly performance for week " + (weekNumber - 1));
            }

            long weekEnding = startTsSec + (weekNumber * weekSecs);
            boolean isFirstNewWeek = weekNumber > highestExistingWeek;
            double carryForward = isFirstNewWeek ? previousWeek.getTotalDistance() : 0.0;
            WeeklyPerformance nextWeek = new WeeklyPerformance(
                    "Week" + weekNumber,
                    weekEnding,
                    previousWeek.getWeekGoal(),
                    carryForward);
            perf.addWeeklyPerformance(nextWeek, weekNumber);
        }
    }

    public void run() {
        try {
            prepareSummary();
            generateAllSummaryMaps();
        } catch (Exception e) {
            log.error("ActivityProcessService, Something went wrong generating summary", e);
        }
    }
}