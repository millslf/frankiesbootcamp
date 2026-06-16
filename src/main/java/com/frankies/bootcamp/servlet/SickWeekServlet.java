package com.frankies.bootcamp.servlet;

import com.frankies.bootcamp.model.BootcampAthlete;
import com.frankies.bootcamp.service.ActivityProcessFacade;
import com.frankies.bootcamp.service.DBService;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jboss.logging.Logger;
import org.wildfly.security.credential.store.CredentialStoreException;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;

@WebServlet(name = "sickWeek", value = "/app/sick-week")
public class SickWeekServlet extends BootcampServlet {
    private static final Logger log = Logger.getLogger(SickWeekServlet.class);

    @Inject
    private DBService dbService;
    @Inject
    private ActivityProcessFacade activityProcessFacade;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        BootcampAthlete athlete = (BootcampAthlete) request.getAttribute("athlete");
        Long competitionId = (Long) request.getAttribute("selectedCompetitionId");
        String athleteEmail = (String) request.getAttribute("athleteEmail");

        if (athlete == null || athlete.getId() == null || competitionId == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "A selected competition is required to mark sick weeks.");
            return;
        }

        try {
            boolean selectedCompetitionIsCurrent = dbService.listCurrentActiveCompetitions(athlete.getId()).stream()
                    .anyMatch(competition -> competition.getId() == competitionId);
            if (!selectedCompetitionIsCurrent) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Sick weeks can only be changed for a current competition.");
                return;
            }
        } catch (SQLException e) {
            log.error("Unable to validate current competition before updating sick week for athlete " + athlete.getId()
                    + " competition " + competitionId, e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to validate competition.");
            return;
        }

        int weekNumber;
        try {
            weekNumber = Integer.parseInt(request.getParameter("week"));
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid week number.");
            return;
        }

        boolean sick = Boolean.parseBoolean(request.getParameter("sick"));
        try {
            dbService.setCompetitionSickWeek(competitionId, athlete.getId(), weekNumber, sick, athleteEmail);
            activityProcessFacade.prepareAthleteSummaryForCompetition(athlete, competitionId);
            response.sendRedirect(request.getContextPath() + "/app/");
        } catch (SQLException | CredentialStoreException | NoSuchAlgorithmException e) {
            log.error("Unable to update sick week " + weekNumber + " for athlete " + athlete.getId()
                    + " competition " + competitionId, e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to update sick week.");
        }
    }
}
