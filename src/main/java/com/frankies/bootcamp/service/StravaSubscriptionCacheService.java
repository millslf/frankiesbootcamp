package com.frankies.bootcamp.service;

import com.frankies.bootcamp.utils.WildflyUtils;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.annotation.JsonbProperty;
import org.wildfly.security.credential.store.CredentialStoreException;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class StravaSubscriptionCacheService {

    private static final Logger LOG = Logger.getLogger(StravaSubscriptionCacheService.class.getName());

    private static final String STRAVA_API_BASE = System.getProperty("STRAVA_API_BASE", "https://www.strava.com/api/v3");
    private static final String EXPECTED_CALLBACK_URL = System.getProperty("STRAVA_CALLBACK_URL");

    private final HttpClient http = HttpClient.newHttpClient();
    private final Jsonb jsonb = JsonbBuilder.create();

    private volatile Long subscriptionId;   // cached
    private volatile String  callbackUrl;      // cached (for visibility/debug)

    @PostConstruct
    void init() {
        try {
            refresh();  // load at startup
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to load Strava subscriptions at startup", e);
        }
    }

    /**
     * Force a reload from Strava and update the cached subscription id.
     */
    public synchronized void refresh() throws IOException, InterruptedException, CredentialStoreException, NoSuchAlgorithmException {
        WildflyUtils wf = new WildflyUtils();
        String clientID = wf.giveMeAPass("stravaClientId");
        String clientSecret = wf.giveMeAPass("stravaClientSecret");

        if (clientID.isBlank() || clientSecret.isBlank()) {
            LOG.warning("STRAVA_CLIENT_ID or STRAVA_CLIENT_SECRET is not set; cannot fetch push_subscriptions.");
            return;
        }

        String url = STRAVA_API_BASE
                + "/push_subscriptions?client_id=" + urlEnc(clientID)
                + "&client_secret=" + urlEnc(clientSecret);

        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .GET()
                .header("Accept", "application/json")
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() / 100 != 2) {
            LOG.warning(() -> "Strava push_subscriptions returned " + resp.statusCode() + ": " + resp.body());
            return;
        }

        PushSubscription[] subs = jsonb.fromJson(resp.body(), PushSubscription[].class);
        if (subs == null || subs.length == 0) {
            LOG.warning("No Strava push subscriptions found for this client.");
            return;
        }

        PushSubscription chosen = choose(subs);
        this.subscriptionId = chosen.id;
        this.callbackUrl = chosen.callbackUrl;

        LOG.info(() -> "Cached Strava subscription: id=" + subscriptionId + ", callback_url=" + callbackUrl);
    }

    /**
     * Returns the cached subscription id (may be null if not loaded/found).
     */
    public Long getSubscriptionId() {
        return subscriptionId;
    }

    /**
     * Pick a subscription. Prefer the one matching STRAVA_CALLBACK_URL (if provided),
     * otherwise fall back to the first.
     */
    private PushSubscription choose(PushSubscription[] subs) {
        if (EXPECTED_CALLBACK_URL != null && !EXPECTED_CALLBACK_URL.isBlank()) {
            Optional<PushSubscription> match = Arrays.stream(subs)
                    .filter(s -> EXPECTED_CALLBACK_URL.equalsIgnoreCase(s.callbackUrl))
                    .findFirst();
            if (match.isPresent()) return match.get();
            LOG.warning(() -> "No subscription matched STRAVA_CALLBACK_URL=" + EXPECTED_CALLBACK_URL
                    + " – falling back to the first subscription.");
        }
        return subs[0];
    }

    private static String urlEnc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    // --- minimal DTO for Strava's GET /push_subscriptions ---
    public static final class PushSubscription {
        public Long id;
        @JsonbProperty("callback_url")
        public String callbackUrl;

        @Override public String toString() {
            return "PushSubscription{id=" + id + ", callbackUrl='" + callbackUrl + "'}";
        }
    }
}
