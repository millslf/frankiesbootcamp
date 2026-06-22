package com.frankies.bootcamp.servlet;

import com.frankies.bootcamp.model.BootcampAthlete;
import com.frankies.bootcamp.service.AuthSessionService;
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

@WebServlet(name = "manageCompetitions", value = "/app/competitions")
public class ManageCompetitionsServlet extends BootcampServlet {
    @Inject
    private AuthSessionService authSessionService;

    @Inject
    private DBService dbService;

    @Inject
    private CompetitionSetupService competitionSetupService;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            renderPage(request, response);
        } catch (SQLException e) {
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
            } else if ("join".equals(action)) {
                competitionSetupService.joinCompetition(athlete, request.getParameter("competitionId"), request.getParameter("joinStartingGoal"));
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
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "A linked athlete is required.");
            return;
        }

        request.setAttribute("activeCompetitions", dbService.listCurrentActiveCompetitions(athlete.getId()));
        request.setAttribute("pastCompetitions", dbService.listPastCompetitions(athlete.getId()));
        request.setAttribute("availableCompetitions", dbService.listJoinableCompetitions(athlete.getId()));
        Long selectedCompetitionId = authSessionService.getSelectedCompetitionId(request);
        request.setAttribute("selectedCompetitionAdmin", selectedCompetitionId != null && dbService.isCompetitionAdmin(selectedCompetitionId, athlete.getId()));
        request.getSession(true).setAttribute("activeCompetitions", request.getAttribute("activeCompetitions"));
        request.getSession(true).setAttribute("pastCompetitions", request.getAttribute("pastCompetitions"));
        request.getSession(true).setAttribute("selectedCompetitionId", selectedCompetitionId);
        request.getRequestDispatcher("/app/competition-selection.jsp").forward(request, response);
    }
}
