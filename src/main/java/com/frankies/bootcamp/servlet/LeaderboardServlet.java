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

@WebServlet(name = "leaderBoard", value = "/app/LeaderBoard")
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
        out.println("<link href=\"/styles/main.css\" rel=\"stylesheet\">");
        out.println("</head><body>");
        out.println("<div class='container'>");
        out.println("<h2 class='history-heading'>");
        out.println("<i class='bi bi-trophy-fill'></i> Frankie's Bootcamp Leaderboard");
        out.println("</h2>");
        out.println("<p class='history-subheading'>");
        out.println("🔥 Tracking top performers and weekly goal crushers across the challenge.");
        out.println("</p>");

// Accordion Buttons
        out.println("<button id='btnScore' class='accordion-button btn btn-secondary'>");
        out.println("<span><i class='bi bi-trophy me-2'></i> Total Challenge Score</span>");
        out.println("<i class='bi bi-caret-down-fill accordion-arrow'></i>");
        out.println("</button>");

        out.println("<button id='btnProgress' class='accordion-button btn btn-secondary'>");
        out.println("<span><i class='bi bi-speedometer2 me-2'></i> This Week’s Goal Progress</span>");
        out.println("<i class='bi bi-caret-down-fill accordion-arrow'></i>");
        out.println("</button>");

// Accordion Contents

// Total Challenge Score Table
        out.println("<div id='scoreContent' class='accordion-content'>");
        out.println("<p class='history-subheading'>");
        out.println("🏅 Cumulative scores for athletes conquering the full challenge.");
        out.println("</p>");
        out.println("<table>");
        out.println("<thead><tr>");
        out.println("<th><i class='bi bi-person-fill'></i> Athlete</th>");
        out.println("<th><i class='bi bi-star-fill'></i> Score</th>");
        out.println("</tr></thead>");
        out.println("<tbody>");
        int rank = 1;
        for (Map.Entry<String, Double> entry : sortedSummaries.get(BootcampConstants.currentYearlyScoreSummary).entrySet()) {
            out.println("<tr><td>" + entry.getKey());
            if (rank == 1) {
                out.println(" <i class='bi bi-trophy trophy gold' title='Gold Trophy'></i>");
            } else if (rank == 2) {
                out.println(" <i class='bi bi-trophy trophy silver' title='Silver Trophy'></i>");
            } else if (rank == 3) {
                out.println(" <i class='bi bi-trophy trophy bronze' title='Bronze Trophy'></i>");
            }
            out.println("</td>");
            out.println("<td>" + df.format(entry.getValue()) + "</td></tr>");
            rank++;
        }
        out.println("</tbody></table>");
        out.println("</div>");

// This Week Percentage Of Goal Table
        out.println("<div id='progressContent' class='accordion-content'>");
        out.println("<p class='history-subheading'>");
        out.println("📈 Who’s pushing hardest this week and smashing their personal targets.");
        out.println("</p>");
        out.println("<table>");
        out.println("<thead><tr>");
        out.println("<th><i class='bi bi-person-fill'></i> Athlete</th>");
        out.println("<th><i class='bi bi-percent'></i> Progress</th>");
        out.println("</tr></thead>");
        out.println("<tbody>");
        rank = 1;
        for (Map.Entry<String, Double> entry : sortedSummaries.get(BootcampConstants.currentWeekPercentageOfGoalSummary).entrySet()) {
            out.println("<tr><td>" + entry.getKey());
            if (rank == 1) {
                out.println(" <i class='bi bi-trophy trophy gold' title='Gold Trophy'></i>");
            } else if (rank == 2) {
                out.println(" <i class='bi bi-trophy trophy silver' title='Silver Trophy'></i>");
            } else if (rank == 3) {
                out.println(" <i class='bi bi-trophy trophy bronze' title='Bronze Trophy'></i>");
            }
            out.println("</td>");
            out.println("<td>" + df.format(entry.getValue()) + "%</td></tr>");
            rank++;
        }
        out.println("</tbody></table>");
        out.println("</div>");

        out.println("<script>");
        out.println("const buttons = document.querySelectorAll('.accordion-button');");
        out.println("const contents = {");
        out.println("  btnScore: document.getElementById('scoreContent'),");
        out.println("  btnProgress: document.getElementById('progressContent')");
        out.println("};");

        out.println("buttons.forEach(button => {");
        out.println("  button.addEventListener('click', function () {");
        out.println("    const isActive = this.classList.contains('active');");

        out.println("    // Reset all");
        out.println("    buttons.forEach(b => b.classList.remove('active', 'btn-primary'));");
        out.println("    buttons.forEach(b => b.classList.add('btn-secondary'));");
        out.println("    Object.values(contents).forEach(c => c.classList.remove('active'));");

        out.println("    if (!isActive) {");
        out.println("      this.classList.add('active', 'btn-primary');");
        out.println("      this.classList.remove('btn-secondary');");
        out.println("      const contentId = this.id === 'btnScore' ? 'scoreContent' : 'progressContent';");
        out.println("      contents[this.id].classList.add('active');");
        out.println("    }");
        out.println("  });");
        out.println("});");
        out.println("</script>");

        out.println("</div></body></html>");
    }
}
