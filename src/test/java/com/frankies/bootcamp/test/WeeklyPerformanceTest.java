package com.frankies.bootcamp.test;

import com.frankies.bootcamp.constant.BootcampConstants;
import com.frankies.bootcamp.model.WeeklyPerformance;
import com.frankies.bootcamp.model.strava.StravaActivityResponse;
import com.frankies.bootcamp.sport.Run;
import com.frankies.bootcamp.sport.Surf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WeeklyPerformanceTest {

    private static final double EPS = 1e-9;

    /* ---------- helpers ---------- */

    private static StravaActivityResponse distAct(String type, String sportType, double km) {
        StravaActivityResponse a = new StravaActivityResponse();
        a.setType(type);
        a.setSport_type(sportType);
        a.setDistance(km * 1000.0);      // Strava uses meters
        a.setElapsed_time(1800);
        return a;
    }

    private static StravaActivityResponse durAct(String type, String sportType, int seconds) {
        StravaActivityResponse a = new StravaActivityResponse();
        a.setType(type);
        a.setSport_type(sportType);
        a.setElapsed_time(seconds);
        a.setDistance(0.0);              // not used for duration sports
        return a;
    }

    private static WeeklyPerformance wpWithGoal(double baseGoalKm, double prevWeekTotal) {
        long weekEnding = BootcampConstants.WEEK_IN_SECONDS * 2L; // any epoch-sec is fine
        return new WeeklyPerformance("WeekX", weekEnding, baseGoalKm, prevWeekTotal);
    }

    /* ---------- goal adjustment rules ---------- */

    @Test
    void goal_initial_usesPreviousGoal() {
        WeeklyPerformance wp = wpWithGoal(50.0, -1.0);
        assertEquals(50.0, wp.getWeekGoal(), EPS);
    }

    @Test
    void goal_increasesBy20pct_whenPrevTotalOver2x() {
        WeeklyPerformance wp = wpWithGoal(50.0, 101.0); // > 100
        assertEquals(60.0, wp.getWeekGoal(), EPS);
    }

    @Test
    void goal_increasesBy10pct_whenPrevTotalOver1_5x() {
        WeeklyPerformance wp = wpWithGoal(50.0, 76.0); // > 75 and <= 100
        assertEquals(55.0, wp.getWeekGoal(), EPS);
    }

    @Test
    void goal_decreasesBy10pct_whenPrevTotalUnderHalf() {
        WeeklyPerformance wp = wpWithGoal(50.0, 24.0); // < 25
        assertEquals(45.0, wp.getWeekGoal(), EPS);
    }

    @Test
    void goal_unchanged_whenWithinHalfToTwoX() {
        WeeklyPerformance wp = wpWithGoal(50.0, 50.0); // between 25 and 100 inclusive of edges not covered above
        assertEquals(50.0, wp.getWeekGoal(), EPS);
    }

    /* ---------- distance sport aggregation + scoring thresholds ---------- */

    @Test
    void addDistanceSports_accumulatesTotals_counts_originals_andScores() {
        // Base weekly goal 50 km (initial week: prevTotal = -1 => goal = 50)
        WeeklyPerformance wp = wpWithGoal(50.0, -1.0);

        // 1) Add a 10 km Run (multiplier 1.0)
        Run r10 = new Run(distAct("Run", "Run", 10.0));
        wp.addSports(r10);

        assertEquals(10.0, wp.getTotalDistance(), EPS);
        assertEquals(10.0, wp.getSports().get("Run"), EPS);
        assertEquals(1, wp.getSportsCount().get("Run"));
        assertEquals(10.0, wp.getSportsOriginalDistance().get("Run"), EPS);
        assertEquals(0.0, wp.getWeekScore(), EPS, "10 < 25 (0.5*goal) -> score 0.0");
        assertEquals(10.0 / 50.0, wp.getTotalPercentOfGoal(), EPS);

        // 2) Add another 20 km → total 30 (< goal but >= 0.5*goal) => score 0.5
        Run r20 = new Run(distAct("Run", "Run", 20.0));
        wp.addSports(r20);
        assertEquals(30.0, wp.getTotalDistance(), EPS);
        assertEquals(2, wp.getSportsCount().get("Run"));
        assertEquals(30.0, wp.getSports().get("Run"), EPS);
        assertEquals(30.0, wp.getSportsOriginalDistance().get("Run"), EPS);
        assertEquals(0.5, wp.getWeekScore(), EPS);
        assertEquals(30.0 / 50.0, wp.getTotalPercentOfGoal(), EPS);

        // 3) Add 20 km → total 50 == goal -> score 1.0
        Run r20b = new Run(distAct("Run", "Run", 20.0));
        wp.addSports(r20b);
        assertEquals(50.0, wp.getTotalDistance(), EPS);
        assertEquals(1.0, wp.getWeekScore(), EPS);

        // 4) Add 30 km → total 80 > 1.5*goal (75) and <= 2*goal (100) -> score 1.5
        Run r30 = new Run(distAct("Run", "Run", 30.0));
        wp.addSports(r30);
        assertEquals(80.0, wp.getTotalDistance(), EPS);
        assertEquals(1.5, wp.getWeekScore(), EPS);

        // 5) Add 30 km → total 110 > 2*goal -> score 1.75 (max)
        Run r30b = new Run(distAct("Run", "Run", 30.0));
        wp.addSports(r30b);
        assertEquals(110.0, wp.getTotalDistance(), EPS);
        assertEquals(1.75, wp.getWeekScore(), EPS);
        assertEquals(110.0 / 50.0, wp.getTotalPercentOfGoal(), EPS);
    }

    /* ---------- duration sport aggregation ---------- */

    @Test
    void addDurationSport_tracksOriginalDuration_andCalculatedDistance() {
        // goal 50 km
        WeeklyPerformance wp = wpWithGoal(50.0, -1.0);

        // Surf is duration-based: distance = hours * 7.5
        // 2 hours => 15.0 km
        Surf s2h = new Surf(durAct("Surfing", "Surfing", 7200));
        wp.addSports(s2h);

        assertEquals(15.0, wp.getTotalDistance(), EPS);
        assertEquals(15.0, wp.getSports().get("Surf"), EPS);
        assertEquals(1, wp.getSportsCount().get("Surf"));
        assertEquals(2.0, wp.getSportsOriginalDuration().get("Surf"), EPS);
        assertEquals(15.0 / 50.0, wp.getTotalPercentOfGoal(), EPS);
    }

    /* ---------- sickness override ---------- */

    @Test
    void sicknessOverride_setsWeekScoreToAverageWeeklyScore() {
        WeeklyPerformance wp = wpWithGoal(50.0, -1.0);

        // Add 50 km to get a "normal" score of 1.0
        wp.addSports(new Run(distAct("Run", "Run", 50.0)));
        assertEquals(1.0, wp.getWeekScore(), EPS);

        // Suppose historical average across previous weeks is 1.23
        wp.setAverageWeeklyScore(12.3, 10); // 12.3 / 10 = 1.23
        wp.setIsSick(true);                 // triggers score override

        assertTrue(wp.isSick());
        assertEquals(1.23, wp.getWeekScore(), EPS, "sick weeks should use averageWeeklyScore");
    }

    /* ---------- formatting sanity (optional) ---------- */

    @Test
    void toString_containsKeyFields() {
        WeeklyPerformance wp = wpWithGoal(50.0, -1.0);
        wp.addSports(new Run(distAct("Run", "Run", 12.345))); // 12.35 rounded in output

        String text = wp.toString();
        assertTrue(text.contains("WeekX:"), "should include week label");
        assertTrue(text.contains("Distance this week: 12.35km"), "rounded distance appears");
        assertTrue(text.contains("Goal this week: 50"), "goal appears");
        assertTrue(text.contains("Run(x1)"), "sport and count appear");
    }
}

