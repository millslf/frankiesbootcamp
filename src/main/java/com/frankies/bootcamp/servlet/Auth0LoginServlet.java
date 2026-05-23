package com.frankies.bootcamp.servlet;

import com.frankies.bootcamp.service.ExternalAuthConfig;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@WebServlet(name = "auth0Login", value = "/auth/external/login")
public class Auth0LoginServlet extends HttpServlet {
    @Inject
    private ExternalAuthConfig config;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String state = randomToken();
        req.getSession(true).setAttribute("auth0State", state);

        String authorizeUrl = "https://" + config.getDomain() + "/authorize"
                + "?response_type=code"
                + "&client_id=" + url(config.getClientId())
                + "&redirect_uri=" + url(config.getBaseUrl() + req.getContextPath() + "/auth/external/callback")
                + "&scope=" + url("openid profile email")
                + "&state=" + url(state);

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
