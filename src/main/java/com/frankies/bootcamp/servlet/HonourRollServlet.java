package com.frankies.bootcamp.servlet;

import com.frankies.bootcamp.constant.BootcampConstants;
import com.frankies.bootcamp.model.BootcampAthlete;
import com.frankies.bootcamp.model.PerformanceResponse;
import com.frankies.bootcamp.service.ActivityProcessFacade;
import com.frankies.bootcamp.utils.WildflyUtils;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet(name = "honourRoll", value = "/app/HonourRoll")
public class HonourRollServlet extends BootcampServlet {
    @Inject
    private ActivityProcessFacade activityProcessFacade;
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        DecimalFormat df = new DecimalFormat("#.##");
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        com.frankies.bootcamp.model.BootcampAthlete loggedInAthlete = (com.frankies.bootcamp.model.BootcampAthlete) request.getAttribute("athlete");
        String athleteId = loggedInAthlete == null ? null : loggedInAthlete.getId();
        Long selectedCompetitionId = (Long) request.getAttribute("selectedCompetitionId");
        HashMap<Integer, HashMap<String, Double>> percOfGoal = selectedCompetitionId != null
                ? activityProcessFacade.getHonourRollPercentageOfGoalForCompetition(selectedCompetitionId)
                : activityProcessFacade.getHonourRollPercentageOfGoal(athleteId);
        HashMap<Integer, HashMap<String, Double>> totalDist = selectedCompetitionId != null
                ? activityProcessFacade.getHonourRollTotalDistanceForCompetition(selectedCompetitionId)
                : activityProcessFacade.getHonourRollTotalDistance(athleteId);
        Map<String, String> athleteIdsByName = athleteIdsByName(selectedCompetitionId);
        int numberOfWeeksSinceStart = activityProcessFacade.getNumberOfWeeksSinceStart();

        out.println("<div class='container'>");
        out.println("<h2 class='history-heading'>");
        out.println("<i class='bi bi-award-fill'></i> Bootcamp Honour Roll: Finishers & Front-Runners");
        out.println("</h2>");
        out.println("<p class='history-subheading'>");
        out.println("🎯 Recognising weekly legends who pushed furthest and hit their goals.");
        out.println("</p>");
        out.println("<div class='table-responsive mt-4'>");
        out.println("<table class='table table-bordered table-striped align-middle'>");
        out.println("<thead class='table-dark'>");
        out.println("    <tr>");
        out.println("      <th><i class='bi bi-calendar'></i> Week</th>");
        out.println("      <th><i class='bi bi-rulers'></i> Distance Leader</th>");
        out.println("      <th><i class='bi bi-graph-up'></i> Percentage of Goal</th>");
        out.println("    </tr>");
        out.println("  </thead>");
        out.println("  <tbody>");
        for (int i = 1; i < numberOfWeeksSinceStart; i++) {
            HashMap<String, Double> totalDistanceWeek = totalDist.get(i);
            HashMap<String, Double> totalPercentWeek = percOfGoal.get(i);
            if (totalDistanceWeek == null || totalDistanceWeek.isEmpty() || totalPercentWeek == null || totalPercentWeek.isEmpty()) {
                continue;
            }
            out.println("<tr>");
            out.println("<td>Week " + i + "</td>");
            Map.Entry<String, Double> totalDistance = totalDistanceWeek.entrySet().iterator().next();
            Map.Entry<String, Double> totalPercent = totalPercentWeek.entrySet().iterator().next();
            out.println("<td>" + athleteProfileLink(totalDistance.getKey(), athleteIdsByName) + "</td>");
            out.println("<td>" + athleteProfileLink(totalPercent.getKey(), athleteIdsByName) + " " + df.format(totalPercent.getValue() * 100) + "%</td>");
            out.println("</tr>");
        }
        out.println("  </tbody>");
        out.println("</table>");
        out.println("</div>");
        out.println("</div>");
    }

    private Map<String, String> athleteIdsByName(Long selectedCompetitionId) {
        Map<String, String> athleteIdsByName = new HashMap<>();
        if (selectedCompetitionId == null) {
            return athleteIdsByName;
        }
        List<PerformanceResponse> performances = activityProcessFacade.getPerformanceListForCompetition(selectedCompetitionId);
        for (PerformanceResponse performance : performances) {
            BootcampAthlete athlete = performance.getAthlete();
            if (athlete == null || athlete.getId() == null) {
                continue;
            }
            String firstName = safe(athlete.getFirstname());
            String fullName = (firstName + " " + safe(athlete.getLastname())).trim();
            if (!firstName.isBlank()) {
                athleteIdsByName.putIfAbsent(firstName, athlete.getId());
            }
            if (!fullName.isBlank()) {
                athleteIdsByName.putIfAbsent(fullName, athlete.getId());
            }
        }
        return athleteIdsByName;
    }

    private String athleteProfileLink(String athleteName, Map<String, String> athleteIdsByName) {
        String athleteId = athleteIdsByName.get(athleteName);
        if (athleteId == null || athleteId.isBlank()) {
            return WildflyUtils.escape(athleteName);
        }
        return "<a href='#' class='link-primary' data-athlete-profile-id='" + escapeAttribute(athleteId)
                + "' data-athlete-profile-name='" + escapeAttribute(athleteName) + "'>" + WildflyUtils.escape(athleteName) + "</a>";
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String escapeAttribute(String value) {
        return WildflyUtils.escape(value == null ? "" : value).replace("'", "&#39;");
    }
}
