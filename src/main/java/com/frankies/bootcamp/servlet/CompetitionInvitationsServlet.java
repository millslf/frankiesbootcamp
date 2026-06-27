package com.frankies.bootcamp.servlet;

import com.frankies.bootcamp.model.AuthenticatedUser;
import com.frankies.bootcamp.model.BootcampAthlete;
import com.frankies.bootcamp.model.CompetitionInvitationView;
import com.frankies.bootcamp.service.AuthService;
import com.frankies.bootcamp.service.AuthSessionService;
import com.frankies.bootcamp.service.CompetitionInvitationService;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.wildfly.security.credential.store.CredentialStoreException;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.List;

@WebServlet(name = "competitionInvitations", value = "/app/invitations")
public class CompetitionInvitationsServlet extends BootcampServlet {
    @Inject
    private CompetitionInvitationService competitionInvitationService;
    @Inject
    private AuthSessionService authSessionService;
    @Inject
    private AuthService authService;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            AuthenticatedUser user = authSessionService.getAuthenticatedUser(req);
            BootcampAthlete athlete = authService.loadAthleteForUser(user);
            List<CompetitionInvitationView> invitations = competitionInvitationService.listPendingForUser(user, athlete);
            req.setAttribute("pendingCompetitionInvitations", invitations);
            req.setAttribute("pendingInvitation", req.getAttribute("pendingInvitation"));
            req.getRequestDispatcher("/app/invitations.jsp").forward(req, resp);
        } catch (ServletException e) {
            throw new IOException("Unable to render invitations", e);
        } catch (SQLException e) {
            throw new IOException("Unable to load invitations", e);
        }
    }
}
