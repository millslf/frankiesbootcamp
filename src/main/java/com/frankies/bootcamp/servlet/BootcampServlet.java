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
                    renderStravaOnboarding(req, resp, authenticatedUser);
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

    private void renderStravaOnboarding(HttpServletRequest req, HttpServletResponse resp, AuthenticatedUser user) throws IOException {
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("text/html; charset=UTF-8");
        String displayName = user.getDisplayName() == null || user.getDisplayName().isBlank() ? "there" : user.getDisplayName();
        String ctx = req.getContextPath();
        try (PrintWriter out = resp.getWriter()) {
            out.println("<!doctype html><html lang=\"en\"><head>");
            out.println("<meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">");
            out.println("<title>Link Strava</title>");
            out.println("<link href=\"https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css\" rel=\"stylesheet\">");
            out.println("<link rel=\"stylesheet\" href=\"https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css\">");
            out.println("</head><body class=\"bg-light\">\n<div class=\"container py-5\">\n  <div class=\"row justify-content-center\">\n    <div class=\"col-lg-8\">\n      <div class=\"card shadow-sm border-0\">\n        <div class=\"card-body p-4 p-md-5\">\n          <div class=\"text-center mb-4\">\n            <div class=\"display-5 text-primary mb-3\"><i class=\"bi bi-link-45deg\"></i></div>");
            out.println("            <h1 class=\"h3 mb-3\">Link your Strava account</h1>");
            out.println("            <p class=\"text-muted mb-0\">You are signed in as <strong>" + escapeHtml(displayName) + "</strong>, but your Bootcamp account is not linked to Strava yet.</p>");
            out.println("          </div>");
            out.println("          <div class=\"alert alert-primary\" role=\"alert\">Link Strava to unlock weekly history, leaderboard, honour roll, and performance summaries.</div>");
            out.println("          <div class=\"d-grid gap-2 d-sm-flex justify-content-sm-center mt-4\">\n            <button class=\"btn btn-primary btn-lg\" onclick=\"linkStravaPopup()\"><i class=\"bi bi-strava me-2\"></i>Link Strava</button>\n            <a class=\"btn btn-outline-secondary btn-lg\" href=\"" + ctx + "/\">Back to home</a>\n          </div>");
            out.println("        </div>\n      </div>\n    </div>\n  </div>\n</div>");
            out.println("<script>function linkStravaPopup(){const callback='https://www.frankiesbootcamp.com/api/Auth';const authUrl='https://www.strava.com/oauth/authorize'+'?client_id=143025'+'&redirect_uri='+encodeURIComponent(callback)+'&response_type=code'+'&scope=activity:read'+'&state=popup';const w=window.open(authUrl,'stravaAuth','width=520,height=720');function onMsg(e){if(e.origin==='https://www.frankiesbootcamp.com'&&e.data==='strava-linked'){window.removeEventListener('message',onMsg);location.reload();}}window.addEventListener('message',onMsg);}</script>");
            out.println("</body></html>");
        }
    }

    private String escapeHtml(String value) {
        return value == null ? "" : value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
