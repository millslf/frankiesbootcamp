
package com.frankies.bootcamp.rest;
import com.frankies.bootcamp.model.BootcampAthlete;
import com.frankies.bootcamp.model.strava.StravaActivityResponse;
import com.frankies.bootcamp.model.strava.StravaEvent;
import com.frankies.bootcamp.service.ActivityProcessService;
import com.frankies.bootcamp.service.DBService;
import com.frankies.bootcamp.service.StravaService;
import com.frankies.bootcamp.service.StravaSubscriptionCacheService;
import jakarta.annotation.Resource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Map;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import org.wildfly.security.credential.store.CredentialStoreException;

@ApplicationScoped
@Path("/strava/webhook")
public class StravaWebhook {

    @Inject
    private ActivityProcessService activityProcessService;
    @Inject
    private StravaService stravaService;
    @Inject
    DBService db;

    // Configure from env; fallbacks are only for local dev
    private static final String VERIFY_TOKEN  = System.getProperty("STRAVA_VERIFY_TOKEN", "");

    @Resource
    ManagedExecutorService executor;

    @Inject
    StravaSubscriptionCacheService subCache;

    private static final Logger log = Logger.getLogger(StravaWebhook.class);

    // ---- Verification handshake (Strava calls GET when creating the subscription) ----
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response verify(
            @QueryParam("hub.mode") String mode,
            @QueryParam("hub.verify_token") String token,
            @QueryParam("hub.challenge") String challenge
    ) {
        if ("subscribe".equalsIgnoreCase(mode) && VERIFY_TOKEN.equals(token)) {
            JsonObject obj = Json.createObjectBuilder()
                    .add("hub.challenge", challenge == null ? "" : challenge)
                    .build();
            return Response.ok(obj).build();
        }
        return Response.status(Response.Status.FORBIDDEN).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response receive(StravaEvent event) {
        // 1) Basic validation of body
        if (event == null || !"activity".equalsIgnoreCase(event.objectType)) {
            return Response.ok().build(); // ignore non-activity or malformed
        }

        Long expected = subCache.getSubscriptionId(); // may be null if not loaded
        if (expected != null && !expected.equals(event.subscriptionId)) {
            // Ignore events not meant for this subscription instance
            return Response.ok().build();
        }

        // 3) ACK fast
        Response ok = Response.ok().build();

        // 4) Process async (keep webhook snappy)
        executor.submit(() -> {
            Long athleteId = event.ownerId;
            Long activityId = event.objectId;

            switch (event.aspectType) {
                case "create" -> {
                    try {
                        onActivityCreated(athleteId, activityId);
                    } catch (SQLException | CredentialStoreException | NoSuchAlgorithmException | IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                case "update" -> {
                    if (event.isHidden()) onActivityHidden(athleteId, activityId, event.updates);
                    else onActivityUpdated(athleteId, activityId, event.updates);
                }
                case "delete" -> {
                    try {
                        onActivityDeleted(athleteId, activityId);
                    } catch (SQLException | CredentialStoreException | NoSuchAlgorithmException | IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                default -> { /* ignore */ }
            }

            // Optional “authenticity” check: fetch the activity using your stored
            // athlete token and ensure ownerId matches before persisting.
            // (Strava doesn’t sign events.)
            // fetchFullActivityAndPersist(athleteId, activityId);
        });

        return ok;
    }

    private void onActivityCreated(Long athleteId, Long activityId) throws SQLException, CredentialStoreException, NoSuchAlgorithmException, IOException {
        BootcampAthlete athlete = db.findAthleteByStravaID(athleteId.toString());
        athlete = stravaService.refreshToken(athlete);
        StravaActivityResponse stravaActivityResponse = stravaService.getActivityById(activityId, athlete.getAccessToken(), false);
        activityProcessService.addActivityEvent(athleteId.toString(), stravaActivityResponse);
        log.info("StravaWebhook, onActivityCreated" + athleteId + " " + activityId);
    }

    private void onActivityUpdated(Long athleteId, Long activityId, Map<String, String> updates) {
        //Too much trouble to implement for now, at the moment everything is recalculated daily, so no need for this...
        log.info("StravaWebhook, onActivityUpdated " +  athleteId + " " + activityId + " " + updates);
    }

    private void onActivityHidden(Long athleteId, Long activityId, Map<String, String> updates) {
        activityProcessService.removeActivityEvent(athleteId.toString(), activityId);
        log.info("StravaWebhook, onActivityHidden " +  athleteId + " " + activityId + " " + updates);
    }

    private void onActivityDeleted(Long athleteId, Long activityId) throws SQLException, CredentialStoreException, NoSuchAlgorithmException, IOException {
        activityProcessService.removeActivityEvent(athleteId.toString(), activityId);
        log.info("StravaWebhook, onActivityDeleted " +  athleteId + " " + activityId);
    }

}
