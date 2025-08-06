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

@WebServlet(name = "honourRoll", value = "/HonourRoll")
public class HonourRollServlet extends BootcampServlet {
    @Inject
    private ActivityProcessService activityProcessService;
    @Inject
    private DBService db;
    private static final Logger log = Logger.getLogger(HonourRollServlet.class);

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
        HashMap<Integer, HashMap<String, Double>> percOfGoal = activityProcessService.getHonourRollPercentageOfGoal();
        HashMap<Integer, HashMap<String, Double>> totalDist = activityProcessService.getHonourRollTotalDistance();
        int numberOfWeeksSinceStart = activityProcessService.getNumberOfWeeksSinceStart();

        out.println("<!DOCTYPE html>");
        out.println("<html lang=\"en\">");
        out.println("<head>");
        out.println("  <meta charset=\"UTF-8\">");
        out.println("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">");
        out.println("  <title>Frankies Bootcamp - Honour Roll</title>");
        out.println("  <link href=\"https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css\" rel=\"stylesheet\">");
        out.println("  <link rel=\"stylesheet\" href=\"https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.5/font/bootstrap-icons.css\">");
        out.println("  <link href=\"styles/main.css\" rel=\"stylesheet\">");
        out.println("</head>");
        out.println("<body>");

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
        out.println("      <th><i class='bi bi-rulers'></i> Total Distance</th>");
        out.println("      <th><i class='bi bi-graph-up'></i> Percentage of Goal</th>");
        out.println("    </tr>");
        out.println("  </thead>");
        out.println("  <tbody>");
        for (int i = 1; i < numberOfWeeksSinceStart; i++) {
            out.println("<tr>");
            out.println("<td>Week " + i + "</td>");
            Map.Entry<String, Double> totalDistance = totalDist.get(i).entrySet().iterator().next();
            Map.Entry<String, Double> totalPercent = percOfGoal.get(i).entrySet().iterator().next();
            out.println("<td>" + totalDistance.getKey() + " " + df.format(totalDistance.getValue()) + " km</td>");
            out.println("<td>" + totalPercent.getKey() + " " + df.format(totalPercent.getValue() * 100) + "%</td>");
            out.println("</tr>");
        }
        out.println("  </tbody>");
        out.println("</table>");
        out.println("</div>");
        out.println("  </div>");
        out.println("</body>");
        out.println("</html>");
    }
}
