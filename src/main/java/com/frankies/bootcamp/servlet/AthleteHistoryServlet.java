package com.frankies.bootcamp.servlet;

import com.frankies.bootcamp.model.BootcampAthlete;
import com.frankies.bootcamp.model.WeeklyPerformance;
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

@WebServlet(name = "athleteHistory", value = "/AthleteHistory")
public class AthleteHistoryServlet extends BootcampServlet {
    @Inject
    private ActivityProcessService activityProcessService;
    @Inject
    private DBService db;

    private static final Logger log = Logger.getLogger(AthleteHistoryServlet.class);

    public void init() {
    }

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
                out.println(home);
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

        // Hello
        out.println("<html><body>");
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
                "}tr:nth-child(even) {" +
                "  background-color: #dddddd;" +
                "}" +
                "</style>");
        out.println(home);
        out.println("<h2>Weekly History</h2>");
        out.println("<table>");
        out.println("<tr>");
        out.println("<th>Week</th>");
        out.println("<th>Distance</th>");
        out.println("<th>Commitment</th>");
        out.println("<th>Percentage of commitment</th>");
        out.println("<th>Points scored</th>");
        out.println("<th>Activities</th>");
        out.println("</tr>");
        for (int i = 1; i <= numberOfWeeksSinceStart; i++) {
            out.println("<tr>");
            String weekText = history.get(i).getWeek();
            if(history.get(i).isSick()){
                weekText += "</br><i>(Sick Note)";
            }
            if (i == numberOfWeeksSinceStart) {
                out.println("<td>" + weekText +
                        "</br><i>(Week in progress)" + "</td>");
            } else {
                out.println("<td>" + weekText + "</td>");
            }
            out.println("<td>" + df.format(history.get(i).getTotalDistance()) + "km</td>");
            out.println("<td>" + df.format(history.get(i).getWeekGoal()) + "km</td>");
            out.println("<td>" + df.format(history.get(i).getTotalPercentOfGoal() * 100) + "%</td>");
            out.println("<td>" + df.format(history.get(i).getWeekScore()) + "</td>");
            out.println("<td>");
            for (String key : history.get(i).getSports().keySet()) {
                out.println(key + " " + df.format(history.get(i).getSports().get(key)) + "km");
                if (history.get(i).getSportsOriginalDistance().containsKey(key)) {
                    out.println(" (" + df.format(history.get(i).getSportsOriginalDistance().get(key)) + "km)</br>");
                } else if (history.get(i).getSportsOriginalDuration().containsKey(key)) {
                    out.println(" (" + df.format(history.get(i).getSportsOriginalDuration().get(key)) + "hrs)</br>");
                }
            }
            out.println("</td>");
        }
        out.println("</table>");
        out.println("</body></html>");
    }

    public void destroy() {
    }
}