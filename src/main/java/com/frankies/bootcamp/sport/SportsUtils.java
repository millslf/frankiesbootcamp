package com.frankies.bootcamp.sport;

import com.frankies.bootcamp.model.strava.StravaActivityResponse;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SportsUtils {
    public static Set<String> getAllSportTypes() {
        Set<String> sportTypes = new HashSet<>();
        for (String[] mapping : Arrays.asList(
                new String[]{"Run", "Run"},
                new String[]{"Run", "TrailRun"},
                new String[]{"Swim", "Swim"},
                new String[]{"Walk", "Walk"},
                new String[]{"Hike", "Hike"},
                new String[]{"Golf", "Golf"},
                new String[]{"Surfing", "Surfing"},
                new String[]{"WaterSport", "Surfing"},
                new String[]{"StandUpPaddling", "StandUpPaddling"},
                new String[]{"WeightTraining", "WeightTraining"},
                new String[]{"Elliptical", "Elliptical"},
                new String[]{"Workout", "Workout"},
                new String[]{"Rowing", "VirtualRow"},
                new String[]{"Kayaking", "Kayaking"},
                new String[]{"Soccer", "Soccer"},
                new String[]{"Ride", "MountainBikeRide"},
                new String[]{"Ride", "GravelRide"},
                new String[]{"Ride", "Ride"},
                new String[]{"VirtualRide", "VirtualRide"},
                new String[]{"EBikeRide", "EMountainBikeRide"},
                new String[]{"EBikeRide", "EBikeRide"}
        )) {
            BaseSport sport = SportFactory.getSport(createActivity(mapping[0], mapping[1]));
            if (sport != null && sport.getSportType() != null && !sport.getSportType().isBlank()) {
                sportTypes.add(sport.getSportType());
            }
        }
        return sportTypes;
    }

    public static String getFavouriteSports(Map<String, Double> sports, int topN) {
        if (sports == null || sports.isEmpty()) return "";
        return sports.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(topN)
                .map(Map.Entry::getKey)
                .collect(Collectors.joining(", "));
    }

    public static String getSuggestedSport(Set<String> triedSports) {
        Set<String> allSports = getAllSportTypes();
        if (triedSports != null) {
            allSports.removeAll(triedSports);
        }
        return allSports.stream().findAny().orElse("");
    }

    private static StravaActivityResponse createActivity(String type, String sportType) {
        StravaActivityResponse activity = new StravaActivityResponse();
        activity.setType(type);
        activity.setSport_type(sportType);
        activity.setDistance(0.0);
        activity.setMoving_time(0);
        return activity;
    }
}
