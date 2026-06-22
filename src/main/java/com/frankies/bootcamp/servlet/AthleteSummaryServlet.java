package com.frankies.bootcamp.servlet;

import com.frankies.bootcamp.service.ActivityProcessFacade;
import com.frankies.bootcamp.model.BootcampAthlete;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jboss.logging.Logger;
import org.wildfly.security.credential.store.CredentialStoreException;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;

@WebServlet(name = "athleteSummary", value = "/app/AthleteSummary")
public class AthleteSummaryServlet extends BootcampServlet {
    private static final Logger log = Logger.getLogger(AthleteSummaryServlet.class);

    @Inject
    private ActivityProcessFacade activityProcessFacade;
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            BootcampAthlete loggedInAthlete = (BootcampAthlete) request.getAttribute("athlete");
            if (loggedInAthlete == null || loggedInAthlete.getId() == null || loggedInAthlete.getId().isBlank()) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "A linked athlete is required.");
                return;
            }
            if (activityProcessFacade == null) {
                throw new IllegalStateException("ActivityProcessFacade was not available.");
            }
            Long selectedCompetitionId = resolveSelectedCompetitionId(request);
            String summaryContent = selectedCompetitionId != null
                    ? activityProcessFacade.getLoggedInAthleteSummaryForCompetition(selectedCompetitionId, loggedInAthlete.getId())
                    : activityProcessFacade.getLoggedInAthleteSummary(loggedInAthlete.getId());
            request.setAttribute("summaryContent", summaryContent);
            request.setAttribute("summaryError", null);
            request.getRequestDispatcher("/app/athlete-summary.jsp").forward(request, response);
        } catch (IOException | CredentialStoreException | NoSuchAlgorithmException | SQLException | jakarta.servlet.ServletException e) {
            log.error("Failed to load athlete summary", e);
            renderFallback(request, response, e);
        } catch (RuntimeException e) {
            log.error("Failed to load athlete summary", e);
            renderFallback(request, response, e);
        }
    }

    private Long resolveSelectedCompetitionId(HttpServletRequest request) {
        Object selectedCompetitionId = request.getAttribute("selectedCompetitionId");
        if (selectedCompetitionId instanceof Long competitionId) {
            return competitionId;
        }
        if (selectedCompetitionId instanceof String competitionIdText && !competitionIdText.isBlank()) {
            return Long.parseLong(competitionIdText);
        }
        return null;
    }

    private void renderFallback(HttpServletRequest request, HttpServletResponse response, Exception error) throws IOException {
        if (response.isCommitted()) {
            throw new IOException("Failed to load athlete summary", error);
        }
        request.setAttribute("summaryContent", "Summary unavailable right now.");
        request.setAttribute("summaryError", "Could not load your summary right now.");
        try {
            request.getRequestDispatcher("/app/athlete-summary.jsp").forward(request, response);
        } catch (jakarta.servlet.ServletException e) {
            throw new IOException("Failed to load athlete summary", e);
        }
    }
}
