package com.frankies.bootcamp.model;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

public class PerformanceResponse {
    private BootcampAthlete athlete;
    private Map<Integer, WeeklyPerformance> weeklyPerformances;
    private Double distanceToDate = 0.0;
    private Double scoreToDate = 0.0;

    public BootcampAthlete getAthlete() {
        return athlete;
    }

    public void setAthlete(BootcampAthlete athlete) {
        this.athlete = athlete;
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

    @Override
    public String toString() {
        DecimalFormat df = new DecimalFormat("#.##");
        return "Liewe " + athlete.getFirstname() + ",\n\n" +
                "Distance this challenge: " + df.format(this.distanceToDate) + "km\n" +
                "Total points: " + df.format(this.scoreToDate) + "\n" +
                "Original weekly commitment: " + df.format(athlete.getGoal()) + "km\n\n" +
                this.getWeeklyPerformances().get(weeklyPerformances.size()).toString();
    }
}
