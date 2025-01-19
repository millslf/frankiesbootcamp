package com.frankies.bootcamp.rest;


import com.frankies.bootcamp.constant.BootcampConstants;
import com.frankies.bootcamp.model.BootcampAthlete;
import com.frankies.bootcamp.model.PerformanceResponse;
import com.frankies.bootcamp.service.ActivityProcessService;
import com.frankies.bootcamp.service.DBService;
import com.google.gson.Gson;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import org.jboss.logging.Logger;
import org.wildfly.security.credential.store.CredentialStoreException;

import java.io.IOException;
import java.net.http.HttpHeaders;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static com.frankies.bootcamp.constant.BootcampConstants.START_TIMESTAMP;

@Path("/Athletes")
public class AthleteResource {
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
                                    @QueryParam("startTimeStamp") Long startTimeStamp,
                                    @QueryParam("sendReport") Boolean sendReport,
                                    @QueryParam("reportToDevOnly") Boolean reportToDevOnly) {
        try {
            DBService db = new DBService();
            BootcampAthlete loggedInAthlete = db.findAthleteByEmail(userEmail);
            if (loggedInAthlete == null ||
                    !loggedInAthlete.getEmail().equals("millslf@gmail.com")) {
                return String.valueOf(HttpServletResponse.SC_UNAUTHORIZED);
            }
            int numberOfWeeksSinceStart = (int) Math.round(Math.ceil((double) (System.currentTimeMillis() - (startTimeStamp * 1000)) / (BootcampConstants.WEEK_IN_SECONDS * 1000)));
            List<PerformanceResponse> performanceList = new ArrayList<>();
            ActivityProcessService activityProcessService = new ActivityProcessService();
            performanceList = activityProcessService.prepareSummary(performanceList, startTimeStamp, numberOfWeeksSinceStart);
            activityProcessService.sendReport(performanceList, numberOfWeeksSinceStart, sendReport, reportToDevOnly, loggedInAthlete.getEmail());
            return new Gson().toJson(performanceList);
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
            DBService db = new DBService();
            BootcampAthlete loggedInAthlete = db.findAthleteByEmail(userEmail);
            if (loggedInAthlete == null) {
                return HttpServletResponse.SC_UNAUTHORIZED + " Athlete not authorised";
            }
            int numberOfWeeksSinceStart = (int) Math.round(Math.ceil((double) (System.currentTimeMillis() - (START_TIMESTAMP * 1000)) / (BootcampConstants.WEEK_IN_SECONDS * 1000)));
            List<PerformanceResponse> performanceList = new ArrayList<>();
            ActivityProcessService activityProcessService = new ActivityProcessService();
            performanceList = activityProcessService.prepareSummary(performanceList, START_TIMESTAMP, numberOfWeeksSinceStart);
            return activityProcessService.sendReport(performanceList, numberOfWeeksSinceStart, false, false, loggedInAthlete.getEmail());
        } catch (IOException | CredentialStoreException | NoSuchAlgorithmException | SQLException e) {
            log.error("AthletesResource, allAthleteSummary", e);
            return "Something went wrong, phone a friend!";
        }
    }
}