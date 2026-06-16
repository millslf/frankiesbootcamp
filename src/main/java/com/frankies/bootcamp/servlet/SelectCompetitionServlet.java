package com.frankies.bootcamp.servlet;

import com.frankies.bootcamp.model.AuthenticatedUser;
import com.frankies.bootcamp.model.BootcampAthlete;
import com.frankies.bootcamp.service.AuthSessionService;
import com.frankies.bootcamp.service.AuthService;
import com.frankies.bootcamp.service.ActivityProcessFacade;
import com.frankies.bootcamp.service.DBService;
import jakarta.annotation.Resource;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@WebServlet(name = "selectCompetition", value = "/app/select-competition")
public class SelectCompetitionServlet extends HttpServlet {
    private static final Logger log = Logger.getLogger(SelectCompetitionServlet.class);

    @Inject
    private AuthSessionService authSessionService;

    @Inject
    private AuthService authService;

    @Inject
    private DBService dbService;

    @Inject
    private ActivityProcessFacade activityProcessFacade;

    @Resource
    private ManagedExecutorService executor;

    private static final long COMPLETED_COMPETITION_REBUILD_FREEZE_SECONDS = 14L * 24 * 60 * 60;
    private static final Set<Long> COMPETITION_REBUILDS_IN_PROGRESS = ConcurrentHashMap.newKeySet();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String competitionId = req.getParameter("competitionId");
        String returnTo = req.getParameter("returnTo");
        if (competitionId != null && !competitionId.isBlank()) {
            try {
                AuthenticatedUser user = authSessionService.getAuthenticatedUser(req);
                BootcampAthlete athlete = user == null ? null : authService.loadAthleteForUser(user);
                long parsedCompetitionId = Long.parseLong(competitionId);
                if (athlete != null && athlete.getId() != null && dbService.athleteBelongsToCompetition(athlete.getId(), parsedCompetitionId)) {
                    prepareSnapshotIfNeeded(athlete, parsedCompetitionId);
                    authSessionService.setSelectedCompetitionId(req, parsedCompetitionId);
                } else {
                    authSessionService.setSelectedCompetitionId(req, null);
                }
            } catch (SQLException e) {
                log.error("Unable to select competition " + competitionId, e);
                throw new IOException("Unable to select competition", e);
            }
        }
        if (returnTo != null && !returnTo.isBlank()) {
            resp.sendRedirect(req.getContextPath() + returnTo);
            return;
        }
        resp.sendRedirect(req.getContextPath() + "/app");
    }

    private void prepareSnapshotIfNeeded(BootcampAthlete athlete, long competitionId) throws SQLException {
        try {
            if (dbService.competitionHasIncompletePersistentState(competitionId)) {
                prepareCompetitionSnapshotAsync(competitionId);
            } else if (shouldPrepareCompetitionSnapshot(athlete.getId(), competitionId)) {
                prepareAthleteSnapshotAsync(athlete, competitionId);
            }
        } catch (SQLException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Unable to prepare competition snapshot for athlete " + athlete.getId()
                    + " competition " + competitionId + "; serving persisted snapshot as-is", e);
        }
    }

    private void prepareCompetitionSnapshotAsync(long competitionId) {
        if (!COMPETITION_REBUILDS_IN_PROGRESS.add(competitionId)) {
            log.info("Competition rebuild already in progress for competition " + competitionId);
            return;
        }
        log.info("Starting background competition rebuild for competition " + competitionId);
        executor.submit(() -> {
            try {
                activityProcessFacade.prepareCompetitionSummary(competitionId);
                log.info("Completed background competition rebuild for competition " + competitionId);
            } catch (Exception e) {
                log.warn("Unable to complete background competition rebuild for competition " + competitionId, e);
            } finally {
                COMPETITION_REBUILDS_IN_PROGRESS.remove(competitionId);
            }
        });
    }

    private void prepareAthleteSnapshotAsync(BootcampAthlete athlete, long competitionId) {
        log.info("Starting background athlete rebuild for athlete " + athlete.getId() + " competition " + competitionId);
        executor.submit(() -> {
            try {
                activityProcessFacade.prepareAthleteSummaryForCompetition(athlete, competitionId);
                log.info("Completed background athlete rebuild for athlete " + athlete.getId() + " competition " + competitionId);
            } catch (Exception e) {
                log.warn("Unable to complete background athlete rebuild for athlete " + athlete.getId()
                        + " competition " + competitionId, e);
            }
        });
    }

    private boolean shouldPrepareCompetitionSnapshot(String athleteId, long competitionId) throws SQLException {
        Long competitionAthleteId = dbService.findCompetitionAthleteId(athleteId, competitionId);
        if (competitionAthleteId == null) {
            return false;
        }

        DBService.CompetitionAthleteConfig config = dbService.getCompetitionAthleteConfig(competitionAthleteId);
        if (isCompletedBeyondRebuildWindow(config)) {
            return false;
        }

        DBService.PersistentAthleteSummarySnapshot snapshot = dbService.getPersistentAthleteSummarySnapshot(athleteId, competitionId);
        if (snapshot == null) {
            return true;
        }
        if (dbService.getPersistentAthleteHistory(athleteId, competitionId).isEmpty()) {
            return true;
        }

        return Math.abs(snapshot.originalWeeklyGoal() - config.startingGoal()) > 0.0001;
    }

    private static boolean isCompletedBeyondRebuildWindow(DBService.CompetitionAthleteConfig config) {
        return config.endTimestamp() != null
                && config.endTimestamp() < Instant.now().getEpochSecond() - COMPLETED_COMPETITION_REBUILD_FREEZE_SECONDS;
    }
}
