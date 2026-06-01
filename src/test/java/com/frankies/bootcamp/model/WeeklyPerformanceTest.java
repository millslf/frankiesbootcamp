package com.frankies.bootcamp.model;

import com.frankies.bootcamp.model.strava.StravaActivityResponse;
import com.frankies.bootcamp.sport.Run;
import com.frankies.bootcamp.sport.TrailRun;
import com.frankies.bootcamp.sport.Walk;
import com.frankies.bootcamp.sport.Workout;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WeeklyPerformanceTest {

    @Test
    void keepsSameGoalWhenPreviousWeekIsWithinExpectedRange() {
        WeeklyPerformance weeklyPerformance = new WeeklyPerformance("Week2", 0L, 20.0, 20.0);

        assertEquals(20.0, weeklyPerformance.getWeekGoal());
    }

    @Test
    void increasesGoalWhenPreviousWeekStronglyExceededGoal() {
        WeeklyPerformance weeklyPerformance = new WeeklyPerformance("Week2", 0L, 20.0, 24.0);

        assertEquals(22.0, weeklyPerformance.getWeekGoal());
    }

    @Test
    void decreasesGoalWhenPreviousWeekWasWellBelowGoal() {
        WeeklyPerformance weeklyPerformance = new WeeklyPerformance("Week2", 0L, 20.0, 8.0);

        assertEquals(18.0, weeklyPerformance.getWeekGoal());
    }

    @Test
    void scoresExactlyOnePointWhenGoalIsMet() {
        WeeklyPerformance weeklyPerformance = new WeeklyPerformance("Week1", 0L, 20.0, -1.0);

        weeklyPerformance.addSports(run(20.0));

        assertEquals(20.0, weeklyPerformance.getTotalDistance(), 0.0001);
        assertEquals(1.0, weeklyPerformance.getTotalPercentOfGoal(), 0.0001);
        assertEquals(1.0, weeklyPerformance.getWeekScore(), 0.0001);
    }

    @Test
    void addsProgressionBonusWhenGoalIsExceeded() {
        WeeklyPerformance weeklyPerformance = new WeeklyPerformance("Week1", 0L, 20.0, -1.0);

        weeklyPerformance.addSports(run(18.0));
        weeklyPerformance.addSports(trailRun(5.0));

        assertEquals(24.0, weeklyPerformance.getTotalDistance(), 0.0001);
        assertEquals(1.2, weeklyPerformance.getTotalPercentOfGoal(), 0.0001);
        assertEquals(1.2, weeklyPerformance.getWeekScore(), 0.0001);
    }

    @Test
    void usesAverageScoreWhenMarkedSick() {
        WeeklyPerformance weeklyPerformance = new WeeklyPerformance("Week1", 0L, 20.0, -1.0);

        weeklyPerformance.addSports(run(6.0));
        weeklyPerformance.setAverageWeeklyScore(2.0, 2);
        weeklyPerformance.setIsSick(true);

        assertEquals(1.0, weeklyPerformance.getWeekScore(), 0.0001);
    }

    @Test
    void removeSportsReversesTotalsCountsAndOriginalValues() {
        WeeklyPerformance weeklyPerformance = new WeeklyPerformance("Week1", 0L, 20.0, -1.0);

        Walk walk = walk(4.0);
        Workout workout = workout(0.8);

        weeklyPerformance.addSports(walk);
        weeklyPerformance.addSports(workout);
        weeklyPerformance.removeSports(walk);

        assertEquals(4.0, weeklyPerformance.getTotalDistance(), 0.0001);
        assertEquals(1, weeklyPerformance.getSportsCount().get("Workout"));
        assertEquals(4.0, weeklyPerformance.getSports().get("Workout"), 0.0001);
        assertEquals(0.8, weeklyPerformance.getSportsOriginalDuration().get("Workout"), 0.0001);
        assertEquals(0.2, weeklyPerformance.getWeekScore(), 0.0001);
    }

    private static Run run(double distance) {
        StravaActivityResponse activity = new StravaActivityResponse();
        activity.setDistance(distance * 1000);
        return new Run(activity);
    }

    private static TrailRun trailRun(double distance) {
        StravaActivityResponse activity = new StravaActivityResponse();
        activity.setDistance(distance * 1000);
        return new TrailRun(activity);
    }

    private static Walk walk(double distance) {
        StravaActivityResponse activity = new StravaActivityResponse();
        activity.setDistance(distance * 1000);
        return new Walk(activity);
    }

    private static Workout workout(double hours) {
        StravaActivityResponse activity = new StravaActivityResponse();
        activity.setMoving_time((int) Math.round(hours * 3600));
        return new Workout(activity);
    }
}
