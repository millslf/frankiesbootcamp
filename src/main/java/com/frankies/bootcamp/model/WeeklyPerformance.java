package com.frankies.bootcamp.model;

import com.frankies.bootcamp.sport.BaseSport;
import com.frankies.bootcamp.sport.DistanceSport;
import com.frankies.bootcamp.sport.DurationSport;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

public class WeeklyPerformance {
    private final String week;
    private final Double weekGoal;
    private final Map<String, Double> sports;
    private final Map<String, Integer> sportsCount;
    private final Map<String, Double> sportsOriginalDistance;
    private final Map<String, Double> sportsOriginalDuration;
    private Double totalDistance = 0.0;
    private Double totalPercentOfGoal = 0.0;
    private Double weekScore = 0.0;
    private Double weekProgressionBonus = 0.0;
    private Double weekGoalAchievementScore = 0.0;
    private boolean isSick;
    private Double averageWeeklyScore = 0.0;
    public WeeklyPerformance(String week, Long weekEnding, Double previousWeekGoal, Double previousWeekTotalDistance) {
        sportsOriginalDistance = new HashMap<>();
        sportsOriginalDuration = new HashMap<>();
        sports = new HashMap<>();
        sportsCount = new HashMap<>();
        this.week = week;
        this.weekGoal = calculateWeekGoal(previousWeekGoal, previousWeekTotalDistance);
    }

    public Double getTotalDistance() {
        return totalDistance;
    }

    public String getWeek() {
        return week;
    }

    public Double getWeekGoal() {
        return weekGoal;
    }

    public Double getWeekScore() {
        return weekScore;
    }

    public Double getWeekProgressionBonus() {
        return weekProgressionBonus;
    }

    public Double getWeekGoalAchievementScore() {
        return weekGoalAchievementScore;
    }

    public Map<String, Double> getSports() {
        return sports;
    }

    public Map<String, Integer> getSportsCount() {
        return sportsCount;
    }

    public Map<String, Double> getSportsOriginalDistance() {
        return sportsOriginalDistance;
    }

    public Map<String, Double> getSportsOriginalDuration() {
        return sportsOriginalDuration;
    }

    public Double getTotalPercentOfGoal() {
        return totalPercentOfGoal;
    }

    public void addSports(BaseSport sport) {
        totalDistance += sport.getCalculatedDistance();
        if(sports.containsKey(sport.getSportType())) {
            sports.put(sport.getSportType(), sports.get(sport.getSportType()) + sport.getCalculatedDistance());
            sportsCount.put(sport.getSportType(), sportsCount.get(sport.getSportType()) + 1);
            if(sport instanceof DurationSport){
                sportsOriginalDuration.put(sport.getSportType(),
                        sportsOriginalDuration.get(sport.getSportType()) + ((DurationSport) sport).getOriginalDuration());
            }else if(sport instanceof DistanceSport){
                sportsOriginalDistance.put(sport.getSportType(),
                        sportsOriginalDistance.get(sport.getSportType()) + ((DistanceSport) sport).getOriginalDistance());
            }
        }else{
            sports.put(sport.getSportType(), sport.getCalculatedDistance());
            sportsCount.put(sport.getSportType(), 1);
            if(sport instanceof DurationSport) {
                sportsOriginalDuration.put(sport.getSportType(),((DurationSport) sport).getOriginalDuration());
            }else if(sport instanceof DistanceSport){
                sportsOriginalDistance.put(sport.getSportType(), ((DistanceSport) sport).getOriginalDistance());
            }
        }
        calculateWeekScore();
        calculateTotalPercentOfGoal();
    }

    public void setIsSick(boolean isSick){
        this.isSick = isSick;
        if(this.isSick){
            calculateWeekScore();
            calculateTotalPercentOfGoal();
        }
    }

    public boolean isSick() {
        return isSick;
    }

    public void setAverageWeeklyScore(Double currentScore, int numberOfWeeksSinceStart) {
        if(numberOfWeeksSinceStart > 0) {
            this.averageWeeklyScore = currentScore / numberOfWeeksSinceStart;
        }else{
            this.averageWeeklyScore = weekScore;
        }
    }

    @Override
    public String toString() {
        DecimalFormat df = new DecimalFormat("#.##");
        StringBuilder sb = new StringBuilder();
        sb.append(week).append(":\n");
        sb.append("Distance this week: ").append(df.format(totalDistance)).append("km\n");
        sb.append("Goal this week: ").append(df.format(getWeekGoal())).append("km\n");
        sb.append("Percentage of goal completed this week: ").append(df.format(totalPercentOfGoal * 100)).append("%\n");
        sb.append("Points scored this week: ").append(df.format(getWeekScore())).append("\n");
        if(!sports.isEmpty()) {
            for (String key : sports.keySet()) {
                sb.append("\t").append(key).append("(x").append(sportsCount.get(key)).append(") ").append(" : ").append(df.format(sports.get(key))).append("km\n");
            }
        }
        return sb.append("\n").toString();
    }

    private double calculateWeekGoal(double previousWeekGoal, double previousWeekTotalDistance) {
        double ratio = previousWeekTotalDistance / previousWeekGoal;
        double weekGoal;
        if(previousWeekTotalDistance == -1.0) {
            return previousWeekGoal;
        }
        if (ratio < 0.50) {
            weekGoal = previousWeekGoal * 0.90;
        } else if (ratio < 0.80) {
            weekGoal = previousWeekGoal * 0.95;
        } else if (ratio < 1.10) {
            weekGoal = previousWeekGoal;
        } else if (ratio < 1.15) {
            weekGoal = previousWeekGoal * 1.03;
        } else if (ratio < 1.20) {
            weekGoal = previousWeekGoal * 1.07;
        } else if (ratio < 1.50) {
            weekGoal = previousWeekGoal * 1.1;
        } else if (ratio < 1.80) {
            weekGoal = previousWeekGoal * 1.15;
        } else {
            weekGoal = previousWeekGoal * 1.2;
        }

        return roundGoal(weekGoal);
    }

    private double roundGoal(double goal) {
        return Math.round(goal * 2.0) / 2.0; // nearest 0.5
    }

    private void calculateWeekScore() {
        if (isSick) {
            this.weekGoalAchievementScore = averageWeeklyScore;
            this.weekProgressionBonus = 0.0;
            this.weekScore = averageWeeklyScore;
            return;
        }

        if (weekGoal <= 0.0) {
            this.weekGoalAchievementScore = 0.0;
            this.weekProgressionBonus = 0.0;
            this.weekScore = 0.0;
            return;
        }

        double ratio = totalDistance / weekGoal;

        // Achievement: count actual percentage up to 110% (so 101% -> 1.01). Cap at 1.10 so progression bonuses stack on top.
        this.weekGoalAchievementScore = Math.min(ratio, 1.10);

        // Progression bonus tiers (unchanged) — applied on top of the achievement score for >=110%
        if (ratio < 1.1) {
            this.weekProgressionBonus = 0.0;
        } else if (ratio < 1.15) {
            this.weekProgressionBonus = 0.03;
        } else if (ratio < 1.20) {
            this.weekProgressionBonus = 0.07;
        } else if (ratio < 1.50) {
            this.weekProgressionBonus = 0.1;
        } else if (ratio < 1.80) {
            this.weekProgressionBonus = 0.15;
        } else {
            this.weekProgressionBonus = 0.2;
        }

        this.weekScore = this.weekGoalAchievementScore + this.weekProgressionBonus;
    }

    private void calculateTotalPercentOfGoal(){
        if(totalDistance == 0.0) {
            totalPercentOfGoal = 0.0;
        } else {
            totalPercentOfGoal = totalDistance / weekGoal;
        }
    }

    public void removeSports(BaseSport sport) {
        if (sport == null) return;
        final String key = sport.getSportType();
        if (!sports.containsKey(key)) return;

        double calc = sport.getCalculatedDistance();
        totalDistance = Math.max(0.0, totalDistance - calc);

        double newDist = sports.get(key) - calc;
        int newCount   = sportsCount.getOrDefault(key, 0) - 1;

        if (newDist <= 0.0 || newCount <= 0) {
            sports.remove(key);
            sportsCount.remove(key);
            if (sport instanceof DurationSport) {
                double d = sportsOriginalDuration.getOrDefault(key, 0.0)
                        - ((DurationSport) sport).getOriginalDuration();
                if (d <= 0.0) sportsOriginalDuration.remove(key); else sportsOriginalDuration.put(key, d);
            } else if (sport instanceof DistanceSport) {
                double d = sportsOriginalDistance.getOrDefault(key, 0.0)
                        - ((DistanceSport) sport).getOriginalDistance();
                if (d <= 0.0) sportsOriginalDistance.remove(key); else sportsOriginalDistance.put(key, d);
            }
        } else {
            sports.put(key, newDist);
            sportsCount.put(key, newCount);
            if (sport instanceof DurationSport) {
                sportsOriginalDuration.put(key,
                        Math.max(0.0, sportsOriginalDuration.getOrDefault(key, 0.0)
                                - ((DurationSport) sport).getOriginalDuration()));
            } else if (sport instanceof DistanceSport) {
                sportsOriginalDistance.put(key,
                        Math.max(0.0, sportsOriginalDistance.getOrDefault(key, 0.0)
                                - ((DistanceSport) sport).getOriginalDistance()));
            }
        }

        calculateWeekScore();
        calculateTotalPercentOfGoal();
    }

    public void setPersistedValues(Double totalDistance,
                                   Double totalPercentOfGoal,
                                   Double weekGoalAchievementScore,
                                   Double weekProgressionBonus,
                                   Double weekScore,
                                   Double averageWeeklyScore,
                                   boolean isSick) {
        this.totalDistance = totalDistance == null ? 0.0 : totalDistance;
        this.totalPercentOfGoal = totalPercentOfGoal == null ? 0.0 : totalPercentOfGoal;
        this.weekGoalAchievementScore = weekGoalAchievementScore == null ? 0.0 : weekGoalAchievementScore;
        this.weekProgressionBonus = weekProgressionBonus == null ? 0.0 : weekProgressionBonus;
        this.weekScore = weekScore == null ? 0.0 : weekScore;
        this.averageWeeklyScore = averageWeeklyScore == null ? 0.0 : averageWeeklyScore;
        this.isSick = isSick;
    }

    public void setPersistedSportTotals(String sportType,
                                        Integer activityCount,
                                        Double calculatedDistance,
                                        Double originalDistance,
                                        Double originalDuration) {
        if (sportType == null || sportType.isBlank()) {
            return;
        }

        sports.put(sportType, calculatedDistance == null ? 0.0 : calculatedDistance);
        sportsCount.put(sportType, activityCount == null ? 0 : activityCount);
        if (originalDistance != null) {
            sportsOriginalDistance.put(sportType, originalDistance);
        }
        if (originalDuration != null) {
            sportsOriginalDuration.put(sportType, originalDuration);
        }
    }

}
