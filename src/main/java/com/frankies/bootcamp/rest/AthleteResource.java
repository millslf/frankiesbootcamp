package com.frankies.bootcamp.rest;


import com.frankies.bootcamp.model.BootcampAthlete;
import com.frankies.bootcamp.service.ActivityProcessService;
import com.frankies.bootcamp.service.DBService;
import com.google.gson.Gson;
import jakarta.inject.Inject;
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
    private ActivityProcessService activityProcessService;
    @Inject
    private DBService db;

    private static final Logger log = Logger.getLogger(AthleteResource.class);
    @Context
    HttpHeaders requestHeaders;

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
            BootcampAthlete loggedInAthlete = db.findAthleteByEmail(userEmail);
            if (loggedInAthlete == null ||
                    !loggedInAthlete.getEmail().equals("millslf@gmail.com")) {
                return String.valueOf(HttpServletResponse.SC_UNAUTHORIZED);
            }
            if(forceRecalc != null && forceRecalc) {
                activityProcessService.prepareSummary();
                activityProcessService.generateAllSummaryMaps();
            }
            activityProcessService.sendReport(sendReport, reportToDevOnly, loggedInAthlete.getEmail());
            return new Gson().toJson(activityProcessService.getPerformanceList());
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
            BootcampAthlete loggedInAthlete = db.findAthleteByEmail(userEmail);
            if (loggedInAthlete == null) {
                return HttpServletResponse.SC_UNAUTHORIZED + " Athlete not authorised";
            }
            return activityProcessService.sendReport(false, false, loggedInAthlete.getEmail());
        } catch (IOException | CredentialStoreException | NoSuchAlgorithmException | SQLException e) {
            log.error("AthletesResource, allAthleteSummary", e);
            return "Something went wrong, phone a friend!";
        }
    }
}