package com.frankies.bootcamp.filter;

import com.frankies.bootcamp.model.AuthenticatedUser;
import com.frankies.bootcamp.service.AuthService;
import com.frankies.bootcamp.service.AuthSessionService;
import jakarta.inject.Inject;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Set;

@WebFilter(urlPatterns = "/*")
public class AuthenticationFilter implements Filter {
    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/", "/index.jsp", "/privacy", "/privacy.jsp", "/terms", "/terms.jsp", "/scoring", "/scoring.jsp",
            "/login", "/logout", "/error.jsp",
            "/auth/external/login", "/auth/external/callback", "/app/whoami.jsp"
    );

    @Inject
    private AuthSessionService authSessionService;
    @Inject
    private AuthService authService;

    @Override
    public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        String path = req.getRequestURI().substring(req.getContextPath().length());

        bootstrapHeaderSession(req);

        if (isPublic(path) || isStatic(path)) {
            chain.doFilter(request, response);
            return;
        }

        if (authSessionService.getAuthenticatedUser(req) == null) {
            if (path.startsWith("/api/")) {
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            } else {
                resp.sendRedirect(req.getContextPath() + "/login");
            }
            return;
        }

        chain.doFilter(request, response);
    }

    private void bootstrapHeaderSession(HttpServletRequest req) {
        if (authSessionService.getAuthenticatedUser(req) != null) {
            return;
        }

        String email = req.getHeader("Ngrok-Auth-User-Email");
        if (email == null || email.isBlank()) {
            return;
        }

        try {
            AuthenticatedUser user = authService.resolveAuthenticatedUser(email);
            if (user == null) {
                return;
            }
            authSessionService.storeAuthenticatedUser(req, user, authService.loadAthleteForUser(user));
        } catch (SQLException ignored) {
        }
    }

    private boolean isPublic(String path) {
        return PUBLIC_PATHS.contains(path);
    }

    private boolean isStatic(String path) {
        return path.startsWith("/styles/") || path.startsWith("/images/") || path.startsWith("/assets/")
                || path.startsWith("/js/") || path.startsWith("/WEB-INF/") || path.endsWith(".css")
                || path.endsWith(".js") || path.endsWith(".png") || path.endsWith(".jpg")
                || path.endsWith(".svg") || path.endsWith(".ico") || path.endsWith(".woff2")
                || path.endsWith(".jspf");
    }
}
