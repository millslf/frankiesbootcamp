package com.frankies.bootcamp.servlet;

import com.frankies.bootcamp.constant.BootcampConstants;
import com.frankies.bootcamp.model.BootcampAthlete;
import com.frankies.bootcamp.service.ActivityProcessService;
import com.frankies.bootcamp.service.DBService;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

@WebServlet(name = "leaderBoard", value = "/LeaderBoard")
public class LeaderboardServlet extends BootcampServlet {
    @Inject
    private ActivityProcessService activityProcessService;
    @Inject
    private DBService db;
    private static final Logger log = Logger.getLogger(LeaderboardServlet.class);

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        DecimalFormat df = new DecimalFormat("#.##");
        response.setContentType("text/html");
        String authenticatedUserMail = request.getHeader("Ngrok-Auth-User-Email");
        PrintWriter out = response.getWriter();

        try {
            BootcampAthlete loggedInAthlete = db.findAthleteByEmail(authenticatedUserMail);
            if (loggedInAthlete == null) {
                log.info("Athlete not authorised: " + authenticatedUserMail);
                out.println("<html><body>");
                out.println("<h1>" + HttpServletResponse.SC_UNAUTHORIZED + " Athlete not authorised</h1>");
                out.println("</body></html>");
                return;
            }
            log.info("Athlete authorised: " + authenticatedUserMail);
        } catch (SQLException e) {
            log.error("AthletesResource, allAthleteSummary", e);
        }

        Map<String, HashMap<String, Double>> sortedSummaries = activityProcessService.getSortedSummaries();

        out.println("<html><head>");
        out.println("<link rel=\"stylesheet\" href=\"https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.5/font/bootstrap-icons.css\">");
        out.println("<link rel=\"stylesheet\" href=\"/css/main.css\">\n");
        out.println("</head><body>");
        out.println("<div class='container'>");

        out.println("<h2><i class='bi bi-trophy header-icon'></i>Total Challenge Score</h2>");
        out.println("<table>");
        out.println("<thead><tr>");
        out.println("<th><i class='bi bi-person-fill header-icon' title='Athlete'></i>Athlete</th>");
        out.println("<th><i class='bi bi-star-fill header-icon' title='Score'></i>Score</th>");
        out.println("</tr></thead>");
        out.println("<tbody>");

        int rank = 1;
        for (Map.Entry<String, Double> entry : sortedSummaries.get(BootcampConstants.currentYearlyScoreSummary).entrySet()) {
            out.println("<tr><td>" + entry.getKey());
            if (rank == 1) {
                out.println(" <i class=\"bi bi-trophy trophy gold\" title=\"Gold Trophy\"></i>");
            } else if (rank == 2) {
                out.println(" <i class=\"bi bi-trophy trophy silver\" title=\"Silver Trophy\"></i>");
            } else if (rank == 3) {
                out.println(" <i class=\"bi bi-trophy trophy bronze\" title=\"Bronze Trophy\"></i>");
            }
            out.println("</td>");
            out.println("<td>" + df.format(entry.getValue()) + "</td></tr>");
            rank++;
        }
        out.println("</tbody></table>");

        // This Week Percentage Of Goal Table
        out.println("<h2><i class='bi bi-speedometer2 header-icon'></i>This Week Percentage Of Goal</h2>");
        out.println("<table>");
        out.println("<thead><tr>");
        out.println("<th><i class='bi bi-person-fill header-icon' title='Athlete'></i>Athlete</th>");
        out.println("<th><i class='bi bi-percent header-icon' title='Progress'></i>Progress</th>");
        out.println("</tr></thead>");
        out.println("<tbody>");

        rank = 1;
        for (Map.Entry<String, Double> entry : sortedSummaries.get(BootcampConstants.currentWeekPercentageOfGoalSummary).entrySet()) {
            out.println("<tr><td>" + entry.getKey());
            if (rank == 1) {
                out.println(" <i class=\"bi bi-trophy trophy gold\" title=\"Gold Trophy\"></i>");
            } else if (rank == 2) {
                out.println(" <i class=\"bi bi-trophy trophy silver\" title=\"Silver Trophy\"></i>");
            } else if (rank == 3) {
                out.println(" <i class=\"bi bi-trophy trophy bronze\" title=\"Bronze Trophy\"></i>");
            }
            out.println("</td>");
            out.println("<td>" + df.format(entry.getValue()) + "%</td></tr>");
            rank++;
        }
        out.println("</tbody></table>");
        out.println("</div>");
        out.println("</body></html>");
    }
}
