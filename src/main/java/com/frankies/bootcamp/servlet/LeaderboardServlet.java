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
        out.println("<style>" +
                "table {" +
                "  font-family: arial, sans-serif;" +
                "  border-collapse: collapse;" +
                "  width: 100%;" +
                "}" +
                "td, th {" +
                "  border: 1px solid #696969;" +
                "  text-align: left;" +
                "  padding: 8px;" +
                "}" +
                "tr:nth-child(even) {" +
                "  background-color: #dddddd;" +
                "}" +
                ".trophy { font-size: 1.2em; margin-left: 5px; }" +
                ".gold { color: gold; }" +
                ".silver { color: silver; }" +
                ".bronze { color: #cd7f32; }" +
                "</style>");
        out.println("</head><body>");

        // Total Challenge Score Table
        out.println("<h2>Total Challenge Score</h2>");
        out.println("<table>");
        out.println("<tr><th>Athlete</th><th>Score</th></tr>");

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
        out.println("</table>");

        // This Week Percentage Of Goal Table
        out.println("<h2>This Week Percentage Of Goal</h2>");
        out.println("<table>");
        out.println("<tr><th>Athlete</th><th>Progress</th></tr>");

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
        out.println("</table>");

        out.println("</body></html>");
    }
}
