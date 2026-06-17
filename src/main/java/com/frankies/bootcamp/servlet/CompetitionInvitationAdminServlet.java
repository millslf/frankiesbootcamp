package com.frankies.bootcamp.servlet;

import com.frankies.bootcamp.model.AuthenticatedUser;
import com.frankies.bootcamp.model.BootcampAthlete;
import com.frankies.bootcamp.model.CompetitionInvitationPageView;
import com.frankies.bootcamp.model.CompetitionInviteCandidateView;
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

@WebServlet(name = "competitionInvitationAdmin", value = "/app/competition-invitations")
public class CompetitionInvitationAdminServlet extends BootcampServlet {
    @Inject
    private CompetitionInvitationService competitionInvitationService;
    @Inject
    private AuthSessionService authSessionService;
    @Inject
    private AuthService authService;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            long competitionId = resolveCompetitionId(req);
            BootcampAthlete athlete = requireAdminAthlete(req, competitionId);
            String query = req.getParameter("q");
            CompetitionInvitationPageView view = competitionInvitationService.loadAdminPage(competitionId, query);
            req.setAttribute("adminAthlete", athlete);
            req.setAttribute("invitationAdminView", view);
            req.getRequestDispatcher("/app/competition-invitations.jsp").forward(req, resp);
        } catch (IllegalStateException e) {
            resp.sendRedirect(req.getContextPath() + "/app/");
        } catch (ServletException e) {
            throw new IOException("Unable to render competition invitations", e);
        } catch (SQLException e) {
            throw new IOException("Unable to load competition invitations", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            long competitionId = resolveCompetitionId(req);
            BootcampAthlete athlete = requireAdminAthlete(req, competitionId);
            String action = req.getParameter("action");
            String feedback = null;
            String error = null;

            if ("bulkInvite".equals(action)) {
                CompetitionInvitationService.InviteSubmissionResult result = competitionInvitationService.inviteByEmails(
                        competitionId,
                        athlete.getUserId(),
                        req.getParameter("emails"),
                        req
                );
                feedback = result.createdInvites().isEmpty()
                        ? "No invitations were created."
                        : "Created " + result.createdInvites().size() + " invitation(s).";
                if (!result.errors().isEmpty()) {
                    error = String.join("; ", result.errors());
                }
            } else if ("inviteCandidate".equals(action)) {
                String invitedUserId = req.getParameter("invitedUserId");
                String invitedEmail = req.getParameter("invitedEmail");
                CompetitionInvitationService.InviteRecord created = competitionInvitationService.inviteExistingUser(
                        competitionId,
                        athlete.getUserId(),
                        invitedUserId,
                        invitedEmail,
                        req
                );
                feedback = "Created invitation for " + created.invitedEmail();
            } else {
                throw new IllegalArgumentException("Unsupported invite action.");
            }

            CompetitionInvitationPageView view = competitionInvitationService.loadAdminPage(competitionId, req.getParameter("q"));
            req.setAttribute("adminAthlete", athlete);
            req.setAttribute("invitationAdminView", view);
            req.setAttribute("invitationAdminFeedback", feedback);
            req.setAttribute("invitationAdminError", error);
            req.getRequestDispatcher("/app/competition-invitations.jsp").forward(req, resp);
        } catch (IllegalStateException e) {
            resp.sendRedirect(req.getContextPath() + "/app/");
        } catch (IllegalArgumentException e) {
            try {
                long competitionId = resolveCompetitionId(req);
                BootcampAthlete athlete = requireAdminAthlete(req, competitionId);
                CompetitionInvitationPageView view = competitionInvitationService.loadAdminPage(competitionId, req.getParameter("q"));
                req.setAttribute("adminAthlete", athlete);
                req.setAttribute("invitationAdminView", view);
                req.setAttribute("invitationAdminError", e.getMessage());
                req.getRequestDispatcher("/app/competition-invitations.jsp").forward(req, resp);
            } catch (SQLException | ServletException ex) {
                throw new IOException("Unable to render competition invitations after validation error", ex);
            }
        } catch (ServletException e) {
            throw new IOException("Unable to render competition invitations", e);
        } catch (SQLException e) {
            throw new IOException("Unable to load competition invitations", e);
        }
    }

    private long resolveCompetitionId(HttpServletRequest req) throws SQLException {
        Long selectedCompetitionId = authSessionService.getSelectedCompetitionId(req);
        if (selectedCompetitionId != null) {
            return selectedCompetitionId;
        }
        String competitionId = req.getParameter("competitionId");
        if (competitionId != null && !competitionId.isBlank()) {
            return Long.parseLong(competitionId);
        }
        throw new IllegalStateException("Competition selection required.");
    }

    private BootcampAthlete requireAdminAthlete(HttpServletRequest req, long competitionId) throws SQLException {
        AuthenticatedUser user = authSessionService.getAuthenticatedUser(req);
        if (user == null) {
            throw new IllegalStateException("Login required");
        }
        BootcampAthlete athlete = authService.loadAthleteForUser(user);
        if (athlete == null || athlete.getId() == null || athlete.getId().isBlank() || !competitionInvitationService.isAdmin(competitionId, athlete.getId())) {
            throw new IllegalStateException("Admin access required");
        }
        return athlete;
    }
}
