package com.frankies.bootcamp.model;

import java.util.HashMap;
import java.util.Map;

public class WeeklyPerformance {
    private String week;
    private Map<String, Double> sports;

    public WeeklyPerformance(String week) {
        this.week = week;
    }

    public String getWeek() {
        return week;
    }
    public void setWeek(String week) {
        this.week = week;
    }

    public void addSports(String sport, Double distance) {
        if (sports == null) {
            sports = new HashMap<>();
        }
        if(sports.containsKey(sport)) {
            sports.put(sport, sports.get(sport) + distance);
        }else{
            sports.put(sport, distance);
        }
    }
}
