package com.frankies.bootcamp.servlet;

import com.frankies.bootcamp.model.BootcampAthlete;
import com.frankies.bootcamp.model.AuthenticatedUser;
import com.frankies.bootcamp.model.OnboardingState;
import com.frankies.bootcamp.model.OnboardingStatus;
import com.frankies.bootcamp.model.CompetitionInvitationView;
import com.frankies.bootcamp.service.AuthService;
import com.frankies.bootcamp.service.OnboardingStateService;
import com.frankies.bootcamp.service.StravaService;
import com.frankies.bootcamp.service.AuthSessionService;
import com.frankies.bootcamp.service.DBService;
import com.frankies.bootcamp.service.CompetitionAccessService;
import com.frankies.bootcamp.service.CompetitionInvitationService;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;

public class BootcampServlet extends HttpServlet {
    private static final String SITE_HIT_LOGGED_SESSION_KEY = "siteHitLogged";

    @Inject
    private AuthSessionService authSessionService;
    @Inject
    private DBService dbService;
    @Inject
    private AuthService authService;
    @Inject
    private OnboardingStateService onboardingStateService;
    @Inject
    private StravaService stravaService;
    @Inject
    private CompetitionInvitationService competitionInvitationService;
    @Inject
    private CompetitionAccessService competitionAccessService;

    private static final Logger log = Logger.getLogger(BootcampServlet.class);

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        HttpSession session = req.getSession(true);
        AuthenticatedUser authenticatedUser = authSessionService.getAuthenticatedUser(req);
        BootcampAthlete athlete = null;

        if (authenticatedUser == null || authenticatedUser.getEmail() == null || authenticatedUser.getEmail().isBlank()) {
            unauthorized(resp, "Login required");
            return;
        }

        try {
            athlete = authService.loadAthleteForUser(authenticatedUser);
            req.setAttribute("competitionSetupAllowed", competitionAccessService.canAccessCompetitionSetup(authenticatedUser));
            if (session.getAttribute(SITE_HIT_LOGGED_SESSION_KEY) == null) {
                log.info("Site hit by " + authenticatedUser.getEmail());
                session.setAttribute(SITE_HIT_LOGGED_SESSION_KEY, Boolean.TRUE);
            }
            syncSelectedCompetition(req, athlete);
            OnboardingStatus onboardingStatus = onboardingStateService.resolve(authenticatedUser, athlete);
            applyOnboardingAttributes(req, session, authenticatedUser, athlete, onboardingStatus);
            applyInvitationAttributes(req, authenticatedUser, athlete);

            if (onboardingStatus.getState() == OnboardingState.STRAVA_PENDING) {
                log.debug("Strava link required: " + authenticatedUser.getEmail());
                forwardToStravaOnboarding(req, resp, authenticatedUser, onboardingStatus);
                return;
            }

            if (onboardingStatus.getState() == OnboardingState.COMPETITION_PENDING) {
                log.debug("Competition onboarding required: " + authenticatedUser.getEmail());
                req.getRequestDispatcher("/app/competition-onboarding.jsp").forward(req, resp);
                return;
            }

            if (onboardingStatus.getState() == OnboardingState.COMPETITION_STARTS_SOON) {
                log.debug("Competition starts soon for athlete: " + authenticatedUser.getEmail());
                req.getRequestDispatcher("/app/competition-starts-soon.jsp").forward(req, resp);
                return;
            }

            if (onboardingStatus.getState() == OnboardingState.COMPETITION_SELECTION_REQUIRED
                    && authSessionService.getSelectedCompetitionId(req) == null) {
                log.debug("Competition selection required for athlete: " + authenticatedUser.getEmail());
                req.getRequestDispatcher("/app/competition-selection.jsp").forward(req, resp);
                return;
            }

            if (onboardingStatus.getState() == OnboardingState.COMPETITION_HISTORY_ONLY
                    && authSessionService.getSelectedCompetitionId(req) == null) {
                log.debug("Only past competitions available for athlete: " + authenticatedUser.getEmail());
                req.getRequestDispatcher("/app/competition-history-only.jsp").forward(req, resp);
                return;
            }

            String pendingInviteToken = authSessionService.getPendingInvitationToken(req);
            if (pendingInviteToken != null && !isInvitationPath(req)) {
                CompetitionInvitationView invitation = competitionInvitationService.resolveInvitationToken(pendingInviteToken);
                if (invitation != null && invitation.isPending()) {
                    req.setAttribute("pendingInvitation", invitation);
                    req.getRequestDispatcher("/app/invitations").forward(req, resp);
                    return;
                }
                authSessionService.clearPendingInvitationToken(req);
            }

            if (athlete != null) {
                log.debug("Athlete authorised: " + buildDisplayName(athlete, authenticatedUser.getEmail()));
            }
        } catch (SQLException e) {
            log.error("Error resolving onboarding state", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        // Expose on the request for convenience
        req.setAttribute("athlete", athlete);
        req.setAttribute("athleteName", session.getAttribute("athleteName"));
        req.setAttribute("athleteEmail", session.getAttribute("athleteEmail"));

        super.service(req, resp);
    }

    private static String buildDisplayName(BootcampAthlete a, String fallbackEmail) {
        String first = safe(a.getFirstname());
        String last  = safe(a.getLastname());
        String name  = (first + " " + last).trim();
        if (name.isEmpty()) {
            int at = (fallbackEmail == null) ? -1 : fallbackEmail.indexOf('@');
            name = (at > 0) ? fallbackEmail.substring(0, at)
                    : (fallbackEmail != null ? fallbackEmail : "Athlete");
        }
        return name;
    }

    private static String safe(String s) { return s == null ? "" : s; }

    private static void unauthorized(HttpServletResponse resp, String message) throws IOException {
        resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        resp.setContentType("text/html; charset=UTF-8");
        try (PrintWriter out = resp.getWriter()) {
            out.println("<!doctype html><html><body>");
            out.println("<h1>401 " + message + "</h1>");
            out.println("<p><a href=\"/\">Back to home</a></p>");
            out.println("</body></html>");
        }
    }

    private void forwardToStravaOnboarding(HttpServletRequest req,
                                           HttpServletResponse resp,
                                           AuthenticatedUser authenticatedUser,
                                           OnboardingStatus onboardingStatus) throws ServletException, IOException {
        String clientId = stravaService.getClientId();
        String callback = stravaService.buildCallbackUrl(req);

        req.setAttribute("stravaClientId", clientId);
        req.setAttribute("stravaCallback", callback);
        req.setAttribute("stravaOnboardingUser", authenticatedUser);
        req.setAttribute("onboardingStatus", onboardingStatus);
        req.getRequestDispatcher("/app/strava-onboarding.jsp").forward(req, resp);
    }

    private void applyOnboardingAttributes(HttpServletRequest req,
                                           HttpSession session,
                                           AuthenticatedUser authenticatedUser,
                                           BootcampAthlete athlete,
                                           OnboardingStatus onboardingStatus) {
        req.setAttribute("onboardingStatus", onboardingStatus);
        req.setAttribute("onboardingState", onboardingStatus.getState());

        session.setAttribute("athlete", athlete);
        session.setAttribute("athleteEmail", authenticatedUser.getEmail());
        session.setAttribute("athleteName", authenticatedUser.getDisplayName());
        Long storedCompetitionId = authSessionService.getSelectedCompetitionId(req);
        Long selectedCompetitionId = defaultSelectedCompetitionId(storedCompetitionId, onboardingStatus);
        if (storedCompetitionId == null && selectedCompetitionId != null) {
            authSessionService.setSelectedCompetitionId(req, selectedCompetitionId);
        }
        req.setAttribute("activeCompetitions", onboardingStatus.getActiveCompetitions());
        req.setAttribute("pastCompetitions", onboardingStatus.getPastCompetitions());
        req.setAttribute("selectedCompetitionId", selectedCompetitionId);
    }

    private void applyInvitationAttributes(HttpServletRequest req, AuthenticatedUser authenticatedUser, BootcampAthlete athlete) {
        try {
            req.setAttribute("pendingCompetitionInvitations", competitionInvitationService.listPendingForUser(authenticatedUser, athlete));
            Long selectedCompetitionId = authSessionService.getSelectedCompetitionId(req);
            if (selectedCompetitionId != null && athlete != null && athlete.getId() != null && !athlete.getId().isBlank()) {
                req.setAttribute("selectedCompetitionAdmin", competitionInvitationService.isAdmin(selectedCompetitionId, athlete.getId()));
            } else {
                req.setAttribute("selectedCompetitionAdmin", Boolean.FALSE);
            }
        } catch (SQLException e) {
            log.error("Unable to load competition invitation attributes", e);
            throw new RuntimeException("Unable to load competition invitations", e);
        }
    }

    private boolean isInvitationPath(HttpServletRequest req) {
        String path = req.getRequestURI().substring(req.getContextPath().length());
        return "/app/invitations".equals(path) || "/app/invitations/respond".equals(path) || "/app/competition-invitations".equals(path);
    }

    static Long defaultSelectedCompetitionId(Long storedCompetitionId, OnboardingStatus onboardingStatus) {
        if (storedCompetitionId != null || onboardingStatus == null) {
            return storedCompetitionId;
        }
        if (onboardingStatus.getActiveCompetitions().size() == 1) {
            return onboardingStatus.getActiveCompetitions().get(0).getId();
        }
        return null;
    }

    private void syncSelectedCompetition(HttpServletRequest req, BootcampAthlete athlete) {
        Long selectedCompetitionId = authSessionService.getSelectedCompetitionId(req);
        if (selectedCompetitionId == null || athlete == null || athlete.getId() == null || athlete.getId().isBlank()) {
            return;
        }
        try {
            if (!dbService.athleteBelongsToCompetition(athlete.getId(), selectedCompetitionId)) {
                authSessionService.setSelectedCompetitionId(req, null);
                return;
            }
            req.setAttribute("selectedCompetitionId", selectedCompetitionId);
        } catch (SQLException e) {
            log.errorf(e, "Unable to validate selected competition athleteId=%s competitionId=%s", athlete.getId(), selectedCompetitionId);
            throw new RuntimeException("Unable to validate selected competition", e);
        }
    }

}
