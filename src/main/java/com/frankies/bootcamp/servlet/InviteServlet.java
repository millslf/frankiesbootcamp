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
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

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
        String token = extractInvitationToken(req.getParameter("token"));
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

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String token = extractInvitationToken(req.getParameter("token"));
        if (token == null || token.isBlank()) {
            resp.sendRedirect(req.getContextPath() + "/invite");
            return;
        }
        authSessionService.setPendingInvitationToken(req, token.trim());
        resp.sendRedirect(req.getContextPath() + "/invite?token=" + java.net.URLEncoder.encode(token.trim(), java.nio.charset.StandardCharsets.UTF_8));
    }

    private String extractInvitationToken(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (!trimmed.contains("://") && !trimmed.contains("token=")) {
            return trimmed;
        }
        try {
            URI uri = URI.create(trimmed);
            String query = uri.getQuery();
            if (query == null || query.isBlank()) {
                return trimmed;
            }
            for (String part : query.split("&")) {
                int equals = part.indexOf('=');
                if (equals <= 0) {
                    continue;
                }
                String key = part.substring(0, equals);
                if ("token".equalsIgnoreCase(key)) {
                    return URLDecoder.decode(part.substring(equals + 1), StandardCharsets.UTF_8);
                }
            }
        } catch (IllegalArgumentException ignored) {
            int index = trimmed.indexOf("token=");
            if (index >= 0) {
                String tokenPart = trimmed.substring(index + "token=".length());
                int end = tokenPart.indexOf('&');
                if (end >= 0) {
                    tokenPart = tokenPart.substring(0, end);
                }
                return URLDecoder.decode(tokenPart, StandardCharsets.UTF_8);
            }
        }
        return trimmed;
    }
}
