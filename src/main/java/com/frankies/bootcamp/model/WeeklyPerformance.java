package com.frankies.bootcamp.model;

import com.frankies.bootcamp.constant.BootcampConstants;
import com.frankies.bootcamp.sport.BaseSport;

import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

public class WeeklyPerformance {
    private String week;
    private int weekGoal;
    private Map<String, Double> sports;
    private Double totalDistance = 0.0;
    private Double totalPercentOfGoal = 0.0;
    private Double weekScore = 0.0;

    public WeeklyPerformance(String week, Long timestamp, int previousWeekGoal, Double previousWeekTotalDistance) {
        sports = new HashMap<>();
        String startDate = (new Timestamp(timestamp*1000 - BootcampConstants.WEEK_IN_SECONDS*1000)).toLocalDateTime().toLocalDate().toString();
        String endDate = (new Timestamp(timestamp*1000)).toLocalDateTime().toLocalDate().toString();
        this.week = week + " (" + startDate + " - " + endDate + ")";
        this.weekGoal = calculateWeekGoal(previousWeekGoal, previousWeekTotalDistance);
    }

    public Double getTotalDistance() {
        return totalDistance;
    }

    public String getWeek() {
        return week;
    }

    public int getWeekGoal() {
        return weekGoal;
    }

    public Double getWeekScore() {
        return weekScore;
    }

    public Double getTotalPercentOfGoal() {
        return totalPercentOfGoal;
    }

    public void addSports(BaseSport sport) {
        totalDistance += sport.getCalculatedDistance();
        if (sports == null) {
            sports = new HashMap<>();
        }
        if(sports.containsKey(sport.getSportType())) {
            sports.put(sport.getSportType(), sports.get(sport.getSportType()) + sport.getCalculatedDistance());
        }else{
            sports.put(sport.getSportType(), sport.getCalculatedDistance());
        }
        calculateWeekScore();
        calculateTotalPercentOfGoal();
    }

    @Override
    public String toString() {
        DecimalFormat df = new DecimalFormat("#.##");
        StringBuilder sb = new StringBuilder();
        if(totalDistance == 0.0){
            sb.append(":-( Lets get going!\n");
        }else{
            sb.append("Distance this week: ").append(df.format(totalDistance)).append("km\n");
        }
        sb.append("Commitment this week: ").append(getWeekGoal()).append("km\n");
        sb.append("Percentage of commitment: ").append(df.format(totalPercentOfGoal * 100)).append("%\n");
        sb.append("Points scored this week: ").append(getWeekScore()).append("\n");
        if(!sports.isEmpty()) {
            for (String key : sports.keySet()) {
                sb.append("\t").append(key).append(" : ").append(df.format(sports.get(key))).append("km\n");
            }
        }
        return sb.toString();
    }

    private int calculateWeekGoal(int previousWeekGoal, double previousWeekTotalDistance) {
        if(previousWeekTotalDistance == -1.0) {
            return previousWeekGoal;
        } else if (previousWeekTotalDistance > (previousWeekGoal*1.5)) {
            return (int) (previousWeekGoal * 1.1);
        } else if (previousWeekTotalDistance > (previousWeekGoal*2)) {
                return (int) (previousWeekGoal * 1.2);
        } else if (previousWeekTotalDistance < (previousWeekGoal*0.5)) {
            return (int) (previousWeekGoal * 0.9);
        } else {
            return previousWeekGoal;
        }
    }

    private void calculateWeekScore() {
        if(totalDistance < weekGoal*.5) {
            this.weekScore =  0.0;
        } else if (totalDistance > weekGoal*1.5) {
            this.weekScore = 1.5;
        }else if (totalDistance > weekGoal*2) {
            this.weekScore = 1.75;
        } else if (totalDistance < weekGoal) {
            this.weekScore = 0.5;
        } else {
            this.weekScore =  1.0;
        }
    }

    private void calculateTotalPercentOfGoal(){
        if(totalDistance == 0.0) {
            totalPercentOfGoal = 0.0;
        } else {
            totalPercentOfGoal = totalDistance / weekGoal;
        }
    }
}
