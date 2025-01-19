package com.frankies.bootcamp.servlet;

import com.frankies.bootcamp.constant.BootcampConstants;
import com.frankies.bootcamp.model.BootcampAthlete;
import com.frankies.bootcamp.model.PerformanceResponse;
import com.frankies.bootcamp.service.DBService;
import com.frankies.bootcamp.service.ActivityProcessService;
import com.frankies.bootcamp.utils.WildflyUtils;
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
import java.util.ArrayList;
import java.util.List;

import static com.frankies.bootcamp.constant.BootcampConstants.START_TIMESTAMP;

@WebServlet(name = "athleteSummary", value = "/AthleteSummary")
public class BootcampServlet extends HttpServlet {
    private String message;
    private static final Logger log = Logger.getLogger(BootcampServlet.class);

    public void init() {
        message = "YOUR SUMMARY";
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html");
        String authenticatedUserMail = request.getHeader("Ngrok-Auth-User-Email");
        PrintWriter out = response.getWriter();
        String summaryContent="";
        try {
            DBService db = new DBService();
            BootcampAthlete loggedInAthlete = db.findAthleteByEmail(authenticatedUserMail);
            if (loggedInAthlete == null) {
                out.println("<html><body>");
                out.println("<h1>" + HttpServletResponse.SC_UNAUTHORIZED + " Athlete not authorised" + "</h1>");
                out.println("</body></html>");
                return;
            }
            int numberOfWeeksSinceStart = (int) Math.round(Math.ceil((double) (System.currentTimeMillis() - (START_TIMESTAMP * 1000)) / (BootcampConstants.WEEK_IN_SECONDS * 1000)));
            List<PerformanceResponse> performanceList = new ArrayList<>();
            ActivityProcessService activityProcessService = new ActivityProcessService();
            performanceList = activityProcessService.prepareSummary(performanceList, START_TIMESTAMP, numberOfWeeksSinceStart);
            summaryContent =  activityProcessService.sendReport(performanceList, numberOfWeeksSinceStart, false, false, loggedInAthlete.getEmail());
        } catch (IOException | CredentialStoreException | NoSuchAlgorithmException | SQLException e) {
            log.error("AthletesResource, allAthleteSummary", e);
        }

        // Hello
        out.println("<html><body>");
        out.println(WildflyUtils.escape(summaryContent));
        out.println("</body></html>");
    }

    public void destroy() {
    }
}