package com.frankies.bootcamp.service;

import com.frankies.bootcamp.model.AuthenticatedUser;
import com.frankies.bootcamp.model.BootcampAthlete;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@ApplicationScoped
public class AuthSessionService {
    public static final String AUTH_USER_SESSION_KEY = "authUser";
    public static final String SELECTED_COMPETITION_ID_SESSION_KEY = "selectedCompetitionId";
    public static final String LAST_VIEWED_COMPETITION_ID_SESSION_KEY = "lastViewedCompetitionId";
    public static final String PENDING_INVITATION_TOKEN_SESSION_KEY = "pendingCompetitionInvitationToken";
    public static final String HISTORY_MOTIVATIONAL_MESSAGE_HIDDEN_SESSION_KEY = "historyMotivationalMessageHidden";
    private static final int DEFAULT_SESSION_TIMEOUT_SECONDS = 60 * 60 * 24 * 30;

    public AuthenticatedUser getAuthenticatedUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object authUser = session.getAttribute(AUTH_USER_SESSION_KEY);
        if (authUser instanceof AuthenticatedUser user) {
            return user;
        }
        return null;
    }

    public void storeAuthenticatedUser(HttpServletRequest request, AuthenticatedUser user, BootcampAthlete athlete) {
        HttpSession session = request.getSession(true);
        session.setMaxInactiveInterval(DEFAULT_SESSION_TIMEOUT_SECONDS);
        session.setAttribute(AUTH_USER_SESSION_KEY, user);
        session.setAttribute("athlete", athlete);
        session.setAttribute("athleteEmail", user.getEmail());
        session.setAttribute("athleteName", user.getDisplayName());
    }

    public void clear(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    public Long getSelectedCompetitionId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object selectedCompetitionId = session.getAttribute(SELECTED_COMPETITION_ID_SESSION_KEY);
        if (selectedCompetitionId instanceof Long competitionId) {
            return competitionId;
        }
        if (selectedCompetitionId instanceof String competitionIdText && !competitionIdText.isBlank()) {
            return Long.parseLong(competitionIdText);
        }
        return null;
    }

    public void setSelectedCompetitionId(HttpServletRequest request, Long competitionId) {
        HttpSession session = request.getSession(true);
        if (competitionId == null) {
            session.removeAttribute(SELECTED_COMPETITION_ID_SESSION_KEY);
            return;
        }
        session.setAttribute(SELECTED_COMPETITION_ID_SESSION_KEY, competitionId);
        session.setAttribute(LAST_VIEWED_COMPETITION_ID_SESSION_KEY, competitionId);
    }

    public boolean hasSelectedCompetition(HttpServletRequest request) {
        return getSelectedCompetitionId(request) != null;
    }

    public Long getLastViewedCompetitionId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object lastViewedCompetitionId = session.getAttribute(LAST_VIEWED_COMPETITION_ID_SESSION_KEY);
        if (lastViewedCompetitionId instanceof Long competitionId) {
            return competitionId;
        }
        if (lastViewedCompetitionId instanceof String competitionIdText && !competitionIdText.isBlank()) {
            return Long.parseLong(competitionIdText);
        }
        return null;
    }

    public String getPendingInvitationToken(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object token = session.getAttribute(PENDING_INVITATION_TOKEN_SESSION_KEY);
        return token instanceof String value ? value : null;
    }

    public void setPendingInvitationToken(HttpServletRequest request, String token) {
        HttpSession session = request.getSession(true);
        if (token == null || token.isBlank()) {
            session.removeAttribute(PENDING_INVITATION_TOKEN_SESSION_KEY);
            return;
        }
        session.setAttribute(PENDING_INVITATION_TOKEN_SESSION_KEY, token);
    }

    public void clearPendingInvitationToken(HttpServletRequest request) {
        setPendingInvitationToken(request, null);
    }

    public boolean isHistoryMotivationalMessageHidden(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }
        Object hidden = session.getAttribute(HISTORY_MOTIVATIONAL_MESSAGE_HIDDEN_SESSION_KEY);
        return hidden instanceof Boolean value && value;
    }

    public void hideHistoryMotivationalMessage(HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        session.setAttribute(HISTORY_MOTIVATIONAL_MESSAGE_HIDDEN_SESSION_KEY, Boolean.TRUE);
    }
}
