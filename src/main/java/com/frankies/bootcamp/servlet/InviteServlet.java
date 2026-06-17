package com.frankies.bootcamp.servlet;

import com.frankies.bootcamp.model.CompetitionInvitationView;
import com.frankies.bootcamp.service.AuthSessionService;
import com.frankies.bootcamp.service.CompetitionInvitationService;
import com.frankies.bootcamp.service.AuthService;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet(name = "invite", value = "/invite")
public class InviteServlet extends jakarta.servlet.http.HttpServlet {
    private static final Logger log = Logger.getLogger(InviteServlet.class);

    @Inject
    private CompetitionInvitationService competitionInvitationService;
    @Inject
    private AuthSessionService authSessionService;
    @Inject
    private AuthService authService;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        String token = req.getParameter("token");
        if (token != null && !token.isBlank()) {
            log.debugf("Invite page token received token=%s", token);
            authSessionService.setPendingInvitationToken(req, token.trim());
        }

        try {
            CompetitionInvitationView invitation = competitionInvitationService.resolveInvitationToken(authSessionService.getPendingInvitationToken(req));
            log.debugf("Invite page resolved invitation=%s authenticated=%s", invitation == null ? "null" : invitation.getId(), authSessionService.getAuthenticatedUser(req) != null);
            req.setAttribute("invitation", invitation);
            if (invitation != null && !invitation.isPending()) {
                req.setAttribute("inviteError", "This invitation is no longer available.");
            }
            req.getRequestDispatcher("/invite.jsp").forward(req, resp);
        } catch (SQLException e) {
            throw new ServletException("Unable to load invitation", e);
        }
    }
}
