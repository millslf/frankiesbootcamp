package com.frankies.bootcamp.model;

import java.util.ArrayList;
import java.util.List;

public class PerformanceResponse {
    private String firstname;
    private List<WeeklyPerformance> weeklyPerformances;
    private Double distanceToDate;

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public List<WeeklyPerformance> getWeeklyPerformances() {
        return weeklyPerformances;
    }

    public void setWeeklyPerformances(List<WeeklyPerformance> weeklyPerformances) {
        this.weeklyPerformances = weeklyPerformances;
    }

    public void addWeeklyPerformance(WeeklyPerformance weeklyPerformance) {
        if (this.weeklyPerformances == null) {
            weeklyPerformances = new ArrayList<>();
        }
        this.weeklyPerformances.add(weeklyPerformance);
    }

    public Double getDistanceToDate() {
        return distanceToDate;
    }

    public void setDistanceToDate(Double distanceToDate) {
        this.distanceToDate = distanceToDate;
    }
}
