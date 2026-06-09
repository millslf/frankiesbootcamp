package com.frankies.bootcamp.rest;


import com.frankies.bootcamp.model.BootcampAthlete;
import com.frankies.bootcamp.model.AuthenticatedUser;
import com.frankies.bootcamp.service.ActivityProcessFacade;
import com.frankies.bootcamp.service.AuthSessionService;
import com.frankies.bootcamp.service.DBService;
import com.google.gson.Gson;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import org.jboss.logging.Logger;
import org.wildfly.security.credential.store.CredentialStoreException;

import java.io.IOException;
import java.net.http.HttpHeaders;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;

@Path("/Athletes")
public class AthleteResource {
    @Inject
    private ActivityProcessFacade activityProcessFacade;
    @Inject
    private DBService db;
    @Inject
    private AuthSessionService authSessionService;

    private static final Logger log = Logger.getLogger(AthleteResource.class);
    @Context
    HttpHeaders requestHeaders;
    @Context
    HttpServletRequest request;

    @GET
    @Produces("application/json")
    @Path("/allAthleteSummary")
    public String allAthleteSummary(@HeaderParam("Ngrok-Auth-User-Email") String userEmail,
                                    @HeaderParam("Ngrok-Auth-User-Id") String UserId,
                                    @HeaderParam("Ngrok-Auth-User-Name") String userName,
                                    @HeaderParam("Referer") String referer,
                                    @HeaderParam("User-Agent") String theUserAgent,
                                    @QueryParam("sendReport") Boolean sendReport,
                                    @QueryParam("forceRecalc") Boolean forceRecalc,
                                    @QueryParam("reportToDevOnly") Boolean reportToDevOnly) {
        try {
            AuthenticatedUser currentUser = authSessionService.getAuthenticatedUser(request);
            if (currentUser == null) {
                return String.valueOf(HttpServletResponse.SC_UNAUTHORIZED);
            }
            BootcampAthlete loggedInAthlete = db.findAthleteByEmail(currentUser.getEmail());
            if (loggedInAthlete == null ||
                    !loggedInAthlete.getEmail().equals("millslf@gmail.com")) {
                return String.valueOf(HttpServletResponse.SC_UNAUTHORIZED);
            }
            if(forceRecalc != null && forceRecalc) {
                activityProcessFacade.prepareSummary();
            }
            return new Gson().toJson(activityProcessFacade.getPerformanceList());
        } catch (IOException | CredentialStoreException | NoSuchAlgorithmException | SQLException e) {
            log.error("AthletesResource, allAthleteSummary", e);
            return "Something went wrong, phone a friend!";
        }
    }

    @GET
    @Produces("application/json")
    @Path("/personalSummary")
    public String personalSummary(@HeaderParam("Ngrok-Auth-User-Email") String userEmail,
                                  @HeaderParam("Ngrok-Auth-User-Id") String UserId,
                                  @HeaderParam("Ngrok-Auth-User-Name") String userName,
                                  @HeaderParam("Referer") String referer,
                                  @HeaderParam("User-Agent") String theUserAgent) {
        try {
            AuthenticatedUser currentUser = authSessionService.getAuthenticatedUser(request);
            if (currentUser == null) {
                return HttpServletResponse.SC_UNAUTHORIZED + " Login required";
            }
            BootcampAthlete loggedInAthlete = db.findAthleteByEmail(currentUser.getEmail());
            if (loggedInAthlete == null) {
                return HttpServletResponse.SC_UNAUTHORIZED + " Athlete not authorised";
            }
            return new Gson().toJson(activityProcessFacade.getAthleteHistory(currentUser.getEmail()));
        } catch (SQLException e) {
            log.error("AthletesResource, allAthleteSummary", e);
            return "Something went wrong, phone a friend!";
        }
    }
}