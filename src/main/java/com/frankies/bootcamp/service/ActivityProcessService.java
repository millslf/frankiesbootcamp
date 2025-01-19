package com.frankies.bootcamp.service;

import com.frankies.bootcamp.constant.BootcampConstants;
import com.frankies.bootcamp.model.BootcampAthlete;
import com.frankies.bootcamp.model.PerformanceResponse;
import com.frankies.bootcamp.model.strava.StravaActivityResponse;
import com.frankies.bootcamp.model.WeeklyPerformance;
import com.frankies.bootcamp.sport.BaseSport;
import com.frankies.bootcamp.sport.SportFactory;
import com.frankies.bootcamp.utils.CreateEmail;
import com.frankies.bootcamp.utils.SendMessage;
import jakarta.mail.MessagingException;
import org.jboss.logging.Logger;
import org.wildfly.security.credential.store.CredentialStoreException;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class ActivityProcessService {
    private static final Logger log = Logger.getLogger(ActivityProcessService.class);
    DecimalFormat df = new DecimalFormat("#.##");

    public List<PerformanceResponse> prepareSummary(List<PerformanceResponse> performanceList, Long startTimeStamp, int numberOfWeeksSinceStart) throws SQLException, CredentialStoreException, NoSuchAlgorithmException, IOException {
        List<BootcampAthlete> athleteList;
        DBService db = new DBService();
        athleteList = db.findAllAthletes();

        List<String> sports = new ArrayList<>();
        List<StravaActivityResponse> stravaActivities;
        //Start timestamp not too long ago
        //Add athlete ID to get only a specific athlete data
        //Send email based on param passed in.
        for (BootcampAthlete athlete : athleteList) {
            if (athlete.getExpiresAt() * 1000 < System.currentTimeMillis()) {
                StravaService strava = new StravaService();
                athlete = strava.refreshToken(athlete);
            }
            StravaService strava = new StravaService();
            PerformanceResponse performance = new PerformanceResponse();
            performance.setAthlete(athlete);
            stravaActivities = strava.getAthleteActivitiesForPeriod(startTimeStamp, athlete.getAccessToken());
            double distance = 0;
            double score = 0;
            int week = 1;
            long weekEnding = startTimeStamp + BootcampConstants.WEEK_IN_SECONDS;
            WeeklyPerformance weeklyPerformance = new WeeklyPerformance("Week" + week, weekEnding, athlete.getGoal(), -1.0);
            for (StravaActivityResponse activity : stravaActivities) {
                if (!sports.contains(activity.getType() + " " + activity.getSport_type())) {
                    sports.add(activity.getType() + " " + activity.getSport_type());
                }
                //Keep adding weeks in case an athlete did nothing for a week or more between activities.
                int loopCount = 0;
                while (Instant.parse(activity.getStart_date()).getEpochSecond() > weekEnding) {
                    score += weeklyPerformance.getWeekScore();
                    performance.addWeeklyPerformance(weeklyPerformance, week);
                    week++;
                    weekEnding = weekEnding + BootcampConstants.WEEK_IN_SECONDS;
                    weeklyPerformance = new WeeklyPerformance("Week" + week, weekEnding, weeklyPerformance.getWeekGoal(), loopCount == 0 ? weeklyPerformance.getTotalDistance() : 0.0);
                    loopCount++;
                }
                BaseSport sport = SportFactory.getSport(activity);
                if (sport != null) {
                    weeklyPerformance.addSports(sport);
                    distance += sport.getCalculatedDistance();
                }
            }
            //Keep adding weeks in case an athlete did nothing for a week or more after last activity.
            int loopCount = 0;
            while (performance.getWeeklyPerformances().size() < numberOfWeeksSinceStart) {
                score += weeklyPerformance.getWeekScore();
                performance.addWeeklyPerformance(weeklyPerformance, week);
                week++;
                weekEnding = weekEnding + BootcampConstants.WEEK_IN_SECONDS;
                weeklyPerformance = new WeeklyPerformance("Week" + week, weekEnding, weeklyPerformance.getWeekGoal(), loopCount == 0 ? weeklyPerformance.getTotalDistance() : 0.0);
                loopCount++;
            }
            performance.setDistanceToDate(distance);
            performance.setScoreToDate(score + weeklyPerformance.getWeekScore());
            performanceList.add(performance);
        }
        log.info("All sports: " + sports);
        return performanceList;
    }

    public String sendReport(List<PerformanceResponse> performanceList,
                             int finalWeekCount,
                             boolean reportToAllAthletes,
                             boolean reportToDevOnly,
                             String loggedInAthlete) throws IOException, CredentialStoreException, NoSuchAlgorithmException, SQLException {
        String loggedInAthleteSummary="";
        log.info("Logged in athlete: " + loggedInAthlete);
        try {
            Map<String, HashMap<String, Double>> summaries = GenerateAllSummaryMaps(performanceList, finalWeekCount);
            for (PerformanceResponse performance : performanceList) {
                String mailBody = getMailBody(summaries.get(BootcampConstants.currentYearlyScoreSummary),
                        summaries.get(BootcampConstants.currentWeekPercentageOfGoalSummary),
                        summaries.get(BootcampConstants.currentWeekTotalDistanceSummary), performance);
                if (null != performance.getAthlete().getEmail() && reportToAllAthletes && loggedInAthlete.equals("millslf@gmail.com")) {
                    sendReport(performance, finalWeekCount, mailBody);
                    log.info("Sent to: " + performance.getAthlete().getFirstname());
                } else {
                    log.info("Skipping email for " + performance.getAthlete().getFirstname() + " because email is null or not reporting to all athletes.");
                }
                if (loggedInAthlete.equals(performance.getAthlete().getEmail())) {
                    loggedInAthleteSummary = mailBody;
                }
                if(reportToDevOnly && performance.getAthlete().getEmail().equals("millslf@gmail.com")) {
//                    sendReport(performance, finalWeekCount, mailBody);
                    log.info("Sent to developer (Not really send is commented out)" + performance.getAthlete().getFirstname());
                }
            }
        } catch (Exception e) {
            log.error("AthletesResource, allAthleteSummary", e);
            //Continue with processing, only email sending fails.
        }
        return loggedInAthleteSummary;
    }

    private void sendReport(PerformanceResponse performance, int finalWeekCount, String mailBody) throws MessagingException, SQLException, IOException {
        SendMessage sendMessage = new SendMessage();
        sendMessage.sendEmail(
                CreateEmail.createEmail(
                        performance.getAthlete().getEmail(),
                        "Frankies Bootcamp '<frankiesbootcamp@gmail.com>'",
                        "Performance report for " +
                                performance.getWeeklyPerformances().get(finalWeekCount).getWeek(), mailBody));

    }

    private Map<String, HashMap<String, Double>> GenerateAllSummaryMaps(List<PerformanceResponse> performanceList,
                                                                        int finalWeekCount) {
        Map<String, HashMap<String, Double>> allSummaries = new HashMap<>();
        HashMap<String, Double> currentWeekTotalDistanceSummary = new HashMap<>();
        for (PerformanceResponse performance : performanceList) {
            if (performance.getWeeklyPerformances().get(finalWeekCount) != null) {
                currentWeekTotalDistanceSummary.put(performance.getAthlete().getFirstname(), performance.getWeeklyPerformances().get(finalWeekCount).getTotalDistance());
            }
        }
        HashMap<String, Double> currentWeekPercentageOfGoalSummary = new HashMap<>();
        for (PerformanceResponse performance : performanceList) {
            if (performance.getWeeklyPerformances().get(finalWeekCount) != null) {
                currentWeekPercentageOfGoalSummary.put(performance.getAthlete().getFirstname(), performance.getWeeklyPerformances().get(finalWeekCount).getTotalPercentOfGoal() * 100);
            }
        }
        HashMap<String, Double> currentYearlyScoreSummary = new HashMap<>();
        for (PerformanceResponse performance : performanceList) {
            currentYearlyScoreSummary.put(performance.getAthlete().getFirstname(), performance.getScoreToDate());
        }
        allSummaries.put(BootcampConstants.currentWeekTotalDistanceSummary, currentWeekTotalDistanceSummary);
        allSummaries.put(BootcampConstants.currentWeekPercentageOfGoalSummary, currentWeekPercentageOfGoalSummary);
        allSummaries.put(BootcampConstants.currentYearlyScoreSummary, currentYearlyScoreSummary);
        return allSummaries;
    }

    private String getMailBody(HashMap<String, Double> currentYearlyScoreSummary,
                               HashMap<String, Double> currentWeekPercentageOfGoalSummary,
                               HashMap<String, Double> currentWeekTotalDistanceSummary,
                               PerformanceResponse performance) {
        return performance + "\n" +
                "OVERALL SCORE COMPETITION:\n"
                + getScoreSummary(currentYearlyScoreSummary) + "\n" +
                "'PERCENTAGE OF GOAL' WEEKLY COMPETITION:"
                + getSummary(currentWeekPercentageOfGoalSummary, "%") +
                "'TOTAL DISTANCE' WEEKLY COMPETITION:"
                + getSummary(currentWeekTotalDistanceSummary, "km") +
                BootcampConstants.SCORING +
                BootcampConstants.DISCLAIMER;
    }

    private String getSummary(HashMap<String, Double> currentWeekSummary, String suffix) {
        Map<String, Double> sortedMap = sortByValue(currentWeekSummary);
        return "\nOns gewaardeerde voorloper vir die week is honourable " + sortedMap.keySet().stream().findFirst().get()
                + " met 'n totaal van " + df.format(sortedMap.values().stream().findFirst().get()) + suffix + "\n" +
                "Dan, in BAIE spesifieke volgorde het ons: " + sortedMap.keySet().stream().skip(1).collect(Collectors.joining(", ")) + "\n\n";
    }

    private String getScoreSummary(HashMap<String, Double> currentWeekSummary) {
        Map<String, Double> sortedMap = sortByValue(currentWeekSummary);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Double> entry : sortedMap.entrySet()) {
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

}
