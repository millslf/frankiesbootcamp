package com.frankies.bootcamp.servlet;

import com.frankies.bootcamp.service.AuthSessionService;
import com.frankies.bootcamp.service.ExternalAuthConfig;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet(name = "logout", value = "/logout")
public class LogoutServlet extends HttpServlet {
    @Inject
    private AuthSessionService authSessionService;
    @Inject
    private ExternalAuthConfig externalAuthConfig;

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        authSessionService.clear(req);
        String returnTo = externalAuthConfig.getBaseUrl() + req.getContextPath() + "/";
        String logoutUrl = "https://" + externalAuthConfig.getDomain()
                + "/v2/logout?client_id=" + url(externalAuthConfig.getClientId())
                + "&returnTo=" + url(returnTo);
        resp.sendRedirect(logoutUrl);
    }

    private String url(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }
}
