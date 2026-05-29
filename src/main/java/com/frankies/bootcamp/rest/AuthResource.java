package com.frankies.bootcamp.rest;


import com.frankies.bootcamp.model.AuthenticatedUser;
import com.frankies.bootcamp.model.BootcampAthlete;
import com.frankies.bootcamp.service.AuthService;
import com.frankies.bootcamp.service.AuthSessionService;
import com.frankies.bootcamp.service.ActivityProcessService;
import com.frankies.bootcamp.service.StravaLinkConflictException;
import com.frankies.bootcamp.service.StravaService;
import com.frankies.bootcamp.service.DBService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.servlet.http.HttpServletRequest;
import org.jboss.logging.Logger;

import java.net.URI;
import java.sql.SQLException;

@Path("/Auth")
public class AuthResource {
    @Inject
    private StravaService stravaService;

    @Inject
    private AuthSessionService authSessionService;

    @Inject
    private AuthService authService;

    @Inject
    private ActivityProcessService activityProcessService;

    @Inject
    private DBService dbService;

    @Context
    private UriInfo uriInfo;

    @Context
    private HttpServletRequest request;

    private static final Logger log = Logger.getLogger(AuthResource.class);

    @GET
    @Produces("text/plain")
    public Response authenticate(@QueryParam("state") String state, @QueryParam("code")String code, @QueryParam("scope")String scope) {
        AuthenticatedUser currentUser = authSessionService.getAuthenticatedUser(request);
        if (currentUser == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Please log in before linking Strava.")
                    .type(MediaType.TEXT_PLAIN)
                    .build();
        }

        try {
            BootcampAthlete athlete = authService.loadAthleteForUser(currentUser);
            if (isAlreadyLinked(athlete)) {
                return Response.status(Response.Status.CONFLICT)
                        .entity("Strava is already linked for this account.")
                        .type(MediaType.TEXT_PLAIN)
                        .build();
            }
        } catch (SQLException e) {
            log.error("Auth, authenticate", e);
            return Response.serverError()
                    .entity("Something went wrong while checking your Strava link.")
                    .type(MediaType.TEXT_PLAIN)
                    .build();
        }

        if (scope == null || !scope.contains("activity:read")) {
            log.error("Auth, authenticate", new Exception("Insufficient scope"));
            return Response.status(Response.Status.FORBIDDEN)
                    .entity("Insufficient scope; need activity:read.")
                    .type(MediaType.TEXT_PLAIN)
                    .build();
        }

        if (code != null && !code.isBlank()) {
            try {
                BootcampAthlete linkedAthlete = stravaService.tokenExchange(code, currentUser.getUserId(), currentUser.getEmail());
                if (linkedAthlete != null) {
                    dbService.updateAuthUserAthleteId(currentUser.getUserId(), linkedAthlete.getId());
                    activityProcessService.prepareAthleteSummary(linkedAthlete);
                    request.getSession(true).setAttribute("athlete", linkedAthlete);
                    request.getSession().setAttribute("athleteEmail", currentUser.getEmail());
                    request.getSession().setAttribute("athleteName", currentUser.getDisplayName());
                }
            } catch (StravaLinkConflictException e) {
                return Response.status(Response.Status.CONFLICT)
                        .entity(e.getMessage())
                        .type(MediaType.TEXT_PLAIN)
                        .build();
            } catch (Exception e) {
                log.error("Auth, authenticate", new Exception("Something went wrong while authenticating"));
                return Response.serverError()
                        .entity("Something went wrong, phone a friend!")
                        .type(MediaType.TEXT_PLAIN)
                        .build();
            }
        }

        String defaultTarget = uriInfo.getBaseUri().getPath() + "app/";
        String target = (state != null && state.startsWith("/")) ? state : defaultTarget;

        if ("popup".equals(state)) {
            String html = """
                    <!doctype html><meta charset="utf-8"><title>Linked</title>
                    <script>
                      try {
                        // Tell the opener we're done (same origin!)
                        window.opener && window.opener.postMessage('strava-linked','https://www.frankiesbootcamp.com');
                      } catch(e) {}
                      window.close();
                      // Safety fallback if window couldn't close (e.g., same-tab auth)
                      setTimeout(function(){ location.href='/app/'; }, 500);
                    </script>
                    Linked. You can close this window.
                    """;
            return Response.ok(html).type(MediaType.TEXT_HTML).build();
        }
        return Response.seeOther(URI.create(target)).build();
    }

    private boolean isAlreadyLinked(BootcampAthlete athlete) {
        return athlete != null && athlete.getId() != null && !athlete.getId().startsWith("local-");
    }

}