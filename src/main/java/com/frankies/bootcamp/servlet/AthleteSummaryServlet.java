package com.frankies.bootcamp.servlet;

import com.frankies.bootcamp.service.ActivityProcessFacade;
import com.frankies.bootcamp.utils.WildflyUtils;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.wildfly.security.credential.store.CredentialStoreException;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;

@WebServlet(name = "athleteSummary", value = "/app/AthleteSummary")
public class AthleteSummaryServlet extends BootcampServlet {
    @Inject
    private ActivityProcessFacade activityProcessFacade;
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        String summaryContent = "";

        try {
            com.frankies.bootcamp.model.BootcampAthlete loggedInAthlete = (com.frankies.bootcamp.model.BootcampAthlete) request.getAttribute("athlete");
            Long selectedCompetitionId = (Long) request.getAttribute("selectedCompetitionId");
            summaryContent = selectedCompetitionId != null
                    ? activityProcessFacade.getLoggedInAthleteSummaryForCompetition(selectedCompetitionId, loggedInAthlete.getId())
                    : activityProcessFacade.getLoggedInAthleteSummary(loggedInAthlete.getId());
        } catch (IOException | CredentialStoreException | NoSuchAlgorithmException | SQLException e) {
            throw new IOException("Failed to load athlete summary", e);
        }

        out.println("<!DOCTYPE html>");
        out.println("<html lang=\"en\">");
        out.println("<head>");
        out.println("  <meta charset=\"UTF-8\">");
        out.println("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">");
        out.println("  <title>Athlete Summary</title>");
        out.println("</head>");
        out.println("<body>");
        out.println("  <div class=\"container\">");

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
