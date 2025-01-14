package com.frankies.bootcamp.sport;

import com.frankies.bootcamp.model.StravaActivityResponse;

public abstract class SportFactory {
    public static BaseSport getSport(StravaActivityResponse activity){
        if(activity.getType().equalsIgnoreCase("run") && activity.getSport_type().equalsIgnoreCase("Run")) return new Run(activity);
        else if(activity.getType().equalsIgnoreCase("run") && activity.getSport_type().equalsIgnoreCase("TrailRun")) return new TrailRun(activity);
        else if(activity.getType().equalsIgnoreCase("swim")) return new Swim(activity);
        else if(activity.getType().equalsIgnoreCase("walk")) return new Walk(activity);
        else if(activity.getType().equalsIgnoreCase("Golf")) return new Golf(activity);
        else if(activity.getType().equalsIgnoreCase("Surfing")) return new Surf(activity);
        else if(activity.getType().equalsIgnoreCase("WeightTraining")) return new WeightTraining(activity);
        else if(activity.getType().equalsIgnoreCase("Workout")) return new Workout(activity);
        else if(activity.getType().equalsIgnoreCase("Soccer")) return new Soccer(activity);
        else if(activity.getType().equalsIgnoreCase("ride") && activity.getSport_type().equalsIgnoreCase("MountainBikeRide")) return new MTB(activity);
        else if(activity.getType().equalsIgnoreCase("ride") && activity.getSport_type().equalsIgnoreCase("GravelRide")) return new GravelRide(activity);
        else if(activity.getType().equalsIgnoreCase("ride") && activity.getSport_type().equalsIgnoreCase("Ride")) return new RoadBike(activity);
        else if(activity.getType().equalsIgnoreCase("EBikeRide") && activity.getSport_type().equalsIgnoreCase("EMountainBikeRide")) return new EMountainBikeRide(activity);
        else if(activity.getType().equalsIgnoreCase("EBikeRide") && activity.getSport_type().equalsIgnoreCase("EBikeRide")) return new EBikeRideRoad(activity);
        return null;
    }
}
