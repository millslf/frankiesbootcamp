package com.frankies.bootcamp.servlet;

import com.frankies.bootcamp.model.WeeklyPerformance;
import com.frankies.bootcamp.service.ActivityProcessFacade;
import com.frankies.bootcamp.utils.DateTimeUtils;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.frankies.bootcamp.service.AiMessageService;

@WebServlet(name = "athleteHistory", value = "/app/AthleteHistory")
public class AthleteHistoryServlet extends BootcampServlet {
    private static final Logger log = Logger.getLogger(AthleteHistoryServlet.class);

    @Inject
    private ActivityProcessFacade activityProcessFacade;
    @Inject
    private AiMessageService aiMessageService;

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        DecimalFormat df = new DecimalFormat("#.##");
        Map<Integer, WeeklyPerformance> history = new HashMap<>();
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        com.frankies.bootcamp.model.BootcampAthlete loggedInAthlete = (com.frankies.bootcamp.model.BootcampAthlete) request.getAttribute("athlete");
        String athleteName = loggedInAthlete.getFirstname();
        String authenticatedUserMail = (String) request.getAttribute("athleteEmail");
        history = activityProcessFacade.getAthleteHistory(loggedInAthlete.getId());
        if (history == null) {
            history = new HashMap<>();
        }
        int numberOfWeeksSinceStart = activityProcessFacade.getNumberOfWeeksSinceStart();

        out.println("<html><head>");
        out.println("</head><body>");

        out.println("<div class='container'>");

        out.println("<h2 class='history-heading'>");
        out.println("<i class='bi bi-clock-history'></i> My Weekly Competition History");
        out.println("</h2>");
        out.println("<p class='history-subheading'><i class='bi bi-graph-up-arrow'></i> Track how you performed each week across all activities, see your progress, week by week!</p>");

        if (history.isEmpty()) {
            out.println("<div class='alert alert-info'>We are getting things ready for your Strava account. Your history should appear shortly.</div>");
            out.println("</div>");
            out.println("</body></html>");
            return;
        }

        WeeklyPerformance latest = history.get(numberOfWeeksSinceStart);
        if (latest != null) {
            double distance = latest.getTotalDistance();
            double goal = latest.getWeekGoal();
            boolean hitGoal = latest.getTotalPercentOfGoal() >= 1.0;

            Integer leaderboardRank = null;
            try {
                Map<String, HashMap<String, Double>> sortedSummaries = activityProcessFacade.getSortedSummaries();
                int rank = 1;
                for (String email : sortedSummaries.keySet()) {
                    if (email.equalsIgnoreCase(authenticatedUserMail)) {
                        leaderboardRank = rank;
                        break;
                    }
                    rank++;
                }
            } catch (RuntimeException e) {
                log.warn("Unable to calculate leaderboard rank for " + authenticatedUserMail, e);
            }

            Map<String, Double> sports = latest.getSports() != null ? latest.getSports() : new LinkedHashMap<>();
            String favouriteSports = com.frankies.bootcamp.sport.SportsUtils.getFavouriteSports(sports, 2);
            String suggestedSport = com.frankies.bootcamp.sport.SportsUtils.getSuggestedSport(sports.keySet());

            String aiMsg = aiMessageService.getMotivationalMessage(
                athleteName, distance, goal, hitGoal, leaderboardRank, favouriteSports, suggestedSport
            );
            out.println("<div class='alert alert-info history-subheading' style='margin-bottom:1em;'>" + aiMsg + "</div>");
        }
        out.println("<div class='table-responsive mt-4'>");
        out.println("<table class='table table-bordered table-striped align-middle'>");
        out.println("<thead class='table-dark'>");
        out.println("<tr>");
        out.println("<th><i class='bi bi-calendar'></i> Week</th>");
        out.println("<th><i class='bi bi-rulers'></i> Distance completed</th>");
        out.println("<th class='col-goal'><i class='bi bi-bullseye'></i> Goal</th>");
        out.println("<th><i class='bi bi-graph-up'></i> % of goal</th>");
        out.println("<th><i class='bi bi-signpost'></i> Distance left</th>");
        out.println("<th><i class='bi bi-star-fill'></i> Progression Points scored</th>");
        out.println("<th><i class='bi bi-star-fill'></i> Goal Points scored</th>");
        out.println("<th><i class='bi bi-star-fill'></i> Total Points scored</th>");
        out.println("<th class='col-activities'><i class='bi bi-activity'></i> Activities</th>");
        out.println("</tr>");
        out.println("</thead>");
        out.println("<tbody>");
        for (int i = numberOfWeeksSinceStart; i >= 1; i--) {
            WeeklyPerformance weekHistory = history.get(i);
            if (weekHistory == null) {
                continue;
            }
            out.println("<tr>");
            String weekText = weekHistory.getWeek();
            if(weekHistory.isSick()){
                weekText += " <i class='bi bi-heart-pulse-fill' title='Sick Note'></i>";
            }
            if (i == numberOfWeeksSinceStart) {
                out.println("<td>" + weekText +
                        "</br><i>(Week in progress)" + "</td>");
            } else {
                out.println("<td>" + weekText + "</td>");
            }
            out.println("<td>" + df.format(weekHistory.getTotalDistance()) + " km </td>");
            out.println("<td>" + df.format(weekHistory.getWeekGoal()) + "km</td>");
            out.println("<td>" + df.format(weekHistory.getTotalPercentOfGoal() * 100) + "%</td>");
            out.println("<td>" + df.format(weekHistory.getWeekGoal()-weekHistory.getTotalDistance()>0?weekHistory.getWeekGoal()-weekHistory.getTotalDistance():0) + "km</td>");
            if(weekHistory.isSick()){
                out.println("<td> <i class='bi bi-heart-pulse-fill' title='Sick Note'></i></td>");
                out.println("<td> <i class='bi bi-heart-pulse-fill' title='Sick Note'></i></td>");
            }
            else{
                out.println("<td>" + df.format(weekHistory.getWeekProgressionBonus()) + "</td>");
                out.println("<td>" + df.format(weekHistory.getWeekGoalAchievementScore()) + "</td>");
            }
            out.println("<td>" + df.format(weekHistory.getWeekScore()) + "</td>");
            out.println("<td>");
            for (String key : weekHistory.getSports().keySet()) {
                out.println(key + "(x" + weekHistory.getSportsCount().get(key) + ") " + df.format(weekHistory.getSports().get(key)) + "km");
                if (weekHistory.getSportsOriginalDistance().containsKey(key)) {
                    out.println(" (" + df.format(weekHistory.getSportsOriginalDistance().get(key)) + "km)</br>");
                } else if (weekHistory.getSportsOriginalDuration().containsKey(key)) {
                    out.println(" (" + DateTimeUtils.convertMinutesToTimeFormat(weekHistory.getSportsOriginalDuration().get(key)*60) + ")</br>");
                }
            }
            out.println("</td>");
        }
        out.println("</tbody>");
        out.println("</table>");
        out.println("</div>");
        out.println("</div>");
        out.println("</body></html>");
    }
}
