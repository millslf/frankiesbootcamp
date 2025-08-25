// src/main/java/com/frankies/bootcamp/sport/SportFactory.java
package com.frankies.bootcamp.sport;

import com.frankies.bootcamp.constant.StravaStrings;
import com.frankies.bootcamp.model.strava.StravaActivityResponse;

public abstract class SportFactory {
    public static BaseSport getSport(StravaActivityResponse activity){
        if (activity == null || activity.getType() == null || activity.getSport_type() == null) return null;

        String type = activity.getType();
        String sport = activity.getSport_type();

        if(type.equalsIgnoreCase(StravaStrings.Type.RUN) && sport.equalsIgnoreCase(StravaStrings.SportType.RUN)) return new Run(activity);
        else if(type.equalsIgnoreCase(StravaStrings.Type.RUN) && sport.equalsIgnoreCase(StravaStrings.SportType.TRAIL_RUN)) return new TrailRun(activity);
        else if(type.equalsIgnoreCase(StravaStrings.Type.SWIM)) return new Swim(activity);
        else if(type.equalsIgnoreCase(StravaStrings.Type.WALK)) return new Walk(activity);
        else if(type.equalsIgnoreCase(StravaStrings.Type.HIKE)) return new Hike(activity);
        else if(type.equalsIgnoreCase(StravaStrings.Type.GOLF)) return new Golf(activity);
        else if(type.equalsIgnoreCase(StravaStrings.Type.SURFING)) return new Surf(activity);
        else if(type.equalsIgnoreCase(StravaStrings.Type.WATER_SPORT) && sport.equalsIgnoreCase(StravaStrings.SportType.SURFING)) return new Surf(activity);
        else if(type.equalsIgnoreCase(StravaStrings.Type.STAND_UP_PADDLING)) return new StandUpPaddling(activity);
        else if(type.equalsIgnoreCase(StravaStrings.Type.WEIGHT_TRAINING)) return new WeightTraining(activity);
        else if(type.equalsIgnoreCase(StravaStrings.Type.WORKOUT)) return new Workout(activity);
        else if(type.equalsIgnoreCase(StravaStrings.Type.ROWING) && sport.equalsIgnoreCase(StravaStrings.SportType.VIRTUAL_ROW)) return new VirtualRow(activity);
        else if(type.equalsIgnoreCase(StravaStrings.Type.KAYAKING)) return new Kayak(activity);
        else if(type.equalsIgnoreCase(StravaStrings.Type.SOCCER)) return new Soccer(activity);
        else if(type.equalsIgnoreCase(StravaStrings.Type.RIDE) && sport.equalsIgnoreCase(StravaStrings.SportType.MOUNTAIN_BIKE_RIDE)) return new MTB(activity);
        else if(type.equalsIgnoreCase(StravaStrings.Type.RIDE) && sport.equalsIgnoreCase(StravaStrings.SportType.GRAVEL_RIDE)) return new GravelRide(activity);
        else if(type.equalsIgnoreCase(StravaStrings.Type.RIDE) && sport.equalsIgnoreCase(StravaStrings.SportType.RIDE)) return new RoadBike(activity);
        else if(type.equalsIgnoreCase(StravaStrings.Type.VIRTUAL_RIDE)) return new VirtualRide(activity);
        else if(type.equalsIgnoreCase(StravaStrings.Type.EBIKE_RIDE) && sport.equalsIgnoreCase(StravaStrings.SportType.E_MOUNTAIN_BIKE_RIDE)) return new EMountainBikeRide(activity);
        else if(type.equalsIgnoreCase(StravaStrings.Type.EBIKE_RIDE) && sport.equalsIgnoreCase(StravaStrings.SportType.EBIKE_RIDE)) return new EBikeRideRoad(activity);
        return null;
    }
}
