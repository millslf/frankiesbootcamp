package com.frankies.bootcamp.rest;


import com.frankies.bootcamp.model.BootcampAthlete;
import com.frankies.bootcamp.model.AuthenticatedUser;
import com.frankies.bootcamp.service.ActivityProcessFacade;
import com.frankies.bootcamp.service.AuthSessionService;
import com.frankies.bootcamp.service.DBService;
import com.frankies.bootcamp.service.CompetitionAccessService;
import com.google.gson.Gson;
import jakarta.annotation.Resource;
import jakarta.inject.Inject;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import org.jboss.logging.Logger;

import java.net.http.HttpHeaders;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Path("/Athletes")
public class AthleteResource {
    @Inject
    private ActivityProcessFacade activityProcessFacade;
    @Inject
    private DBService db;
    @Inject
    private AuthSessionService authSessionService;
    @Inject
    private CompetitionAccessService competitionAccessService;

    @Resource
    private ManagedExecutorService executor;

    private static final Logger log = Logger.getLogger(AthleteResource.class);
    private static final AtomicBoolean FULL_REFRESH_IN_PROGRESS = new AtomicBoolean(false);
    @Context
    HttpHeaders requestHeaders;
    @Context
    HttpServletRequest request;

    @GET
    @Produces("application/json")
    @Path("/allAthleteSummary")
    public String allAthleteSummary(@QueryParam("forceRecalc") Boolean forceRecalc) {
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
                return queueFullRefresh(currentUser);
            }
            return new Gson().toJson(activityProcessFacade.getPerformanceList());
        } catch (SQLException e) {
            log.error("AthletesResource, allAthleteSummary", e);
            return "Something went wrong, phone a friend!";
        }
    }

    @POST
    @Produces("application/json")
    @Path("/maintenance/refresh-all")
    public String refreshAllCompetitionSummaries() {
        try {
            AuthenticatedUser currentUser = authSessionService.getAuthenticatedUser(request);
            if (currentUser == null) {
                return String.valueOf(HttpServletResponse.SC_UNAUTHORIZED);
            }
            BootcampAthlete loggedInAthlete = db.findAthleteByEmail(currentUser.getEmail());
            if (loggedInAthlete == null || !competitionAccessService.canAccessCompetitionSetup(currentUser)) {
                return String.valueOf(HttpServletResponse.SC_UNAUTHORIZED);
            }
            return queueFullRefresh(currentUser);
        } catch (SQLException e) {
            log.error("AthletesResource, refreshAllCompetitionSummaries", e);
            return "Something went wrong, phone a friend!";
        }
    }

    @GET
    @Produces("application/json")
    @Path("/personalSummary")
    public String personalSummary() {
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

    private String queueFullRefresh(AuthenticatedUser currentUser) {
        if (!FULL_REFRESH_IN_PROGRESS.compareAndSet(false, true)) {
            return new Gson().toJson(Map.of(
                    "status", "in_progress",
                    "message", "A full refresh is already running"
            ));
        }
        executor.submit(() -> {
            try {
                activityProcessFacade.prepareSummary();
                log.info("Completed full competition refresh request from " + currentUser.getEmail());
            } catch (Exception e) {
                log.warn("Unable to complete full competition refresh request from " + currentUser.getEmail(), e);
            } finally {
                FULL_REFRESH_IN_PROGRESS.set(false);
            }
        });
        return new Gson().toJson(Map.of(
                "status", "accepted",
                "message", "Full competition refresh started"
        ));
    }
}