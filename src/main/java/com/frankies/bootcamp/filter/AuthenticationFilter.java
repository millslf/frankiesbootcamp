package com.frankies.bootcamp.filter;

import com.frankies.bootcamp.service.AuthSessionService;
import jakarta.inject.Inject;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Set;

@WebFilter(urlPatterns = "/*")
public class AuthenticationFilter implements Filter {
    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/", "/index.jsp", "/privacy", "/privacy.jsp", "/terms", "/terms.jsp", "/scoring", "/scoring.jsp",
            "/login", "/logout", "/error.jsp", "/api/strava/webhook",
            "/auth/external/login", "/auth/external/callback", "/app/whoami.jsp"
    );

    @Inject
    private AuthSessionService authSessionService;

    @Override
    public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        String path = req.getRequestURI().substring(req.getContextPath().length());

        if (isPublic(path) || isStatic(path)) {
            chain.doFilter(request, response);
            return;
        }

        if (authSessionService.getAuthenticatedUser(req) == null) {
            if (path.startsWith("/api/")) {
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            } else {
                if ("/app/".equals(path)) {
                    resp.sendRedirect(req.getContextPath() + "/app");
                } else {
                    resp.sendRedirect(req.getContextPath() + "/login");
                }
            }
            return;
        }

        chain.doFilter(request, response);
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
