package com.frankies.bootcamp.model;

import com.frankies.bootcamp.sport.BaseSport;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PerformanceResponse {
    private BootcampAthlete athlete;
    private Map<String, Double> sports = new HashMap<>();
    private Map<Integer, WeeklyPerformance> weeklyPerformances;
    private Double distanceToDate = 0.0;
    private Double scoreToDate = 0.0;
    private List<StravaActivityDetails> stravaActivityDetails = new ArrayList<>();

    public BootcampAthlete getAthlete() {
        return athlete;
    }

    public void setAthlete(BootcampAthlete athlete) {
        this.athlete = athlete;
    }

    public void addSport(Long stravaActivityId, Integer week, BaseSport sport) {
        stravaActivityDetails.add(new StravaActivityDetails(week, stravaActivityId, sport));
        if(sports.containsKey(sport.getSportType())) {
            sports.put(sport.getSportType(), sports.get(sport.getSportType()) + sport.getCalculatedDistance());
        }else{
            this.sports.put(sport.getSportType(), sport.getCalculatedDistance());
        }
    }

    public void removeSport(StravaActivityDetails details) {
        if(sports.containsKey(details.getSport().getSportType())) {
            sports.put(details.getSport().getSportType(), sports.get(details.getSport().getSportType()) - details.getSport().getCalculatedDistance());
        }
    }
    
    public Map<Integer, WeeklyPerformance> getWeeklyPerformances() {
        return weeklyPerformances;
    }

    public void addWeeklyPerformance(WeeklyPerformance weeklyPerformance, Integer week) {
        if (this.weeklyPerformances == null) {
            weeklyPerformances = new HashMap<>();
        }
        this.weeklyPerformances.put(week, weeklyPerformance);
    }

    public Double getDistanceToDate() {
        return distanceToDate;
    }

    public void setDistanceToDate(Double distanceToDate) {
        this.distanceToDate = distanceToDate;
    }

    public void setScoreToDate(Double scoreToDate) {
        this.scoreToDate = scoreToDate;
    }

    public Double getScoreToDate() {
        return scoreToDate;
    }

    public StravaActivityDetails getStravaActivityDetailsByStravaID(Long stravaActivityId) {
        for (StravaActivityDetails details : stravaActivityDetails) {
            if(details.stravaActivityId.equals(stravaActivityId) && (details.week != null && details.week > 0)) {
                return details;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        DecimalFormat df = new DecimalFormat("#.##");
        StringBuilder sportsString = new StringBuilder();
        for (String key : sports.keySet()) {
            sportsString.append("\t").append(key).append(" ").append(df.format(sports.get(key))).append("km\n");
        }
        return "Liewe " + athlete.getFirstname() + ",\n\n" +
                "Distance this challenge: " + df.format(this.distanceToDate) + "km\n" +
                "Total points: " + df.format(this.scoreToDate) + "\n" +
                "Sports: \n" + sportsString +
                "Original weekly goal: " + df.format(athlete.getGoal()) + "km\n\n" +
                this.getWeeklyPerformances().get(weeklyPerformances.size()).toString();
    }

    public static class StravaActivityDetails {
        public StravaActivityDetails(Integer week, Long stravaActivityId, BaseSport sport) {
            this.week = week;
            this.stravaActivityId = stravaActivityId;
            this.sport = sport;
        }

        private Integer week;
        private Long stravaActivityId;
        private BaseSport sport;

        public Integer getWeek() {
            return week;
        }

        public void setWeek(Integer week) {
            this.week = week;
        }

        public Long getStravaActivityId() {
            return stravaActivityId;
        }

        public void setStravaActivityId(Long stravaActivityId) {
            this.stravaActivityId = stravaActivityId;
        }

        public BaseSport getSport() {
            return sport;
        }

        public void setSport(BaseSport sport) {
            this.sport = sport;
        }
    }
}
