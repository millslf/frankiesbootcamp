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
    @Inject
    private DBService db;

    private static final Logger log = Logger.getLogger(AthleteSummaryServlet.class);

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html");
        String authenticatedUserMail = request.getHeader("Ngrok-Auth-User-Email");
        PrintWriter out = response.getWriter();
        String summaryContent = "";

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
            summaryContent = activityProcessService.sendReport(false, false, loggedInAthlete.getEmail());
        } catch (IOException | CredentialStoreException | NoSuchAlgorithmException | SQLException e) {
            log.error("AthletesResource, allAthleteSummary", e);
        }

        out.println("<!DOCTYPE html>");
        out.println("<html lang=\"en\">");
        out.println("<head>");
        out.println("  <meta charset=\"UTF-8\">");
        out.println("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">");
        out.println("  <title>Athlete Summary</title>");
        out.println("  <link href=\"https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css\" rel=\"stylesheet\">");
        out.println("  <link rel=\"stylesheet\" href=\"https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.5/font/bootstrap-icons.css\">");
        out.println("  <style>");
        out.println("    body { padding: 20px; font-family: Arial, sans-serif; background-color: #f5f8fa; }");
        out.println("    .content-box { background: #ffffff; padding: 20px; border-radius: 10px; box-shadow: 0 0 10px rgba(0,0,0,0.1); }");
        out.println("    .header-icon { margin-right: 10px; }");
        out.println("    .trophy-icon { margin-right: 8px; color: #f0ad4e; }");
        out.println("  </style>");
        out.println("</head>");
        out.println("<body>");
        out.println("  <h2><i class=\"bi bi-person-lines-fill text-primary header-icon\"></i> Athlete Summary</h2>");
        out.println("  <div class=\"content-box\">");

        // Optional: Add trophy icon before the summary content block
        out.println("    <p><i class=\"bi bi-trophy-fill trophy-icon\"></i><strong>Performance Summary:</strong></p>");

        // Output escaped or raw summaryContent depending on its nature
        out.println("    <div class=\"mt-2\">");
        out.println(WildflyUtils.escape(summaryContent));
        out.println("    </div>");

        // Optional: Add another trophy or reward footer
        out.println("    <hr/>");
        out.println("    <p class=\"text-muted\"><i class=\"bi bi-award-fill text-warning\"></i> Keep training hard and breaking limits!</p>");

        out.println("  </div>");
        out.println("</body>");
        out.println("</html>");
    }
}
