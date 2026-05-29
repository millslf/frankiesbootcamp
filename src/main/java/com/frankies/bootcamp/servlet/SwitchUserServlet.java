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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@WebServlet(name = "switchUser", value = "/switch-user")
public class SwitchUserServlet extends HttpServlet {
    @Inject
    private AuthSessionService authSessionService;

    @Inject
    private ExternalAuthConfig externalAuthConfig;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        authSessionService.clear(req);

        String returnTo = externalAuthConfig.getBaseUrl() + req.getContextPath() + "/";
        String logoutUrl = "https://" + externalAuthConfig.getDomain() + "/v2/logout?client_id="
                + URLEncoder.encode(externalAuthConfig.getClientId(), StandardCharsets.UTF_8)
                + "&returnTo="
                + URLEncoder.encode(returnTo, StandardCharsets.UTF_8);

        resp.sendRedirect(logoutUrl);
    }
}
