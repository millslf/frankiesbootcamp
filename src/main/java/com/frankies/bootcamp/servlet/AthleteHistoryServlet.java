package com.frankies.bootcamp.servlet;

import com.frankies.bootcamp.model.WeeklyPerformance;
import com.frankies.bootcamp.model.CompetitionSummaryView;
import com.frankies.bootcamp.service.ActivityProcessFacade;
import com.frankies.bootcamp.service.AuthSessionService;
import com.frankies.bootcamp.utils.DateTimeUtils;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jboss.logging.Logger;
import org.wildfly.security.credential.store.CredentialStoreException;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.frankies.bootcamp.service.AiMessageService;

@WebServlet(name = "athleteHistory", value = "/app/AthleteHistory")
public class AthleteHistoryServlet extends BootcampServlet {
    private static final Logger log = Logger.getLogger(AthleteHistoryServlet.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final long COMPLETED_COMPETITION_REBUILD_FREEZE_SECONDS = 14L * 24 * 60 * 60;

    @Inject
    private ActivityProcessFacade activityProcessFacade;
    @Inject
    private AuthSessionService authSessionService;
    @Inject
    private AiMessageService aiMessageService;

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        DecimalFormat df = new DecimalFormat("#.##");
        Map<Integer, WeeklyPerformance> history = new HashMap<>();
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        com.frankies.bootcamp.model.BootcampAthlete loggedInAthlete = (com.frankies.bootcamp.model.BootcampAthlete) request.getAttribute("athlete");
        String athleteName = loggedInAthlete.getFirstname();
        String authenticatedUserMail = (String) request.getAttribute("athleteEmail");
        Long selectedCompetitionId = (Long) request.getAttribute("selectedCompetitionId");
        history = selectedCompetitionId != null
                ? activityProcessFacade.getAthleteHistoryForCompetition(selectedCompetitionId, loggedInAthlete.getId())
                : activityProcessFacade.getAthleteHistory(loggedInAthlete.getId());
        if (history == null) {
            history = new HashMap<>();
        }
        CompetitionSummaryView selectedCompetition = findSelectedCompetition(request, selectedCompetitionId);
        boolean selectedCompetitionIsCurrent = selectedCompetitionId == null || isCurrentCompetition(request, selectedCompetitionId);
        boolean hideMotivationalMessage = authSessionService.isHistoryMotivationalMessageHidden(request);
        if (history.isEmpty() || selectedActiveCompetitionHistoryIsStale(history, selectedCompetition, selectedCompetitionIsCurrent)) {
            try {
                if (selectedCompetitionId == null) {
                    activityProcessFacade.prepareAthleteSummary(loggedInAthlete);
                    history = activityProcessFacade.getAthleteHistory(loggedInAthlete.getId());
                } else if (!isCompletedBeyondRebuildWindow(selectedCompetition)) {
                    activityProcessFacade.prepareAthleteSummaryForCompetition(loggedInAthlete, selectedCompetitionId);
                    history = activityProcessFacade.getAthleteHistoryForCompetition(selectedCompetitionId, loggedInAthlete.getId());
                }
                if (history == null) {
                    history = new HashMap<>();
                }
            } catch (SQLException | CredentialStoreException | NoSuchAlgorithmException e) {
                log.warn("Unable to prepare missing competition history for athlete " + loggedInAthlete.getId()
                        + " competition " + selectedCompetitionId, e);
            }
        }
        int numberOfWeeksSinceStart = activityProcessFacade.getNumberOfWeeksSinceStart();
        int latestHistoryWeek = history.keySet().stream().mapToInt(Integer::intValue).max().orElse(numberOfWeeksSinceStart);
        int displayWeekCount = selectedCompetitionId == null ? numberOfWeeksSinceStart : latestHistoryWeek;

        out.println("<html><head>");
        out.println("</head><body>");

        out.println("<div class='container'>");

        out.println("<h2 class='history-heading'>");
        out.println("<i class='bi bi-clock-history'></i> My Weekly Competition History");
        out.println("</h2>");
        out.println("<p class='history-subheading'><i class='bi bi-graph-up-arrow'></i> Track how you performed each week across all activities, see your progress, week by week!</p>");
        if (selectedCompetition != null) {
            out.println(buildCompetitionTimingSummary(selectedCompetition, displayWeekCount, selectedCompetitionIsCurrent));
        }

        if (history.isEmpty()) {
            out.println("<div class='alert alert-info'>We are getting things ready for your Strava account. Your history should appear shortly.</div>");
            out.println("</div>");
            out.println("</body></html>");
            return;
        }

        WeeklyPerformance latest = history.get(displayWeekCount);
        if (latest != null) {
            double distance = latest.getTotalDistance();
            double goal = latest.getWeekGoal();
            boolean hitGoal = latest.getTotalPercentOfGoal() >= 1.0;

            Integer leaderboardRank = null;
            try {
                Map<String, HashMap<String, Double>> sortedSummaries = selectedCompetitionId != null
                        ? activityProcessFacade.getSortedSummariesForCompetition(selectedCompetitionId)
                        : activityProcessFacade.getSortedSummaries(loggedInAthlete.getId());
                int rank = 1;
                Map<String, Double> scoreSummary = sortedSummaries.get(com.frankies.bootcamp.constant.BootcampConstants.currentYearlyScoreSummary);
                for (String athleteLabel : scoreSummary.keySet()) {
                    if (athleteLabel.equalsIgnoreCase(authenticatedUserMail) || athleteLabel.equalsIgnoreCase(athleteName)) {
                        leaderboardRank = rank;
                        break;
                    }
                    rank++;
                }
            } catch (RuntimeException e) {
                log.warn("Unable to calculate leaderboard rank for " + authenticatedUserMail, e);
            }

            Map<String, Double> sports = selectedCompetitionIsCurrent
                    ? latest.getSports()
                    : aggregateSports(history);
            if (sports == null) {
                sports = new LinkedHashMap<>();
            }
            String favouriteSports = com.frankies.bootcamp.sport.SportsUtils.getFavouriteSports(sports, 2);
            String suggestedSport = com.frankies.bootcamp.sport.SportsUtils.getSuggestedSport(sports.keySet());

            if (selectedCompetitionIsCurrent && !hideMotivationalMessage) {
                String aiMsg = aiMessageService.getMotivationalMessage(
                        athleteName, distance, goal, hitGoal, leaderboardRank, favouriteSports, suggestedSport
                );
                out.println("<div class='alert alert-info history-subheading d-flex justify-content-between align-items-start gap-3' style='margin-bottom:1em;' role='alert'>");
                out.println("<div>" + aiMsg + "</div>");
                out.println("<form method='post' action='" + request.getContextPath() + "/app/AthleteHistory' class='m-0'>");
                out.println("<input type='hidden' name='action' value='hideMotivationalMessage'>");
                out.println("<button type='submit' class='btn-close' aria-label='Dismiss ZenBot message'></button>");
                out.println("</form>");
                out.println("</div>");
            }
        }
        out.println("<div class='table-responsive mt-4'>");
        out.println("<table class='table table-bordered table-striped align-middle'>");
        out.println("<thead class='table-dark'>");
        out.println("<tr>");
        out.println("<th><i class='bi bi-calendar'></i> Week</th>");
        out.println("<th><i class='bi bi-rulers'></i> Distance completed</th>");
        out.println("<th class='col-goal'><i class='bi bi-bullseye'></i> Goal</th>");
        out.println("<th><i class='bi bi-graph-up'></i> % of goal</th>");
        out.println("<th><i class='bi bi-signpost'></i> Distance left</th>");
        out.println("<th><i class='bi bi-star-fill'></i> Progression Points scored</th>");
        out.println("<th><i class='bi bi-star-fill'></i> Goal Points scored</th>");
        out.println("<th><i class='bi bi-star-fill'></i> Total Points scored</th>");
        out.println("<th class='col-activities'><i class='bi bi-activity'></i> Activities</th>");
        if (selectedCompetitionId != null && selectedCompetitionIsCurrent) {
            out.println("<th><i class='bi bi-heart-pulse-fill'></i> Sick week</th>");
        }
        out.println("</tr>");
        out.println("</thead>");
        out.println("<tbody>");
        for (int i = displayWeekCount; i >= 1; i--) {
            WeeklyPerformance weekHistory = history.get(i);
            if (weekHistory == null) {
                continue;
            }
            out.println("<tr>");
            String weekText = weekHistory.getWeek();
            if(weekHistory.isSick()){
                weekText += " <i class='bi bi-heart-pulse-fill' title='Sick Note'></i>";
            }
            if (selectedCompetitionIsCurrent && i == displayWeekCount) {
                out.println("<td>" + weekText +
                        "</br><i>(Week in progress)" + "</td>");
            } else {
                out.println("<td>" + weekText + "</td>");
            }
            out.println("<td>" + df.format(weekHistory.getTotalDistance()) + " km </td>");
            out.println("<td>" + df.format(weekHistory.getWeekGoal()) + "km</td>");
            out.println("<td>" + df.format(weekHistory.getTotalPercentOfGoal() * 100) + "%</td>");
            out.println("<td>" + df.format(weekHistory.getWeekGoal()-weekHistory.getTotalDistance()>0?weekHistory.getWeekGoal()-weekHistory.getTotalDistance():0) + "km</td>");
            if(weekHistory.isSick()){
                out.println("<td> <i class='bi bi-heart-pulse-fill' title='Sick Note'></i></td>");
                out.println("<td> <i class='bi bi-heart-pulse-fill' title='Sick Note'></i></td>");
            }
            else{
                out.println("<td>" + df.format(weekHistory.getWeekProgressionBonus()) + "</td>");
                out.println("<td>" + df.format(weekHistory.getWeekGoalAchievementScore()) + "</td>");
            }
            out.println("<td>" + df.format(weekHistory.getWeekScore()) + "</td>");
            out.println("<td>");
            for (String key : weekHistory.getSports().keySet()) {
                out.println(key + "(x" + weekHistory.getSportsCount().get(key) + ") " + df.format(weekHistory.getSports().get(key)) + "km");
                if (weekHistory.getSportsOriginalDistance().containsKey(key)) {
                    out.println(" (" + df.format(weekHistory.getSportsOriginalDistance().get(key)) + "km)</br>");
                } else if (weekHistory.getSportsOriginalDuration().containsKey(key)) {
                    out.println(" (" + DateTimeUtils.convertMinutesToTimeFormat(weekHistory.getSportsOriginalDuration().get(key)*60) + ")</br>");
                }
            }
            out.println("</td>");
            if (selectedCompetitionId != null && selectedCompetitionIsCurrent) {
                out.println("<td>");
                out.println("<form method='post' action='" + request.getContextPath() + "/app/sick-week' class='sick-week-form'>");
                out.println("<input type='hidden' name='week' value='" + i + "'>");
                out.println("<input type='hidden' name='sick' value='" + (!weekHistory.isSick()) + "'>");
                out.println("<button type='submit' class='btn btn-sm " + (weekHistory.isSick() ? "btn-outline-secondary" : "btn-outline-danger") + "'>");
                out.println(weekHistory.isSick() ? "Clear sick" : "Mark sick");
                out.println("</button>");
                out.println("</form>");
                out.println("</td>");
            }
            out.println("</tr>");
        }
        out.println("</tbody>");
        out.println("</table>");
        out.println("</div>");
        out.println("</div>");
        out.println("</body></html>");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String action = request.getParameter("action");
        if ("hideMotivationalMessage".equals(action)) {
            authSessionService.hideHistoryMotivationalMessage(request);
        }
        response.sendRedirect(request.getContextPath() + "/app/");
    }

    private static boolean isCurrentCompetition(HttpServletRequest request, Long selectedCompetitionId) {
        return findCompetitionInAttribute(request, "activeCompetitions", selectedCompetitionId) != null;
    }

    private static boolean isCompletedBeyondRebuildWindow(CompetitionSummaryView competition) {
        return competition != null
                && competition.getEndTimestamp() != null
                && competition.getEndTimestamp() < Instant.now().getEpochSecond() - COMPLETED_COMPETITION_REBUILD_FREEZE_SECONDS;
    }

    private static boolean selectedActiveCompetitionHistoryIsStale(Map<Integer, WeeklyPerformance> history,
                                                                   CompetitionSummaryView selectedCompetition,
                                                                   boolean selectedCompetitionIsCurrent) {
        if (!selectedCompetitionIsCurrent || selectedCompetition == null || history.isEmpty()) {
            return false;
        }
        int latestPersistedWeek = history.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
        return latestPersistedWeek < expectedWeekCount(selectedCompetition);
    }

    private static int expectedWeekCount(CompetitionSummaryView competition) {
        long effectiveEndMillis = competition.getEndTimestamp() == null
                ? System.currentTimeMillis()
                : Math.min(System.currentTimeMillis(), competition.getEndTimestamp() * 1000);
        int weeks = (int) Math.ceil((double) (effectiveEndMillis - (competition.getStartTimestamp() * 1000)) /
                (com.frankies.bootcamp.constant.BootcampConstants.WEEK_IN_SECONDS * 1000));
        return Math.max(1, weeks);
    }

    private static double totalDistance(Map<Integer, WeeklyPerformance> history) {
        return history.values().stream().mapToDouble(week -> week.getTotalDistance() == null ? 0.0 : week.getTotalDistance()).sum();
    }

    private static double totalScore(Map<Integer, WeeklyPerformance> history) {
        return history.values().stream().mapToDouble(week -> week.getWeekScore() == null ? 0.0 : week.getWeekScore()).sum();
    }

    private static Map<String, Double> aggregateSports(Map<Integer, WeeklyPerformance> history) {
        Map<String, Double> sports = new LinkedHashMap<>();
        for (WeeklyPerformance week : history.values()) {
            if (week.getSports() == null) {
                continue;
            }
            for (Map.Entry<String, Double> sport : week.getSports().entrySet()) {
                sports.merge(sport.getKey(), sport.getValue(), Double::sum);
            }
        }
        return sports;
    }

    private static CompetitionSummaryView findSelectedCompetition(HttpServletRequest request, Long selectedCompetitionId) {
        if (selectedCompetitionId == null) {
            Object activeCompetitionsAttr = request.getAttribute("activeCompetitions");
            if (activeCompetitionsAttr instanceof List<?> activeCompetitions && activeCompetitions.size() == 1
                    && activeCompetitions.get(0) instanceof CompetitionSummaryView competition) {
                return competition;
            }
            return null;
        }

        CompetitionSummaryView active = findCompetitionInAttribute(request, "activeCompetitions", selectedCompetitionId);
        return active != null ? active : findCompetitionInAttribute(request, "pastCompetitions", selectedCompetitionId);
    }

    private static CompetitionSummaryView findCompetitionInAttribute(HttpServletRequest request, String attributeName, Long competitionId) {
        Object competitionsAttr = request.getAttribute(attributeName);
        if (!(competitionsAttr instanceof List<?> competitions)) {
            return null;
        }
        for (Object item : competitions) {
            if (item instanceof CompetitionSummaryView competition && competition.getId() == competitionId) {
                return competition;
            }
        }
        return null;
    }

    private static String buildCompetitionTimingSummary(CompetitionSummaryView competition, int displayWeekCount, boolean current) {
        ZoneId zone = ZoneId.of(competition.getTimezone());
        LocalDate startDate = Instant.ofEpochSecond(competition.getStartTimestamp()).atZone(zone).toLocalDate();
        LocalDate endDate = competition.getEndTimestamp() == null ? null : Instant.ofEpochSecond(competition.getEndTimestamp()).atZone(zone).toLocalDate();
        LocalDate today = LocalDate.now(zone);
        long daysRemaining = endDate == null ? -1 : Math.max(0, ChronoUnit.DAYS.between(today, endDate));
        long weeksRemaining = endDate == null ? -1 : (long) Math.ceil(daysRemaining / 7.0);

        StringBuilder html = new StringBuilder();
        html.append("<details class='competition-timing-summary'>")
                .append("<summary>")
                .append("<span><strong>").append(competition.getName()).append("</strong></span>")
                .append("<span>Runs ").append(startDate.format(DATE_FORMATTER));
        if (endDate != null) {
            html.append(" to ").append(endDate.format(DATE_FORMATTER));
        } else {
            html.append(" with no end date");
        }
        html.append("</span>")
                .append("</summary>")
                .append("<div>Week ").append(displayWeekCount);
        if (current) {
            html.append(" is in progress");
        } else {
            html.append(" is the final recorded week");
        }
        html.append(". Weeks start every ").append(startDate.getDayOfWeek()).append(", matching the competition start day.</div>");
        if (endDate != null) {
            html.append("<div>").append(daysRemaining).append(" day").append(daysRemaining == 1 ? "" : "s")
                    .append(" left, about ").append(weeksRemaining).append(" week").append(weeksRemaining == 1 ? "" : "s").append(" remaining.</div>");
        }
        html.append("</details>");
        return html.toString();
    }
}
