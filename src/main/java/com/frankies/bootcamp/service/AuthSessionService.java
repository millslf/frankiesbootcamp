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
    }
}
