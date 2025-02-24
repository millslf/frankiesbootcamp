package com.frankies.bootcamp.servlet;

import com.frankies.bootcamp.constant.BootcampConstants;
import com.frankies.bootcamp.model.BootcampAthlete;
import com.frankies.bootcamp.service.ActivityProcessService;
import com.frankies.bootcamp.service.DBService;
import com.frankies.bootcamp.utils.WildflyUtils;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jboss.logging.Logger;
import org.wildfly.security.credential.store.CredentialStoreException;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

@WebServlet(name = "honourRoll", value = "/HonourRoll")
public class HonourRollServlet extends BootcampServlet {
    @Inject
    private ActivityProcessService activityProcessService;
    private static final Logger log = Logger.getLogger(HonourRollServlet.class);

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        DecimalFormat df = new DecimalFormat("#.##");
        response.setContentType("text/html");
        String authenticatedUserMail = request.getHeader("Ngrok-Auth-User-Email");
        PrintWriter out = response.getWriter();
        try {
            DBService db = new DBService();
            BootcampAthlete loggedInAthlete = db.findAthleteByEmail(authenticatedUserMail);
            if (loggedInAthlete == null) {
                log.info( "Athlete not authorised: " + authenticatedUserMail);
                out.println("<html><body>");
                out.println(home);
                out.println("<h1>" + HttpServletResponse.SC_UNAUTHORIZED + " Athlete not authorised" + "</h1>");
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
        out.println("<h2>Honour Roll</h2>");
        out.println("<table>");
        out.println("<tr>");
        out.println("<th>Week</th>");
        out.println("<th>Total Distance</th>");
        out.println("<th>Percentage of Goal</th>");
        out.println("</tr>");
        for (int i = 1; i < numberOfWeeksSinceStart; i++) {
            out.println("<tr>");
            out.println("<td>" + "Week " + (i) + "</td>");
            Map.Entry<String, Double> totalDistance = totalDist.get(i).entrySet().iterator().next();
            Map.Entry<String, Double> totalPercent = percOfGoal.get(i).entrySet().iterator().next();
            out.println("<td>" + totalDistance.getKey() + " " + df.format(totalDistance.getValue()) + "km</td>");
            out.println("<td>" + totalPercent.getKey() + " " + df.format(totalPercent.getValue()*100) + "%</td>");
        }
        out.println("</table>");

        out.println("</body></html>");
    }

    public void destroy() {
    }
}