package com.frankies.bootcamp.servlet;

import com.frankies.bootcamp.model.AthleteProfileBlurb;
import com.frankies.bootcamp.model.BootcampAthlete;
import com.frankies.bootcamp.model.CompetitionInsights;
import com.frankies.bootcamp.model.PerformanceResponse;
import com.frankies.bootcamp.service.ActivityProcessFacade;
import com.frankies.bootcamp.service.AiMessageService;
import com.frankies.bootcamp.service.CompetitionInsightsService;
import com.frankies.bootcamp.service.DBService;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@WebServlet(name = "athleteProfileSummary", value = "/app/AthleteProfileSummary")
public class AthleteProfileSummaryServlet extends BootcampServlet {
    private static final Logger log = Logger.getLogger(AthleteProfileSummaryServlet.class);
    private static final int MAX_SUMMARY_LENGTH = 600;

    @Inject
    private DBService dbService;

    @Inject
    private AiMessageService aiMessageService;

    @Inject
    private ActivityProcessFacade activityProcessFacade;

    @Inject
    private CompetitionInsightsService competitionInsightsService;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        BootcampAthlete loggedInAthlete = (BootcampAthlete) request.getAttribute("athlete");
        if (loggedInAthlete == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String athleteId = trimToEmpty(request.getParameter("athleteId"));
        if (athleteId.isBlank()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Athlete is required.");
            return;
        }

        try {
            String summaryType = trimToEmpty(request.getParameter("summaryType"));
            AthleteProfileBlurb verified = "performance".equals(summaryType) ? null : dbService.getVerifiedAthleteProfileBlurb(athleteId);
            String text;
            boolean isVerified;
            if (verified != null) {
                text = verified.text();
                isVerified = true;
            } else if ("performance".equals(summaryType)) {
                BootcampAthlete profileAthlete = dbService.findAthleteByStravaID(athleteId);
                String name = profileAthlete == null ? "This athlete" : displayName(profileAthlete);
                text = aiMessageService.generateAthletePerformanceSummary(name, performanceContext(request, athleteId, profileAthlete));
                isVerified = false;
            } else {
                BootcampAthlete profileAthlete = dbService.findAthleteByStravaID(athleteId);
                String name = profileAthlete == null ? "This athlete" : displayName(profileAthlete);
                text = aiMessageService.generateAthleteProfileBlurb(name, stableProfileContext(profileAthlete));
                isVerified = false;
            }

            response.setContentType("application/json; charset=UTF-8");
            response.getWriter().write("{\"verified\":" + isVerified + ",\"text\":\"" + jsonEscape(text) + "\"}");
        } catch (SQLException e) {
            log.error("Could not generate athlete profile summary for " + athleteId, e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        BootcampAthlete loggedInAthlete = (BootcampAthlete) request.getAttribute("athlete");
        if (loggedInAthlete == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String athleteId = trimToEmpty(request.getParameter("athleteId"));
        if (!loggedInAthlete.getId().equals(athleteId)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String summaryText = trimToEmpty(request.getParameter("summaryText"));
        if (summaryText.length() > MAX_SUMMARY_LENGTH) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Profile summary must be 600 characters or fewer.");
            return;
        }

        try {
            if (summaryText.isBlank()) {
                dbService.deleteAthleteProfileBlurb(athleteId);
            } else {
                dbService.saveVerifiedAthleteProfileBlurb(athleteId, summaryText);
            }
        } catch (SQLException e) {
            log.error("Could not save athlete profile summary for " + athleteId, e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        response.sendRedirect(request.getContextPath() + "/app?tab=insights");
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private String displayName(BootcampAthlete athlete) {
        String name = (trimToEmpty(athlete.getFirstname()) + " " + trimToEmpty(athlete.getLastname())).trim();
        return name.isBlank() ? "This athlete" : name;
    }

    private String stableProfileContext(BootcampAthlete athlete) {
        return "Write a timeless personal profile, not a competition update. " +
                "Do not mention ranks, points, weeks, current performance, private distances, or kilometres. " +
                pronounInstruction(athlete) + " " +
                "You may invent harmless, obviously light-hearted personality colour such as favourite day, favourite chore, " +
                "or suspiciously specific hobbies, but avoid sensitive personal facts.";
    }

    private String performanceContext(HttpServletRequest request, String athleteId, BootcampAthlete athlete) {
        Object selectedCompetitionId = request.getAttribute("selectedCompetitionId");
        if (!(selectedCompetitionId instanceof Long competitionId)) {
            return "No current competition context is available. " + pronounInstruction(athlete);
        }
        List<PerformanceResponse> performances = activityProcessFacade.getPerformanceListForCompetition(competitionId);
        CompetitionInsights insights = competitionInsightsService.buildInsights(performances, athleteId, false);
        CompetitionInsights.AthleteProfileSummary profile = insights.selectedAthleteProfile();
        if (profile == null || !athleteId.equals(profile.athleteId())) {
            return "No current competition context is available. " + pronounInstruction(athlete);
        }
        return "latest available competition scores including the current week: overall rank #" + profile.overallRank()
                + ", total points " + profile.totalScore()
                + ", active weeks " + profile.activeWeeks()
                + ", goal-crusher weeks " + profile.goalCrushWeeks()
                + ", strongest sport " + profile.strongestSport()
                + ", best week " + (profile.bestWeekNumber() == 0 ? "none yet" : "Week " + profile.bestWeekNumber())
                + ". Do not mention private distances or kilometres. " + pronounInstruction(athlete);
    }

    private String pronounInstruction(BootcampAthlete athlete) {
        if (athlete == null || athlete.getSex() == null) {
            return "Do not guess gender from the name; use they/them pronouns or avoid pronouns.";
        }
        return switch (athlete.getSex().trim().toUpperCase()) {
            case "F" -> "Strava reports this athlete as female; use she/her pronouns where natural.";
            case "M" -> "Strava reports this athlete as male; use he/him pronouns where natural.";
            default -> "Do not guess gender from the name; use they/them pronouns or avoid pronouns.";
        };
    }

    private String jsonEscape(String value) {
        return trimToEmpty(value)
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }
}
