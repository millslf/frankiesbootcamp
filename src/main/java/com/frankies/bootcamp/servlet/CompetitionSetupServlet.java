package com.frankies.bootcamp.servlet;

import com.frankies.bootcamp.model.AuthenticatedUser;
import com.frankies.bootcamp.model.BootcampAthlete;
import com.frankies.bootcamp.service.AuthService;
import com.frankies.bootcamp.service.AuthSessionService;
import com.frankies.bootcamp.service.CompetitionSetupService;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.wildfly.security.credential.store.CredentialStoreException;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;

@WebServlet(name = "competitionSetup", value = "/app/competition-setup")
public class CompetitionSetupServlet extends HttpServlet {
    @Inject
    private AuthSessionService authSessionService;

    @Inject
    private AuthService authService;

    @Inject
    private CompetitionSetupService competitionSetupService;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            BootcampAthlete athlete = requireLinkedAthlete(req);
            req.setAttribute("competitionSetupView", competitionSetupService.loadView(athlete));
            req.getRequestDispatcher("/app/competition-setup.jsp").forward(req, resp);
        } catch (IllegalStateException e) {
            resp.sendRedirect(req.getContextPath() + "/app");
        } catch (SQLException e) {
            throw new ServletException("Unable to load competition setup view", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action");

        try {
            BootcampAthlete athlete = requireLinkedAthlete(req);

            if ("create".equals(action)) {
                competitionSetupService.createCompetitionAndJoin(
                        athlete,
                        req.getParameter("competitionName"),
                        req.getParameter("timezone"),
                        req.getParameter("startDate"),
                        req.getParameter("endDate"),
                        req.getParameter("startingGoal")
                );
            } else if ("join".equals(action)) {
                competitionSetupService.joinCompetition(
                        athlete,
                        req.getParameter("competitionId"),
                        req.getParameter("joinStartingGoal")
                );
            } else {
                throw new IllegalArgumentException("Unsupported competition setup action.");
            }

            resp.sendRedirect(req.getContextPath() + "/app");
        } catch (IllegalArgumentException e) {
            try {
                reloadWithError(req, resp, e.getMessage());
            } catch (SQLException sqlException) {
                throw new ServletException("Unable to reload competition setup after validation error", sqlException);
            }
        } catch (IllegalStateException e) {
            resp.sendRedirect(req.getContextPath() + "/app");
        } catch (SQLException e) {
            throw new ServletException("Unable to save competition setup", e);
        } catch (CredentialStoreException | NoSuchAlgorithmException e) {
            throw new ServletException("Unable to refresh athlete competition data after setup", e);
        }
    }

    private void reloadWithError(HttpServletRequest req, HttpServletResponse resp, String error) throws ServletException, IOException, SQLException {
        BootcampAthlete athlete = requireLinkedAthlete(req);
        req.setAttribute("competitionSetupView", competitionSetupService.loadView(athlete));
        req.setAttribute("competitionSetupError", error);
        req.getRequestDispatcher("/app/competition-setup.jsp").forward(req, resp);
    }

    private BootcampAthlete requireLinkedAthlete(HttpServletRequest req) throws SQLException {
        AuthenticatedUser authenticatedUser = authSessionService.getAuthenticatedUser(req);
        if (authenticatedUser == null) {
            throw new IllegalStateException("Login required");
        }

        BootcampAthlete athlete = authService.loadAthleteForUser(authenticatedUser);
        if (athlete == null || athlete.getId() == null || athlete.getId().isBlank() || athlete.getId().startsWith("local-")) {
            throw new IllegalStateException("Strava link required");
        }
        return athlete;
    }
}
