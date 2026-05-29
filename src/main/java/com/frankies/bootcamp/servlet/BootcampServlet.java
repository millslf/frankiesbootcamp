package com.frankies.bootcamp.servlet;

import com.frankies.bootcamp.model.BootcampAthlete;
import com.frankies.bootcamp.model.AuthenticatedUser;
import com.frankies.bootcamp.service.AuthService;
import com.frankies.bootcamp.service.AuthSessionService;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;

public class BootcampServlet extends HttpServlet {
    @Inject
    private AuthSessionService authSessionService;
    @Inject
    private AuthService authService;

    private static final Logger log = Logger.getLogger(BootcampServlet.class);

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        HttpSession session = req.getSession(true);
        BootcampAthlete athlete = (BootcampAthlete) session.getAttribute("athlete");
        AuthenticatedUser authenticatedUser = authSessionService.getAuthenticatedUser(req);

        if (athlete == null) {
            if (authenticatedUser == null || authenticatedUser.getEmail() == null || authenticatedUser.getEmail().isBlank()) {
                unauthorized(resp, "Login required");
                return;
            }
            try {
                athlete = authService.loadAthleteForUser(authenticatedUser);
                if (athlete == null || isStravaPending(athlete)) {
                    log.info("Strava link required: " + authenticatedUser.getEmail());
                    req.setAttribute("stravaOnboardingUser", authenticatedUser);
                    req.getRequestDispatcher("/app/strava-onboarding.jsp").forward(req, resp);
                    return;
                }

                // Cache in session for future requests
                session.setAttribute("athlete", athlete);
                session.setAttribute("athleteEmail", authenticatedUser.getEmail());
                session.setAttribute("athleteName", authenticatedUser.getDisplayName());
                log.info("Athlete authorised: " + buildDisplayName(athlete, authenticatedUser.getEmail()));
            } catch (SQLException e) {
                log.error("Error looking up athlete by email", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }
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

    private boolean isStravaPending(BootcampAthlete athlete) {
        return athlete.getId() == null || athlete.getId().isBlank() || athlete.getId().startsWith("local-");
    }

}
