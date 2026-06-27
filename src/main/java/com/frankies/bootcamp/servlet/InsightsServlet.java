package com.frankies.bootcamp.servlet;

import com.frankies.bootcamp.model.BootcampAthlete;
import com.frankies.bootcamp.model.AthleteProfileBlurb;
import com.frankies.bootcamp.model.CompetitionInsights;
import com.frankies.bootcamp.model.CompetitionSummaryView;
import com.frankies.bootcamp.model.PerformanceResponse;
import com.frankies.bootcamp.service.ActivityProcessFacade;
import com.frankies.bootcamp.service.CompetitionInsightsService;
import com.frankies.bootcamp.service.DBService;
import com.frankies.bootcamp.utils.WildflyUtils;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@WebServlet(name = "insights", value = "/app/Insights")
public class InsightsServlet extends BootcampServlet {
    private static final Logger log = Logger.getLogger(InsightsServlet.class);

    @Inject
    private ActivityProcessFacade activityProcessFacade;

    @Inject
    private CompetitionInsightsService competitionInsightsService;

    @Inject
    private DBService dbService;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        BootcampAthlete loggedInAthlete = (BootcampAthlete) request.getAttribute("athlete");
        Long selectedCompetitionId = (Long) request.getAttribute("selectedCompetitionId");
        if (loggedInAthlete == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        String fragment = trimToEmpty(request.getParameter("fragment"));
        if ("profile".equals(fragment)) {
            renderProfileFragment(request, response, loggedInAthlete, selectedCompetitionId);
            return;
        }

        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.println("<div class='container'>");
        out.println("<h2 class='history-heading'><i class='bi bi-lightbulb-fill'></i> Competition Insights</h2>");
        if (selectedCompetitionId == null) {
            out.println("<div class='alert alert-info'>Select a competition to view insights.</div>");
            out.println("</div>");
            return;
        }

        List<PerformanceResponse> performances = activityProcessFacade.getPerformanceListForCompetition(selectedCompetitionId);
        boolean excludeLatestWeek = selectedCompetitionIsActive(request, selectedCompetitionId);
        CompetitionInsights insights = competitionInsightsService.buildInsights(performances, loggedInAthlete.getId(), excludeLatestWeek);

        out.println("<p class='history-subheading'>Trends, standings, and athlete highlights. Distances stay private. Active competitions use completed weeks only.</p>");
        renderRankTrend(out, insights);
        renderWeeklyHistory(out, insights);
        renderSportStandings(out, insights);
        out.println("</div>");
    }

    private void renderProfileFragment(HttpServletRequest request, HttpServletResponse response, BootcampAthlete loggedInAthlete, Long selectedCompetitionId) throws IOException {
        String athleteId = trimToEmpty(request.getParameter("athleteId"));
        if (athleteId.isBlank()) {
            athleteId = loggedInAthlete.getId();
        }
        if (athleteId == null || athleteId.isBlank()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Athlete is required.");
            return;
        }

        List<PerformanceResponse> performances = activityProcessFacade.getPerformanceListForCompetition(selectedCompetitionId);
        boolean excludeLatestWeek = selectedCompetitionIsActive(request, selectedCompetitionId);
        CompetitionInsights insights = competitionInsightsService.buildInsights(performances, athleteId, excludeLatestWeek);
        CompetitionInsights.AthleteProfileSummary profile = insights.selectedAthleteProfile();
        if (profile == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();
        AthleteProfileBlurb verifiedBlurb = null;
        try {
            verifiedBlurb = dbService.getVerifiedAthleteProfileBlurb(profile.athleteId());
        } catch (SQLException e) {
            log.warn("Could not load verified athlete profile blurb for " + profile.athleteId(), e);
        }
        String profileSummaryAction = request.getContextPath() + "/app/AthleteProfileSummary";
        String profileSummaryGenerateUrl = request.getContextPath() + "/app/AthleteProfileSummary";
        renderProfileContent(out, profile, verifiedBlurb, loggedInAthlete.getId(), profileSummaryAction, profileSummaryGenerateUrl);
    }

    private void renderRankTrend(PrintWriter out, CompetitionInsights insights) {
        out.println("<section class='mb-4'>");
        out.println("<h3 class='h5'>Position over time</h3>");
        out.println("<p class='text-muted small'>Each line shows weekly overall rank based on cumulative points. Higher is better.</p>");
        if (insights.rankTrends().isEmpty() || insights.rankTrends().get(0).positions().isEmpty()) {
            out.println("<div class='alert alert-info'>No completed weeks are available for the rank graph yet.</div>");
            out.println("</section>");
            return;
        }

        int width = 980;
        int height = 380;
        int left = 190;
        int right = 70;
        int top = 30;
        int bottom = 55;
        int plotWidth = width - left - right;
        int plotHeight = height - top - bottom;
        int weekCount = insights.rankTrends().stream()
                .flatMap(trend -> trend.positions().stream())
                .mapToInt(CompetitionInsights.RankPosition::weekNumber)
                .max()
                .orElse(1);
        int athleteCount = Math.max(1, insights.rankTrends().size());

        out.println("<div class='rank-trend-card border rounded p-3 bg-white overflow-auto'>");
        out.println("<svg viewBox='0 0 " + width + " " + height + "' role='img' aria-label='Athlete position over time chart' style='min-width:760px;width:100%;height:auto;'>");
        out.println("<rect x='0' y='0' width='" + width + "' height='" + height + "' fill='white'/>");
        for (int rank = 1; rank <= athleteCount; rank++) {
            double y = rankY(rank, athleteCount, top, plotHeight);
            out.println("<line x1='" + left + "' y1='" + y + "' x2='" + (left + plotWidth) + "' y2='" + y + "' stroke='#e9ecef' stroke-width='1'/>");
            out.println("<text x='" + (left + plotWidth + 12) + "' y='" + (y + 4) + "' text-anchor='start' font-size='12' fill='#6c757d'>#" + rank + "</text>");
        }
        for (int week = 1; week <= weekCount; week++) {
            double x = weekX(week, weekCount, left, plotWidth);
            out.println("<line x1='" + x + "' y1='" + top + "' x2='" + x + "' y2='" + (top + plotHeight) + "' stroke='#f1f3f5' stroke-width='1'/>");
            out.println("<text x='" + x + "' y='" + (top + plotHeight + 25) + "' text-anchor='middle' font-size='12' fill='#6c757d'>W" + week + "</text>");
        }
        out.println("<line x1='" + left + "' y1='" + top + "' x2='" + left + "' y2='" + (top + plotHeight) + "' stroke='#adb5bd'/>");
        out.println("<line x1='" + left + "' y1='" + (top + plotHeight) + "' x2='" + (left + plotWidth) + "' y2='" + (top + plotHeight) + "' stroke='#adb5bd'/>");

        List<CompetitionInsights.RankTrend> trends = insights.rankTrends().stream()
                .sorted(Comparator.comparingInt(this::currentRank)
                        .thenComparing(trend -> trend.athleteName().toLowerCase()))
                .toList();
        for (int i = 0; i < trends.size(); i++) {
            CompetitionInsights.RankTrend trend = trends.get(i);
            String color = chartColor(i);
            String points = trend.positions().stream()
                    .map(position -> weekX(position.weekNumber(), weekCount, left, plotWidth) + "," + rankY(position.rank(), athleteCount, top, plotHeight))
                    .reduce((a, b) -> a + " " + b)
                    .orElse("");
            out.println("<polyline points='" + points + "' fill='none' stroke='" + color + "' stroke-width='3' stroke-linecap='round' stroke-linejoin='round'/>");
            for (CompetitionInsights.RankPosition position : trend.positions()) {
                out.println("<circle cx='" + weekX(position.weekNumber(), weekCount, left, plotWidth) + "' cy='" + rankY(position.rank(), athleteCount, top, plotHeight)
                        + "' r='4' fill='" + color + "'><title>" + escape(trend.athleteName()) + " Week " + position.weekNumber() + ": #" + position.rank() + "</title></circle>");
            }
        }
        for (int i = 0; i < trends.size(); i++) {
            CompetitionInsights.RankTrend trend = trends.get(i);
            String color = chartColor(i);
            int yOffset = tiedLabelOffset(trends, i);
            double nameY = rankY(currentRank(trend), athleteCount, top, plotHeight) + 4 + yOffset;
            out.println("<a href='#' data-athlete-profile-id='" + escapeAttribute(trend.athleteId()) + "' data-athlete-profile-name='" + escapeAttribute(trend.athleteName()) + "'>");
            out.println("<text x='" + (left - 16) + "' y='" + nameY + "' text-anchor='end' font-size='12' fill='" + color + "' style='text-decoration:underline;'>" + escape(trend.athleteName()) + "</text>");
            out.println("</a>");
        }
        out.println("</svg></div></section>");
    }

    private void renderWeeklyHistory(PrintWriter out, CompetitionInsights insights) {
        out.println("<section class='mb-4'>");
        out.println("<h3 class='h5'>Week-by-week leaderboard winners</h3>");
        List<CompetitionInsights.WeeklyLeaderboard> leaderboards = insights.weeklyLeaderboards();
        List<CompetitionInsights.WeeklyLeaderboard> visibleLeaderboards = leaderboards.stream()
                .filter(leaderboard -> !leaderboard.entries().isEmpty())
                .toList();
        if (visibleLeaderboards.isEmpty()) {
            out.println("<div class='alert alert-info'>No weekly history is available yet.</div>");
            out.println("</section>");
            return;
        }

        out.println("<div class='card shadow-sm border-0'>");
        out.println("<div class='card-body'>");
        out.println("<div class='d-flex flex-wrap gap-2 align-items-center justify-content-between mb-3'>");
        out.println("<div class='btn-group' role='group' aria-label='Week navigation'>");
        out.println("<button type='button' class='btn btn-outline-secondary' id='weekHistoryPrevBtn' aria-label='Previous week'><i class='bi bi-chevron-left'></i></button>");
        out.println("<button type='button' class='btn btn-outline-secondary' id='weekHistoryNextBtn' aria-label='Next week'><i class='bi bi-chevron-right'></i></button>");
        out.println("</div>");
        out.println("<div class='flex-grow-1' style='min-width: 14rem; max-width: 24rem;'>");
        out.println("<label class='form-label mb-1' for='weekHistoryWeekSelect'>Choose a week</label>");
        out.println("<select class='form-select' id='weekHistoryWeekSelect'>");
        for (int i = 0; i < visibleLeaderboards.size(); i++) {
            CompetitionInsights.WeeklyLeaderboard leaderboard = visibleLeaderboards.get(i);
            out.println("<option value='" + i + "'>Week " + leaderboard.weekNumber() + "</option>");
        }
        out.println("</select>");
        out.println("</div>");
        out.println("</div>");

        out.println("<div class='border rounded p-3 bg-light' aria-live='polite'>");
        out.println("<div class='d-flex justify-content-between align-items-start gap-3 flex-wrap mb-2'>");
        out.println("<div>");
        out.println("<div class='text-muted small'>Selected week</div>");
        out.println("<div class='h4 mb-0' id='weekHistoryLabel'></div>");
        out.println("</div>");
        out.println("<div class='text-end'>");
        out.println("<div class='text-muted small'>Leader</div>");
        out.println("<div class='fw-semibold' id='weekHistoryLeader'></div>");
        out.println("</div>");
        out.println("</div>");
        out.println("<div class='row g-3'>");
        out.println("<div class='col-md-4'><div class='card h-100'><div class='card-body'><div class='text-muted small'>Points</div><div class='h5 mb-0' id='weekHistoryPoints'></div></div></div></div>");
        out.println("<div class='col-md-8'><div class='card h-100'><div class='card-body'><div class='text-muted small'>Top three</div><div class='fw-semibold' id='weekHistoryTopThree'></div></div></div></div>");
        out.println("</div>");
        out.println("</div>");
        out.println("</div>");
        out.println("</div></section>");

        out.println("<script>");
        out.println("(function () {");
        out.println("const weeks = [");
        for (int i = 0; i < visibleLeaderboards.size(); i++) {
            CompetitionInsights.WeeklyLeaderboard leaderboard = visibleLeaderboards.get(i);
            CompetitionInsights.RankedAthleteMetric leader = leaderboard.entries().get(0);
            out.println("{weekNumber:" + leaderboard.weekNumber()
                    + ", leader:" + jsString(leader.athleteName())
                    + ", points:" + jsString(format(leader.sortValue()))
                    + ", topThree:" + jsString(topThree(leaderboard))
                    + "}" + (i < visibleLeaderboards.size() - 1 ? "," : ""));
        }
        out.println("];");
        out.println("const select = document.getElementById('weekHistoryWeekSelect');");
        out.println("const prev = document.getElementById('weekHistoryPrevBtn');");
        out.println("const next = document.getElementById('weekHistoryNextBtn');");
        out.println("const label = document.getElementById('weekHistoryLabel');");
        out.println("const leader = document.getElementById('weekHistoryLeader');");
        out.println("const points = document.getElementById('weekHistoryPoints');");
        out.println("const topThree = document.getElementById('weekHistoryTopThree');");
        out.println("if (!select || !prev || !next || !label || !leader || !points || !topThree || !weeks.length) { return; }");
        out.println("let index = 0;");
        out.println("function renderWeek(nextIndex) {");
        out.println("index = Math.max(0, Math.min(weeks.length - 1, nextIndex));");
        out.println("const week = weeks[index];");
        out.println("select.value = String(index);");
        out.println("label.textContent = 'Week ' + week.weekNumber;");
        out.println("leader.textContent = week.leader;");
        out.println("points.textContent = week.points;");
        out.println("topThree.textContent = week.topThree;");
        out.println("prev.disabled = index === 0;");
        out.println("next.disabled = index === weeks.length - 1;");
        out.println("}");
        out.println("select.addEventListener('change', function () { renderWeek(parseInt(select.value, 10)); });");
        out.println("prev.addEventListener('click', function () { renderWeek(index - 1); });");
        out.println("next.addEventListener('click', function () { renderWeek(index + 1); });");
        out.println("renderWeek(0);");
        out.println("})();");
        out.println("</script>");
    }

    private void renderSportStandings(PrintWriter out, CompetitionInsights insights) {
        out.println("<section class='mb-4'>");
        out.println("<h3 class='h5'>Sport-specific standings</h3>");
        out.println("<p class='text-muted small'>Rankings only; athlete distances are not shown.</p>");
        out.println("<div class='row g-3'>");
        for (CompetitionInsights.SportStanding standing : insights.sportStandings()) {
            out.println("<div class='col-md-6 col-lg-4'><div class='card h-100'><div class='card-body'>");
            out.println("<h4 class='h6'>" + escape(standing.sportType()) + "</h4><ol class='mb-0'>");
            for (CompetitionInsights.RankedAthleteMetric entry : standing.entries().stream().limit(5).toList()) {
                out.println("<li>" + escape(entry.athleteName()) + "</li>");
            }
            out.println("</ol></div></div></div>");
        }
        out.println("</div></section>");
    }

    private void renderProfileContent(PrintWriter out, CompetitionInsights.AthleteProfileSummary profile, AthleteProfileBlurb blurb, String loggedInAthleteId, String profileSummaryAction, String profileSummaryGenerateUrl) {
        out.println("<div class='d-flex align-items-center gap-3 mb-3'>");
        if (profile.profileMedium() != null && !profile.profileMedium().isBlank()) {
            out.println("<img src='" + escapeAttribute(profile.profileMedium()) + "' alt='' class='rounded-circle border' style='width:56px;height:56px;object-fit:cover;' onerror=\"this.style.display='none';this.nextElementSibling.classList.remove('d-none');\">");
            out.println("<span class='rounded-circle border d-none d-inline-flex align-items-center justify-content-center' style='width:56px;height:56px;'><i class='bi bi-person'></i></span>");
        } else {
            out.println("<span class='rounded-circle border d-inline-flex align-items-center justify-content-center' style='width:56px;height:56px;'><i class='bi bi-person'></i></span>");
        }
        out.println("<div><div class='fw-semibold'>" + escape(profile.athleteName()) + "</div><div class='text-muted small'>Strava athlete</div></div>");
        out.println("</div>");
        renderProfileBlurb(out, profile, blurb, loggedInAthleteId, profileSummaryAction, profileSummaryGenerateUrl);
        out.println("<div class='row g-3'>");
        metric(out, "Overall rank", "#" + profile.overallRank());
        metric(out, "Points to current week", format(profile.totalScore()));
        metric(out, "Current week progress", format(profile.currentWeekPercentOfGoal() * 100) + "%");
        metric(out, "Active weeks", String.valueOf(profile.activeWeeks()));
        metric(out, "Goal-crusher weeks", String.valueOf(profile.goalCrushWeeks()));
        metric(out, "Sick weeks", String.valueOf(profile.sickWeeks()));
        metric(out, "Strongest sport", profile.strongestSport());
        metric(out, "Best week", profile.bestWeekNumber() == 0 ? "-" : "Week " + profile.bestWeekNumber());
        metric(out, "Best week points", format(profile.bestWeekScore()));
        out.println("</div>");
        if (!profile.milestones().isEmpty()) {
            out.println("<div class='mt-3'><strong>Milestones</strong><ul class='mb-0'>");
            for (String milestone : profile.milestones()) {
                out.println("<li>" + escape(milestone) + "</li>");
            }
            out.println("</ul></div>");
        }
        renderPerformanceSummary(out, profile, profileSummaryGenerateUrl);
    }

    private void renderProfileBlurb(PrintWriter out, CompetitionInsights.AthleteProfileSummary profile, AthleteProfileBlurb blurb, String loggedInAthleteId, String profileSummaryAction, String profileSummaryGenerateUrl) {
        boolean ownProfile = profile.athleteId().equals(loggedInAthleteId);
        boolean verified = blurb != null && blurb.verified();
        String badgeClass = verified ? "text-bg-success" : "text-bg-warning";
        String badgeText = verified ? "Verified" : "Generated / Unverified";
        String generateUrl = profileSummaryGenerateUrl + "?athleteId=" + escapeAttribute(profile.athleteId());

        out.println("<div class='card border-info mb-3 ai-profile-blurb'" + (verified ? "" : " data-ai-blurb-athlete-id='" + escapeAttribute(profile.athleteId()) + "' data-ai-blurb-url='" + escapeAttribute(generateUrl) + "'") + "><div class='card-body'>");
        out.println("<div class='d-flex justify-content-between align-items-start gap-2 mb-2'>");
        out.println("<h4 class='h6 mb-0'>Profile</h4>");
        out.println("<span class='badge " + badgeClass + "'>" + badgeText + "</span>");
        out.println("</div>");
        if (ownProfile) {
            out.println("<form method='post' action='" + escapeAttribute(profileSummaryAction) + "' data-profile-form>");
            out.println("<input type='hidden' name='athleteId' value='" + escapeAttribute(profile.athleteId()) + "'>");
            out.println("<textarea name='summaryText' class='form-control' rows='4' maxlength='600'" + (verified ? "" : " disabled") + ">"
                    + escape(verified ? blurb.text() : "Generating profile summary...") + "</textarea>");
            out.println("<div class='form-text'>Only accepted profiles are saved globally. Clear this box and save to go back to generated profiles.</div>");
            out.println("<button type='submit' class='btn btn-sm btn-primary mt-2' disabled>" + (verified ? "Save changes" : "Accept and save") + "</button>");
            out.println("</form>");
        } else {
            out.println("<p class='mb-0' data-ai-blurb-text>" + escape(verified ? blurb.text() : "Generating profile summary...") + "</p>");
        }
        out.println("</div></div>");
    }

    private void renderPerformanceSummary(PrintWriter out, CompetitionInsights.AthleteProfileSummary profile, String profileSummaryGenerateUrl) {
        String generateUrl = profileSummaryGenerateUrl + "?athleteId=" + escapeAttribute(profile.athleteId()) + "&summaryType=performance";
        out.println("<div class='card border-secondary mt-3 ai-profile-blurb' data-ai-blurb-athlete-id='"
                + escapeAttribute(profile.athleteId()) + "' data-ai-blurb-url='" + escapeAttribute(generateUrl) + "'><div class='card-body'>");
        out.println("<div class='d-flex justify-content-between align-items-start gap-2 mb-2'>");
        out.println("<h4 class='h6 mb-0'>Performance summary</h4>");
        out.println("</div>");
        out.println("<p class='mb-0' data-ai-blurb-text>Generating performance summary...</p>");
        out.println("</div></div>");
    }

    private void metric(PrintWriter out, String label, String value) {
        out.println("<div class='col-6 col-md-3'><div class='border rounded p-3 h-100'><div class='text-muted small'>"
                + escape(label) + "</div><div class='fw-bold'>" + escape(value) + "</div></div></div>");
    }

    private String topThree(CompetitionInsights.WeeklyLeaderboard leaderboard) {
        return leaderboard.entries().stream()
                .limit(3)
                .map(entry -> "#" + entry.rank() + " " + escape(entry.athleteName()))
                .reduce((left, right) -> left + ", " + right)
                .orElse("-");
    }

    private double weekX(int weekNumber, int weekCount, int left, int plotWidth) {
        if (weekCount <= 1) {
            return left + (plotWidth / 2.0);
        }
        return left + ((weekCount - weekNumber) * (plotWidth / (double) (weekCount - 1)));
    }

    private double rankY(int rank, int athleteCount, int top, int plotHeight) {
        if (athleteCount <= 1) {
            return top + (plotHeight / 2.0);
        }
        return top + ((rank - 1) * (plotHeight / (double) (athleteCount - 1)));
    }

    private String chartColor(int index) {
        String[] colors = {"#0d6efd", "#dc3545", "#198754", "#fd7e14", "#6f42c1", "#20c997", "#d63384", "#0dcaf0", "#ffc107", "#6610f2"};
        return colors[index % colors.length];
    }

    private int currentRank(CompetitionInsights.RankTrend trend) {
        if (trend.positions().isEmpty()) {
            return Integer.MAX_VALUE;
        }
        return trend.positions().get(trend.positions().size() - 1).rank();
    }

    private int tiedLabelOffset(List<CompetitionInsights.RankTrend> trends, int index) {
        int rank = currentRank(trends.get(index));
        int sameRankBefore = 0;
        int sameRankTotal = 0;
        for (int i = 0; i < trends.size(); i++) {
            if (currentRank(trends.get(i)) == rank) {
                sameRankTotal++;
                if (i < index) {
                    sameRankBefore++;
                }
            }
        }
        return sameRankTotal <= 1 ? 0 : (sameRankBefore * 14) - ((sameRankTotal - 1) * 7);
    }

    @SuppressWarnings("unchecked")
    private boolean selectedCompetitionIsActive(HttpServletRequest request, long selectedCompetitionId) {
        Object activeCompetitions = request.getAttribute("activeCompetitions");
        if (!(activeCompetitions instanceof List<?> competitions)) {
            return false;
        }
        return competitions.stream()
                .filter(CompetitionSummaryView.class::isInstance)
                .map(CompetitionSummaryView.class::cast)
                .anyMatch(competition -> competition.getId() == selectedCompetitionId);
    }

    private String format(double value) {
        return new DecimalFormat("#.##").format(value);
    }

    private String escape(String value) {
        return WildflyUtils.escape(value == null ? "" : value);
    }

    private String escapeAttribute(String value) {
        return escape(value).replace("'", "&#39;");
    }

    private String jsString(String value) {
        String safe = value == null ? "" : value;
        return "'" + safe
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\r", "\\r")
                .replace("\n", "\\n") + "'";
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
