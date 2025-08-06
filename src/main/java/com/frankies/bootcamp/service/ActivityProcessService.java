package com.frankies.bootcamp.service;

import com.frankies.bootcamp.constant.BootcampConstants;
import com.frankies.bootcamp.model.BootcampAthlete;
import com.frankies.bootcamp.model.PerformanceResponse;
import com.frankies.bootcamp.model.WeeklyPerformance;
import com.frankies.bootcamp.model.strava.StravaActivityResponse;
import com.frankies.bootcamp.sport.BaseSport;
import com.frankies.bootcamp.sport.SportFactory;
import com.frankies.bootcamp.utils.CreateEmail;
import com.frankies.bootcamp.utils.SendMessage;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.mail.MessagingException;
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
public class ActivityProcessService extends TimerTask {
    private DBService db = new DBService();
    private StravaService strava = new StravaService();
    private static final Logger log = Logger.getLogger(ActivityProcessService.class);
    DecimalFormat df = new DecimalFormat("#.##");
    private static List<PerformanceResponse> performanceList;
    private static HashMap<Integer, HashMap<String, Double>> honourRollTotalDistance = new HashMap<>();
    private static HashMap<Integer, HashMap<String, Double>> honourRollPercentageOfGoal = new HashMap<>();
    private static Map<String, HashMap<String, Double>> sortedSummaries = new HashMap<>();

    @PostConstruct
    public void init(@Observes @Initialized(ApplicationScoped.class) Object init) {
        Timer timer = new Timer();
        TimerTask task = new ActivityProcessService();
        timer.schedule(task, 0, BootcampConstants.LITTLE_MORE_THAN_AN_HOUR_IN_MILLIS);
    }

    public void prepareSummary() throws SQLException, CredentialStoreException, NoSuchAlgorithmException, IOException {
        log.info("Prepare New Summary");

        List<PerformanceResponse> performanceList = new ArrayList<>();
        List<BootcampAthlete> athleteList;
        athleteList = db.findAllAthletes();

        List<String> sports = new ArrayList<>();
        List<StravaActivityResponse> stravaActivities;
        for (BootcampAthlete athlete : athleteList) {
            if (athlete.getExpiresAt() * 1000 < System.currentTimeMillis()) {
                athlete = strava.refreshToken(athlete);
            }
            PerformanceResponse performance = new PerformanceResponse();
            performance.setAthlete(athlete);
            log.info("Busy with athlete:" + athlete.getFirstname() + " Token Expiry at:" + LocalDateTime.ofInstant(Instant.ofEpochMilli(athlete.getExpiresAt()*1000), ZoneId.systemDefault()));
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
                    weeklyPerformance.setAverageWeeklyScore(score, week-1);
                    weeklyPerformance.setIsSick(athlete.isSick(week));
                    score += weeklyPerformance.getWeekScore();
                    performance.addWeeklyPerformance(weeklyPerformance, week);
                    week++;
                    weekEnding = weekEnding + BootcampConstants.WEEK_IN_SECONDS;
                    weeklyPerformance = new WeeklyPerformance("Week" + week, weekEnding, weeklyPerformance.getWeekGoal(), loopCount == 0 ? weeklyPerformance.getTotalDistance() : 0.0);
                    loopCount++;
                }
                BaseSport sport = SportFactory.getSport(activity);
                if (sport != null) {
                    performance.addSport(sport.getSportType(), sport.getCalculatedDistance());
                    weeklyPerformance.addSports(sport);
                    distance += sport.getCalculatedDistance();
                }
            }
            //Keep adding weeks in case an athlete did nothing for a week or more after last activity.
            int loopCount = 0;
            if(performance.getWeeklyPerformances() == null){
                performance.addWeeklyPerformance(weeklyPerformance, week);
            }
            while (performance.getWeeklyPerformances().size() < getNumberOfWeeksSinceStart()) {
                updateHonourRolls(week, weeklyPerformance, athlete);
                weeklyPerformance.setAverageWeeklyScore(score, week-1);
                weeklyPerformance.setIsSick(athlete.isSick(week));
                score += weeklyPerformance.getWeekScore();
                performance.addWeeklyPerformance(weeklyPerformance, week);
                week++;
                weekEnding = weekEnding + BootcampConstants.WEEK_IN_SECONDS;
                weeklyPerformance = new WeeklyPerformance("Week" + week, weekEnding, weeklyPerformance.getWeekGoal(), loopCount == 0 ? weeklyPerformance.getTotalDistance() : 0.0);
                loopCount++;
            }
            performance.setDistanceToDate(distance);
            performance.setScoreToDate(score);
            performanceList.add(performance);
        }
        ActivityProcessService.performanceList = performanceList;
        log.info("All sports: " + sports);
    }

    public List<PerformanceResponse> getPerformanceList() {
        return performanceList;
    }

    public Map<Integer, WeeklyPerformance> getAthleteHistory(String loggedInAthlete){
        for (PerformanceResponse performance : performanceList) {
            if (loggedInAthlete.equals(performance.getAthlete().getEmail())) {
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

    public String sendReport(boolean reportToAllAthletes,
                             boolean reportToDevOnly,
                             String loggedInAthlete) throws IOException, CredentialStoreException, NoSuchAlgorithmException, SQLException {
        String loggedInAthleteSummary = "";
        log.info("Logged in athlete: " + loggedInAthlete);
        try {
            for (PerformanceResponse performance : performanceList) {
                String mailBody = getMailBody(sortedSummaries.get(BootcampConstants.currentYearlyScoreSummary),
                        sortedSummaries.get(BootcampConstants.currentWeekPercentageOfGoalSummary),
                        sortedSummaries.get(BootcampConstants.currentWeekTotalDistanceSummary), performance);
                if (null != performance.getAthlete().getEmail() && reportToAllAthletes && loggedInAthlete.equals("millslf@gmail.com")) {
                    emailReport(performance, mailBody);
                    log.info("Sent to: " + performance.getAthlete().getFirstname());
                } else {
//                    log.info("Skipping email for " + performance.getAthlete().getFirstname() + " because email is null or not reporting to all athletes.");
                }
                if (loggedInAthlete.equals(performance.getAthlete().getEmail())) {
                    loggedInAthleteSummary = mailBody;
                }
                if (null != performance.getAthlete().getEmail() && reportToDevOnly && performance.getAthlete().getEmail().equals("millslf@gmail.com") && loggedInAthlete.equals("millslf@gmail.com")) {
                    emailReport(performance, mailBody);
                    log.info("Sent to developer" + performance.getAthlete().getFirstname());
                }
            }
        } catch (Exception e) {
            log.error("AthletesResource, allAthleteSummary", e);
            //Continue with processing, only email sending fails.
        }
        return loggedInAthleteSummary;
    }

    public int getNumberOfWeeksSinceStart() {
        return (int) Math.round(Math.ceil((double) (System.currentTimeMillis() - (getStartTimeStamp() * 1000)) / (BootcampConstants.WEEK_IN_SECONDS * 1000)));
    }

    public Map<String, HashMap<String, Double>> getSortedSummaries() {
        return sortedSummaries;
    }

    private long getStartTimeStamp() {
        return START_TIMESTAMP;
    }

    private void emailReport(PerformanceResponse performance, String mailBody) throws MessagingException, SQLException, IOException {
        SendMessage sendMessage = new SendMessage();
        sendMessage.sendEmail(
                CreateEmail.createEmail(
                        performance.getAthlete().getEmail(),
                        "Frankies Bootcamp '<frankiesbootcamp@gmail.com>'",
                        "Performance report for " +
                                performance.getWeeklyPerformances().get(getNumberOfWeeksSinceStart()).getWeek(), mailBody));

    }

    public void generateAllSummaryMaps() {
        HashMap<String, Double> currentWeekTotalDistanceSummary = new HashMap<>();
        for (PerformanceResponse performance : performanceList) {
            if (performance.getWeeklyPerformances().get(getNumberOfWeeksSinceStart()) != null) {
                currentWeekTotalDistanceSummary.put(performance.getAthlete().getFirstname(), performance.getWeeklyPerformances().get(getNumberOfWeeksSinceStart()).getTotalDistance());
            }
        }
        HashMap<String, Double> currentWeekPercentageOfGoalSummary = new HashMap<>();
        for (PerformanceResponse performance : performanceList) {
            if (performance.getWeeklyPerformances().get(getNumberOfWeeksSinceStart()) != null) {
                currentWeekPercentageOfGoalSummary.put(performance.getAthlete().getFirstname(), performance.getWeeklyPerformances().get(getNumberOfWeeksSinceStart()).getTotalPercentOfGoal() * 100);
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

//    private Double addWeek(int week, Double score, WeeklyPerformance weeklyPerformance, BootcampAthlete athlete, PerformanceResponse performance, long weekEnding, int loopCount) {
//        updateHonourRolls(week, weeklyPerformance, athlete);
//        score += weeklyPerformance.getWeekScore();
//        weeklyPerformance.setAverageWeeklyScore(score/week);
//        weeklyPerformance.setIsSick(athlete.isSick(week));
//        performance.addWeeklyPerformance(weeklyPerformance, week);
//        week++;
//        weekEnding = weekEnding + BootcampConstants.WEEK_IN_SECONDS;
//        weeklyPerformance = new WeeklyPerformance("Week" + week, weekEnding, weeklyPerformance.getWeekGoal(), loopCount == 0 ? weeklyPerformance.getTotalDistance() : 0.0);
//        loopCount++;
//
//    }

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
        if(honourRollTotalDistance.get(week) == null){
            honourRollTotalDistance.put(week, new HashMap<>());
            honourRollTotalDistance.get(week).put(athlete.getFirstname() + " " + athlete.getLastname(), weeklyPerformance.getTotalDistance());
        }else {
            double currentMaxDist = honourRollTotalDistance.get(week).values().iterator().next();
            if (currentMaxDist < weeklyPerformance.getTotalDistance()) {
                honourRollTotalDistance.get(week).clear();
                honourRollTotalDistance.get(week).put(athlete.getFirstname() + " " + athlete.getLastname(), weeklyPerformance.getTotalDistance());
            }
        }

        if(honourRollPercentageOfGoal.get(week) == null){
            honourRollPercentageOfGoal.put(week, new HashMap<>());
            honourRollPercentageOfGoal.get(week).put(athlete.getFirstname() + " " + athlete.getLastname(), weeklyPerformance.getTotalPercentOfGoal());
        }else {
            double currentMaxPerc = honourRollPercentageOfGoal.get(week).values().iterator().next();
            if (currentMaxPerc < weeklyPerformance.getTotalPercentOfGoal()) {
                honourRollPercentageOfGoal.get(week).clear();
                honourRollPercentageOfGoal.get(week).put(athlete.getFirstname() + " " + athlete.getLastname(), weeklyPerformance.getTotalPercentOfGoal());
            }
        }
    }

    @Override
    public void run() {
        try {
            prepareSummary();
            generateAllSummaryMaps();
        } catch (Exception e) {
            log.error("ActivityProcessService, Something went wrong generating summary", e);
        }
    }
}
