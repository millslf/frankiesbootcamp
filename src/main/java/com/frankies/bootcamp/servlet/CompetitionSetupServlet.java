package com.frankies.bootcamp.servlet;

import com.frankies.bootcamp.model.AuthenticatedUser;
import com.frankies.bootcamp.model.BootcampAthlete;
import com.frankies.bootcamp.model.CompetitionInvitationView;
import com.frankies.bootcamp.service.AuthService;
import com.frankies.bootcamp.service.AuthSessionService;
import com.frankies.bootcamp.service.CompetitionAccessService;
import com.frankies.bootcamp.service.CompetitionInvitationService;
import com.frankies.bootcamp.service.CompetitionSetupService;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jboss.logging.Logger;
import org.wildfly.security.credential.store.CredentialStoreException;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.List;

@WebServlet(name = "competitionSetup", value = "/app/competition-setup")
public class CompetitionSetupServlet extends HttpServlet {
    private static final Logger log = Logger.getLogger(CompetitionSetupServlet.class);

    @Inject
    private AuthSessionService authSessionService;

    @Inject
    private AuthService authService;

    @Inject
    private CompetitionSetupService competitionSetupService;

    @Inject
    private CompetitionAccessService competitionAccessService;

    @Inject
    private CompetitionInvitationService competitionInvitationService;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            BootcampAthlete athlete = requireLinkedAthlete(req);
            if (!competitionAccessService.canAccessCompetitionSetup(authSessionService.getAuthenticatedUser(req))) {
                resp.sendRedirect(req.getContextPath() + "/app");
                return;
            }
            req.setAttribute("competitionSetupView", competitionSetupService.loadView(athlete));
            applyInvitationAttributes(req, athlete);
            req.getRequestDispatcher("/app/competition-setup.jsp").forward(req, resp);
        } catch (IllegalStateException e) {
            log.error("Competition setup GET rejected", e);
            resp.sendRedirect(req.getContextPath() + "/app");
        } catch (SQLException e) {
            log.error("Unable to load competition setup view", e);
            throw new ServletException("Unable to load competition setup view", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action");

        try {
            BootcampAthlete athlete = requireLinkedAthlete(req);
            if (!competitionAccessService.canAccessCompetitionSetup(authSessionService.getAuthenticatedUser(req))) {
                resp.sendRedirect(req.getContextPath() + "/app");
                return;
            }

            if ("create".equals(action)) {
                long competitionId = competitionSetupService.createCompetitionAndJoin(
                        athlete,
                        req.getParameter("competitionName"),
                        req.getParameter("timezone"),
                        req.getParameter("startDate"),
                        req.getParameter("endDate"),
                        req.getParameter("startingGoal")
                );
                authSessionService.setSelectedCompetitionId(req, competitionId);
                resp.sendRedirect(req.getContextPath() + "/app/competition-invitations?competitionId=" + competitionId);
                return;
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
            log.error("Competition setup validation failed", e);
            try {
                reloadWithError(req, resp, e.getMessage());
            } catch (SQLException sqlException) {
                log.error("Unable to reload competition setup after validation error", sqlException);
                throw new ServletException("Unable to reload competition setup after validation error", sqlException);
            }
        } catch (IllegalStateException e) {
            log.error("Competition setup blocked", e);
            resp.sendRedirect(req.getContextPath() + "/app");
        } catch (SQLException e) {
            log.error("Unable to save competition setup", e);
            throw new ServletException("Unable to save competition setup", e);
        } catch (CredentialStoreException | NoSuchAlgorithmException e) {
            log.error("Unable to refresh athlete competition data after setup", e);
            throw new ServletException("Unable to refresh athlete competition data after setup", e);
        }
    }

    private void reloadWithError(HttpServletRequest req, HttpServletResponse resp, String error) throws ServletException, IOException, SQLException {
        BootcampAthlete athlete = requireLinkedAthlete(req);
        req.setAttribute("competitionSetupView", competitionSetupService.loadView(athlete));
        applyInvitationAttributes(req, athlete);
        req.setAttribute("competitionSetupError", error);
        req.getRequestDispatcher("/app/competition-setup.jsp").forward(req, resp);
    }

    private void applyInvitationAttributes(HttpServletRequest req, BootcampAthlete athlete) throws SQLException {
        AuthenticatedUser authenticatedUser = authSessionService.getAuthenticatedUser(req);
        List<CompetitionInvitationView> invitations = competitionInvitationService.listPendingForUser(authenticatedUser, athlete);
        req.setAttribute("pendingCompetitionInvitations", invitations);
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
