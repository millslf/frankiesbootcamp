package com.frankies.bootcamp.servlet;

import com.frankies.bootcamp.model.AuthenticatedUser;
import com.frankies.bootcamp.model.BootcampAthlete;
import com.frankies.bootcamp.model.CompetitionInvitationView;
import com.frankies.bootcamp.service.ActivityProcessFacade;
import com.frankies.bootcamp.service.AuthService;
import com.frankies.bootcamp.service.AuthSessionService;
import com.frankies.bootcamp.service.CompetitionInvitationService;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;

import org.wildfly.security.credential.store.CredentialStoreException;

@WebServlet(name = "competitionInvitationResponse", value = "/app/invitations/respond")
public class CompetitionInvitationResponseServlet extends jakarta.servlet.http.HttpServlet {
    private static final Logger log = Logger.getLogger(CompetitionInvitationResponseServlet.class);

    @Inject
    private CompetitionInvitationService competitionInvitationService;
    @Inject
    private AuthSessionService authSessionService;
    @Inject
    private AuthService authService;
    @Inject
    private ActivityProcessFacade activityProcessFacade;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        AuthenticatedUser user = authSessionService.getAuthenticatedUser(req);
        if (user == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        try {
            BootcampAthlete athlete = authService.loadAthleteForUser(user);
            String action = req.getParameter("action");
            long invitationId = Long.parseLong(req.getParameter("invitationId"));
            log.debugf("Invitation response action=%s invitationId=%d user=%s", action, invitationId, user.getEmail());
            if ("decline".equals(action)) {
                competitionInvitationService.declineInvitation(invitationId);
                authSessionService.clearPendingInvitationToken(req);
                resp.sendRedirect(req.getContextPath() + "/app/invitations?status=declined");
                return;
            }

            String startingGoalValue = req.getParameter("startingGoal");
            double startingGoal = startingGoalValue == null || startingGoalValue.isBlank()
                    ? (athlete != null && athlete.getGoal() != null ? athlete.getGoal() : 0.0)
                    : Double.parseDouble(startingGoalValue);

            CompetitionInvitationView invitation = competitionInvitationService.resolveInvitationToken(req.getParameter("token"));
            if (invitation == null) {
                resp.sendRedirect(req.getContextPath() + "/app/invitations?error=invalid");
                return;
            }

            CompetitionInvitationService.InvitationDecisionResult result = competitionInvitationService.acceptInvitation(invitationId, athlete, startingGoal, user.getUserId());
            if (result.success()) {
                log.debugf("Invitation accepted invitationId=%d competitionId=%d athleteId=%s", invitationId, invitation.getCompetitionId(), athlete == null ? null : athlete.getId());
                authSessionService.setSelectedCompetitionId(req, invitation.getCompetitionId());
                activityProcessFacade.prepareAthleteSummaryForCompetition(athlete, invitation.getCompetitionId());
                authSessionService.clearPendingInvitationToken(req);
                resp.sendRedirect(req.getContextPath() + "/app/?invite=accepted");
                return;
            }
            resp.sendRedirect(req.getContextPath() + "/app/invitations?error=" + java.net.URLEncoder.encode(result.message(), java.nio.charset.StandardCharsets.UTF_8));
        } catch (SQLException | NumberFormatException | CredentialStoreException | NoSuchAlgorithmException e) {
            log.error("Unable to process invitation response", e);
            throw new IOException("Unable to process invitation response", e);
        }
    }
}
