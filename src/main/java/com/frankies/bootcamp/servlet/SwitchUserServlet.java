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
import java.security.SecureRandom;
import java.util.Base64;

@WebServlet(name = "switchUser", value = "/switch-user")
public class SwitchUserServlet extends HttpServlet {
    @Inject
    private AuthSessionService authSessionService;

    @Inject
    private ExternalAuthConfig externalAuthConfig;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Clear local session
        authSessionService.clear(req);

        // Create a new state token and store in session so callback can validate
        String state = randomToken();
        req.getSession(true).setAttribute("auth0State", state);

        // Redirect straight to the Auth0 hosted login (authorize) page and force the login prompt
        String authorizeUrl = "https://" + externalAuthConfig.getDomain() + "/authorize"
                + "?response_type=code"
                + "&client_id=" + url(externalAuthConfig.getClientId())
                + "&redirect_uri=" + url(externalAuthConfig.getBaseUrl() + req.getContextPath() + "/auth/external/callback")
                + "&scope=" + url("openid profile email")
                + "&state=" + url(state)
                + "&prompt=login"; // force re-auth / allow user to switch accounts

        resp.sendRedirect(authorizeUrl);
    }

    private String randomToken() {
        byte[] bytes = new byte[24];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String url(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
