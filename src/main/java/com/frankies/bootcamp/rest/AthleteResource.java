package com.frankies.bootcamp.rest;


import com.frankies.bootcamp.model.BootcampAthlete;
import com.frankies.bootcamp.model.PerformanceResponse;
import com.frankies.bootcamp.model.StravaActivityResponse;
import com.frankies.bootcamp.model.WeeklyPerformance;
import com.frankies.bootcamp.service.DBService;
import com.frankies.bootcamp.service.StravaService;
import com.google.gson.Gson;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;

import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Path("/Athletes")
public class AthleteResource {
    @GET
    @Produces("text/plain")
    public String allAthleteSummary(@QueryParam("startTimeStamp") Long startTimeStamp) {

        DBService db = new DBService();
        List<BootcampAthlete> athleteList;
        List<PerformanceResponse> performanceList = new ArrayList<>();
        try {
            athleteList = db.findAll();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return "Nope, iets het nou sleg foutgegaan bel jou maatjie!";
        }
        List<StravaActivityResponse> stravaActivities;
        for (BootcampAthlete athlete : athleteList) {

            try {
                if (athlete.getExpiresAt()*1000 < System.currentTimeMillis()) {
                    StravaService strava = new StravaService();
                    athlete = strava.refreshToken(athlete);
                }

                StravaService strava = new StravaService();
                PerformanceResponse performance = new PerformanceResponse();
                performance.setFirstname(athlete.getFirstname());
                stravaActivities = strava.getAthleteActivitiesForPeriod(startTimeStamp, athlete.getAccessToken());
                double distance = 0;
                int week = 1;
                long weekEnding = startTimeStamp+604800;
                WeeklyPerformance weeklyPerformance = new WeeklyPerformance("Week" + week);
                for (StravaActivityResponse activity : stravaActivities) {
                    while(Instant.parse(activity.getStart_date()).getEpochSecond() > weekEnding){
                        performance.addWeeklyPerformance(weeklyPerformance);
                        week++;
                        weekEnding = weekEnding + 604800;
                        weeklyPerformance = new WeeklyPerformance("Week" + week);
                    }
                    if (activity.getType().equalsIgnoreCase("run")) {
                        distance += activity.getDistance() / 1000;
                        weeklyPerformance.addSports("Run", activity.getDistance()/1000);
                    }
                    if (activity.getType().equalsIgnoreCase("swim")) {
                        distance += activity.getDistance()*4 / 1000;
                        weeklyPerformance.addSports("Swim", activity.getDistance()*4/1000);
                    }
                    if (activity.getType().equalsIgnoreCase("ride") && activity.getSport_type().equalsIgnoreCase("MountainBikeRide")) {
                        distance += activity.getDistance()/2 / 1000;
                        weeklyPerformance.addSports("MTB", activity.getDistance()/2/1000);
                    }
                }
                performance.addWeeklyPerformance(weeklyPerformance);
                performance.setDistanceToDate(distance);
                performanceList.add(performance);
            } catch (Exception e) {
                System.out.println(e.getMessage());
                return "Nope, iets het nou sleg foutgegaan bel jou maatjie!";
            }
        }
        return  new Gson().toJson(performanceList);
    }
}