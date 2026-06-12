package com.frankies.bootcamp.servlet;

import com.frankies.bootcamp.model.AuthenticatedUser;
import com.frankies.bootcamp.model.BootcampAthlete;
import com.frankies.bootcamp.service.AuthSessionService;
import com.frankies.bootcamp.service.AuthService;
import com.frankies.bootcamp.service.ActivityProcessFacade;
import com.frankies.bootcamp.service.DBService;
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

@WebServlet(name = "selectCompetition", value = "/app/select-competition")
public class SelectCompetitionServlet extends HttpServlet {
    @Inject
    private AuthSessionService authSessionService;

    @Inject
    private AuthService authService;

    @Inject
    private DBService dbService;

    @Inject
    private ActivityProcessFacade activityProcessFacade;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String competitionId = req.getParameter("competitionId");
        if (competitionId != null && !competitionId.isBlank()) {
            try {
                AuthenticatedUser user = authSessionService.getAuthenticatedUser(req);
                BootcampAthlete athlete = user == null ? null : authService.loadAthleteForUser(user);
                long parsedCompetitionId = Long.parseLong(competitionId);
                if (athlete != null && athlete.getId() != null && dbService.athleteBelongsToCompetition(athlete.getId(), parsedCompetitionId)) {
                    if (dbService.getPersistentAthleteSummarySnapshot(athlete.getId(), parsedCompetitionId) == null) {
                        activityProcessFacade.prepareAthleteSummaryForCompetition(athlete, parsedCompetitionId);
                    }
                    authSessionService.setSelectedCompetitionId(req, parsedCompetitionId);
                } else {
                    authSessionService.setSelectedCompetitionId(req, null);
                }
            } catch (SQLException | CredentialStoreException | NoSuchAlgorithmException e) {
                throw new IOException("Unable to select competition", e);
            }
        }
        resp.sendRedirect(req.getContextPath() + "/app");
    }
}
