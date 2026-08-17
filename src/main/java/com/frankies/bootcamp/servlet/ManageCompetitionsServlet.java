package com.frankies.bootcamp.servlet;

import com.frankies.bootcamp.model.BootcampAthlete;
import com.frankies.bootcamp.model.AuthenticatedUser;
import com.frankies.bootcamp.model.CompetitionInvitationView;
import com.frankies.bootcamp.service.AuthSessionService;
import com.frankies.bootcamp.service.CompetitionInvitationService;
import com.frankies.bootcamp.service.CompetitionSetupService;
import com.frankies.bootcamp.service.DBService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.inject.Inject;
import org.wildfly.security.credential.store.CredentialStoreException;

import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.jboss.logging.Logger;

@WebServlet(name = "manageCompetitions", value = "/app/competitions")
public class ManageCompetitionsServlet extends BootcampServlet {
    private static final Logger log = Logger.getLogger(ManageCompetitionsServlet.class);

    @Inject
    private AuthSessionService authSessionService;

    @Inject
    private DBService dbService;

    @Inject
    private CompetitionSetupService competitionSetupService;

    @Inject
    private CompetitionInvitationService competitionInvitationService;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            log.debugf("Loading competitions page for athlete request=%s", request.getRequestURI());
            renderPage(request, response);
        } catch (SQLException e) {
            log.error("Unable to load competition management page", e);
            throw new ServletException("Unable to load competition management page", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        BootcampAthlete athlete = (BootcampAthlete) request.getAttribute("athlete");
        if (athlete == null || athlete.getId() == null || athlete.getId().isBlank()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "A linked athlete is required.");
            return;
        }

        String action = request.getParameter("action");
        long competitionId;
        try {
            competitionId = Long.parseLong(request.getParameter("competitionId"));
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Competition selection is required.");
            return;
        }

        try {
            if ("leave".equals(action)) {
                if (!dbService.athleteBelongsToCompetition(athlete.getId(), competitionId)) {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "You are not active in that competition.");
                    return;
                }
                if (dbService.isCompetitionAdmin(competitionId, athlete.getId()) && dbService.countCompetitionAdmins(competitionId) <= 1) {
                    response.sendRedirect(buildLeaveErrorRedirect(request.getContextPath(), "last-admin"));
                    return;
                }

                dbService.leaveCompetition(competitionId, athlete.getId());
                Long selectedCompetitionId = authSessionService.getSelectedCompetitionId(request);
                if (selectedCompetitionId != null && selectedCompetitionId == competitionId) {
                    authSessionService.setSelectedCompetitionId(request, null);
                }
                if (authSessionService.getSelectedCompetitionId(request) == null) {
                    request.getSession(true).removeAttribute("selectedCompetitionId");
                }
            } else if ("end".equals(action)) {
                if (!dbService.isCompetitionAdmin(competitionId, athlete.getId())) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Only competition admins can end a competition.");
                    return;
                }
                dbService.endCompetition(competitionId, athlete.getId());
            } else if ("delete".equals(action)) {
                if (!dbService.isCompetitionAdmin(competitionId, athlete.getId())) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Only competition admins can delete a competition.");
                    return;
                }
                dbService.deleteCompetition(competitionId, athlete.getId());
            } else if ("join".equals(action)) {
                competitionSetupService.joinCompetition(athlete, request.getParameter("competitionId"), request.getParameter("joinStartingGoal"));
                authSessionService.setSelectedCompetitionId(request, competitionId);
                request.getSession(true).setAttribute("selectedCompetitionId", competitionId);
            } else if ("accept-invite".equals(action)) {
                String invitationToken = request.getParameter("invitationToken");
                if (invitationToken == null || invitationToken.isBlank()) {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invitation token is required.");
                    return;
                }
                CompetitionInvitationView invitation = competitionInvitationService.resolveInvitationToken(invitationToken);
                if (invitation == null || !invitation.isPending() || invitation.getCompetitionId() != competitionId) {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invitation not found.");
                    return;
                }
                CompetitionInvitationService.InvitationDecisionResult decision = competitionInvitationService.acceptInvitation(
                        invitation.getId(),
                        athlete,
                        athlete.getGoal() == null ? 0.0 : athlete.getGoal(),
                        null
                );
                if (!decision.success()) {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, decision.message());
                    return;
                }
                authSessionService.setSelectedCompetitionId(request, competitionId);
                request.getSession(true).setAttribute("selectedCompetitionId", competitionId);
            } else {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unsupported competition action.");
                return;
            }
            response.sendRedirect(request.getContextPath() + "/app/competitions");
        } catch (SQLException | CredentialStoreException | NoSuchAlgorithmException e) {
            throw new ServletException("Unable to update competition membership", e);
        }
    }

    static String buildLeaveErrorRedirect(String contextPath, String errorCode) {
        return contextPath + "/app/competitions?leaveError=" + URLEncoder.encode(errorCode, StandardCharsets.UTF_8);
    }

    private void renderPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, SQLException {
        BootcampAthlete athlete = (BootcampAthlete) request.getAttribute("athlete");
        if (athlete == null || athlete.getId() == null || athlete.getId().isBlank()) {
            log.warn("Competition management requested without a linked athlete");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "A linked athlete is required.");
            return;
        }

        Long selectedCompetitionId = authSessionService.getSelectedCompetitionId(request);
        request.setAttribute("selectedCompetitionAdmin", selectedCompetitionId != null && dbService.isCompetitionAdmin(selectedCompetitionId, athlete.getId()));
        request.setAttribute("upcomingCompetitions", dbService.listUpcomingCompetitions(athlete.getId()));
        request.setAttribute("currentCompetitions", dbService.listCurrentActiveCompetitions(athlete.getId()));
        request.setAttribute("pastCompetitions", dbService.listPastCompetitions(athlete.getId()));
        request.setAttribute("deletedCompetitions", dbService.listDeletedCompetitions(athlete.getId()));
        request.setAttribute("availableCompetitions", dbService.listAvailableCompetitions(athlete.getId()));
        AuthenticatedUser authenticatedUser = authSessionService.getAuthenticatedUser(request);
        request.setAttribute("pendingCompetitionInvitations", competitionInvitationService.listPendingForUser(authenticatedUser, athlete));
        request.setAttribute("managedCompetitionIds", buildManagedCompetitionIds(athlete.getId()));
        request.setAttribute("activeCompetitions", request.getAttribute("currentCompetitions"));
        request.getSession(true).setAttribute("activeCompetitions", request.getAttribute("currentCompetitions"));
        request.getSession(true).setAttribute("pastCompetitions", request.getAttribute("pastCompetitions"));
        request.getSession(true).setAttribute("selectedCompetitionId", selectedCompetitionId);
        log.debugf("Loaded competition page lists for athlete=%s current=%d upcoming=%d available=%d past=%d deleted=%d",
                athlete.getId(),
                ((java.util.List<?>) request.getAttribute("currentCompetitions")).size(),
                ((java.util.List<?>) request.getAttribute("upcomingCompetitions")).size(),
                ((java.util.List<?>) request.getAttribute("availableCompetitions")).size(),
                ((java.util.List<?>) request.getAttribute("pastCompetitions")).size(),
                ((java.util.List<?>) request.getAttribute("deletedCompetitions")).size());
        request.getRequestDispatcher("/app/competition-selection.jsp").forward(request, response);
    }

    private java.util.Set<Long> buildManagedCompetitionIds(String athleteId) throws SQLException {
        java.util.Set<Long> managedCompetitionIds = new java.util.HashSet<>();
        for (var competition : dbService.listCurrentActiveCompetitions(athleteId)) {
            if (dbService.isCompetitionAdmin(competition.getId(), athleteId)) {
                managedCompetitionIds.add(competition.getId());
            }
        }
        for (var competition : dbService.listUpcomingCompetitions(athleteId)) {
            if (dbService.isCompetitionAdmin(competition.getId(), athleteId)) {
                managedCompetitionIds.add(competition.getId());
            }
        }
        return managedCompetitionIds;
    }
}
