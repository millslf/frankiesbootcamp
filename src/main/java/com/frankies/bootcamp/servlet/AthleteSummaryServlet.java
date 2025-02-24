package com.frankies.bootcamp.servlet;

import com.frankies.bootcamp.model.BootcampAthlete;
import com.frankies.bootcamp.service.ActivityProcessService;
import com.frankies.bootcamp.service.DBService;
import com.frankies.bootcamp.utils.WildflyUtils;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jboss.logging.Logger;
import org.wildfly.security.credential.store.CredentialStoreException;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;

@WebServlet(name = "athleteSummary", value = "/AthleteSummary")
public class AthleteSummaryServlet extends BootcampServlet {
    @Inject
    private ActivityProcessService activityProcessService;

    private String message;
    private static final Logger log = Logger.getLogger(AthleteSummaryServlet.class);

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
                log.info( "Athlete not authorised: " + authenticatedUserMail);
                out.println("<html><body>");
                out.println(home);
                out.println("<h1>" + HttpServletResponse.SC_UNAUTHORIZED + " Athlete not authorised" + "</h1>");
                out.println("</body></html>");
                return;
            }
            log.info("Athlete authorised: " + authenticatedUserMail);
            summaryContent =  activityProcessService.sendReport(false, false, loggedInAthlete.getEmail());
        } catch (IOException | CredentialStoreException | NoSuchAlgorithmException | SQLException e) {
            log.error("AthletesResource, allAthleteSummary", e);
        }
        out.println("<html><body>");
        out.println(home);
        out.println(WildflyUtils.escape(summaryContent));
        out.println("</body></html>");
    }

    public void destroy() {
    }
}