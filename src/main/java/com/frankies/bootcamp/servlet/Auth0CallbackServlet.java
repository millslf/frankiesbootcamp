package com.frankies.bootcamp.servlet;

import com.frankies.bootcamp.model.AuthenticatedUser;
import com.frankies.bootcamp.model.BootcampAthlete;
import com.frankies.bootcamp.service.AuthService;
import com.frankies.bootcamp.service.AuthSessionService;
import com.frankies.bootcamp.service.ExternalAuthConfig;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import okhttp3.FormBody;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet(name = "auth0Callback", value = "/auth/external/callback")
public class Auth0CallbackServlet extends HttpServlet {
    private static final Logger log = Logger.getLogger(Auth0CallbackServlet.class);

    @Inject
    private ExternalAuthConfig config;
    @Inject
    private AuthService authService;
    @Inject
    private AuthSessionService authSessionService;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(true);
        String expectedState = (String) session.getAttribute("auth0State");
        String state = req.getParameter("state");
        String code = req.getParameter("code");

        if (expectedState == null || state == null || !expectedState.equals(state) || code == null || code.isBlank()) {
            authSessionService.clear(req);
            resp.sendRedirect(req.getContextPath() + "/");
            return;
        }

        session.removeAttribute("auth0State");

        try {
            JsonObject profile = exchangeCodeForProfile(req, code);
            String email = profile.has("email") ? profile.get("email").getAsString() : null;
            String providerSubject = profile.has("sub") ? profile.get("sub").getAsString() : null;
            String displayName = profile.has("name") ? profile.get("name").getAsString() : email;

            AuthenticatedUser user = authService.loginOrProvisionExternal("auth0", providerSubject, email, displayName);
            BootcampAthlete athlete = authService.loadAthleteForUser(user);
            req.changeSessionId();
            authSessionService.storeAuthenticatedUser(req, user, athlete);
            resp.sendRedirect(req.getContextPath() + "/app/");
        } catch (IllegalArgumentException | SQLException e) {
            authSessionService.clear(req);
            resp.sendRedirect(req.getContextPath() + "/");
        }
    }

    private JsonObject exchangeCodeForProfile(HttpServletRequest req, String code) throws IOException {
        OkHttpClient client = new OkHttpClient();
        String redirectUri = config.getBaseUrl() + req.getContextPath() + "/auth/external/callback";
        String clientSecret = config.getClientSecret();

        log.infof("Auth0 token exchange redirectUri=%s domain=%s clientId=%s secretLength=%d", redirectUri, config.getDomain(), config.getClientId(), clientSecret.length());

        RequestBody tokenBody = new FormBody.Builder()
                .add("grant_type", "authorization_code")
                .add("code", code)
                .add("redirect_uri", redirectUri)
                .build();

        Request tokenRequest = new Request.Builder()
                .url("https://" + config.getDomain() + "/oauth/token")
                .addHeader("Authorization", Credentials.basic(config.getClientId(), clientSecret))
                .post(tokenBody)
                .build();

        String accessToken;
        try (Response tokenResponse = client.newCall(tokenRequest).execute()) {
            String tokenResponseBody = tokenResponse.body().string();
            log.infof("Auth0 token response status=%d body=%s", tokenResponse.code(), tokenResponseBody);
            if (!tokenResponse.isSuccessful()) {
                throw new IOException("Auth0 token exchange failed with status " + tokenResponse.code());
            }
            JsonObject tokenJson = new Gson().fromJson(tokenResponseBody, JsonObject.class);
            accessToken = tokenJson.get("access_token").getAsString();
        }

        Request userInfoRequest = new Request.Builder()
                .url("https://" + config.getDomain() + "/userinfo")
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();

        try (Response userInfoResponse = client.newCall(userInfoRequest).execute()) {
            return new Gson().fromJson(userInfoResponse.body().string(), JsonObject.class);
        }
    }
}
