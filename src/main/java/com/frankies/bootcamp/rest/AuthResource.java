package com.frankies.bootcamp.rest;


import com.frankies.bootcamp.service.StravaService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.jboss.logging.Logger;

import java.net.URI;

@Path("/Auth")
public class AuthResource {
    @Inject
    private StravaService stravaService;

    @Context
    private UriInfo uriInfo;

    private static final Logger log = Logger.getLogger(AuthResource.class);

    @GET
    @Produces("text/plain")
    public Response authenticate(@QueryParam("state") String state, @QueryParam("code")String code, @QueryParam("scope")String scope) {
        if (scope == null || !scope.contains("activity:read")) {
            log.error("Auth, authenticate", new Exception("Insufficient scope"));
            return Response.status(Response.Status.FORBIDDEN)
                    .entity("Insufficient scope; need activity:read.")
                    .type(MediaType.TEXT_PLAIN)
                    .build();
        }

        boolean tokenExchanged = false;
        if (code != null && !code.isBlank()) {
            try {
                tokenExchanged = stravaService.tokenExchange(code);
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

}