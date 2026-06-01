package com.frankies.bootcamp.sport;

import com.frankies.bootcamp.model.strava.StravaActivityResponse;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
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
        if (allSports == null || allSports.isEmpty()) return "";

        // Build a normalized map so comparisons ignore case/spacing/punctuation
        Map<String, String> normalizedToOriginal = new LinkedHashMap<>();
        for (String s : allSports) {
            if (s == null) continue;
            normalizedToOriginal.put(normalizeSport(s), s);
        }

        // Normalize tried sports so mismatched labels don't cause accidental misses
        Set<String> normalizedTried = new HashSet<>();
        if (triedSports != null) {
            for (String t : triedSports) {
                if (t == null) continue;
                normalizedTried.add(normalizeSport(t));
            }
        }

        // Collect candidates (those not tried)
        List<String> candidates = normalizedToOriginal.entrySet().stream()
                .filter(e -> !normalizedTried.contains(e.getKey()))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());

        if (candidates.isEmpty()) return "";

        // Pick a random candidate to avoid deterministic repeats
        int idx = new Random().nextInt(candidates.size());
        return candidates.get(idx);
    }

    private static String normalizeSport(String s) {
        if (s == null) return "";
        return s.replaceAll("[^A-Za-z0-9]", "").toLowerCase();
    }

    private static StravaActivityResponse createActivity(String type, String sportType) {
        StravaActivityResponse activity = new StravaActivityResponse();
        activity.setType(type);
        activity.setSport_type(sportType);
        activity.setDistance(0.0);
        activity.setElapsed_time(0);
        activity.setMoving_time(0);
        return activity;
    }
}
