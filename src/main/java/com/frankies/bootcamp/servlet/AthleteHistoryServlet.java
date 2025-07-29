package com.frankies.bootcamp.servlet;

import com.frankies.bootcamp.model.BootcampAthlete;
import com.frankies.bootcamp.model.WeeklyPerformance;
import com.frankies.bootcamp.service.ActivityProcessService;
import com.frankies.bootcamp.service.DBService;
import com.frankies.bootcamp.utils.DateTimeUtils;
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

@WebServlet(name = "athleteHistory", value = "/AthleteHistory")
public class AthleteHistoryServlet extends BootcampServlet {
    @Inject
    private ActivityProcessService activityProcessService;
    @Inject
    private DBService db;

    private static final Logger log = Logger.getLogger(AthleteHistoryServlet.class);

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        DecimalFormat df = new DecimalFormat("#.##");
        Map<Integer, WeeklyPerformance> history = new HashMap<>();
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        String authenticatedUserMail = request.getHeader("Ngrok-Auth-User-Email");
        try {
            BootcampAthlete loggedInAthlete = db.findAthleteByEmail(authenticatedUserMail);
            if (loggedInAthlete == null) {
                log.info("Athlete not authorised: " + authenticatedUserMail);
                out.println("<html><body>");
                out.println("<h1>" + HttpServletResponse.SC_UNAUTHORIZED + " Athlete not authorised" + "</h1>");
                out.println("</body></html>");
                return;
            }
            log.info("Athlete authorised: " + authenticatedUserMail);
            history = activityProcessService.getAthleteHistory(loggedInAthlete.getEmail());
        } catch (SQLException e) {
            log.error("AthleteHistoryServlet, doGet", e);
        }
        int numberOfWeeksSinceStart = activityProcessService.getNumberOfWeeksSinceStart();

        out.println("<html><head>");
        out.println("<link rel=\"stylesheet\" href=\"https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.5/font/bootstrap-icons.css\">");
        out.println("<link rel=\"stylesheet\" href=\"/css/main.css\">\n");
        out.println("</head><body>");

        out.println("<div class='container'>");

        out.println("<h2 class='history-heading'>");
        out.println("<i class='bi bi-clock-history'></i> My Weekly Competition History");
        out.println("</h2>");
        out.println("<p class='history-subheading'><i class='bi bi-graph-up-arrow'></i> Track how you performed each week across all activities, see your progress, week by week!</p>");
        out.println("<div class='table-responsive mt-4'>");
        out.println("<table class='table table-bordered table-striped align-middle'>");
        out.println("<thead class='table-dark'>");
        out.println("<tr>");
        out.println("<th><i class='bi bi-calendar'></i> Week</th>");
        out.println("<th><i class='bi bi-rulers'></i> Distance</th>");
        out.println("<th><i class='bi bi-bullseye'></i> Commitment</th>");
        out.println("<th><i class='bi bi-graph-up'></i> Percentage of commitment</th>");
        out.println("<th><i class='bi bi-signpost'></i> Distance left</th>");
        out.println("<th><i class='bi bi-star-fill'></i> Points scored</th>");
        out.println("<th class='col-activities'><i class='bi bi-activity'></i> Activities</th>");
        out.println("</tr>");
        out.println("</thead>");
        out.println("<tbody>");
        for (int i = numberOfWeeksSinceStart; i >= 1; i--) {
            out.println("<tr>");
            String weekText = history.get(i).getWeek();
            if(history.get(i).isSick()){
                weekText += " <i class='bi bi-heart-pulse-fill' title='Sick Note'></i>";
            }
            if (i == numberOfWeeksSinceStart) {
                out.println("<td>" + weekText +
                        "</br><i>(Week in progress)" + "</td>");
            } else {
                out.println("<td>" + weekText + "</td>");
            }
            out.println("<td>" + df.format(history.get(i).getTotalDistance()) + " km </td>");
            out.println("<td>" + df.format(history.get(i).getWeekGoal()) + "km</td>");
            out.println("<td>" + df.format(history.get(i).getTotalPercentOfGoal() * 100) + "%</td>");
            out.println("<td>" + df.format(history.get(i).getWeekGoal()-history.get(i).getTotalDistance()>0?history.get(i).getWeekGoal()-history.get(i).getTotalDistance():0) + "km</td>");
            out.println("<td>" + df.format(history.get(i).getWeekScore()) + "</td>");
            out.println("<td>");
            for (String key : history.get(i).getSports().keySet()) {
                out.println(key + "(x" + history.get(i).getSportsCount().get(key) + ") " + df.format(history.get(i).getSports().get(key)) + "km");
                if (history.get(i).getSportsOriginalDistance().containsKey(key)) {
                    out.println(" (" + df.format(history.get(i).getSportsOriginalDistance().get(key)) + "km)</br>");
                } else if (history.get(i).getSportsOriginalDuration().containsKey(key)) {
                    out.println(" (" + DateTimeUtils.convertMinutesToTimeFormat(history.get(i).getSportsOriginalDuration().get(key)*60) + ")</br>");
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