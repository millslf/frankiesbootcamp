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
import java.util.LinkedHashMap;
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
        response.setContentType("text/html");
        BootcampAthlete loggedInAthlete = (BootcampAthlete) request.getAttribute("athlete");
        Long selectedCompetitionId = (Long) request.getAttribute("selectedCompetitionId");
        if (loggedInAthlete == null || selectedCompetitionId == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        List<PerformanceResponse> performances = activityProcessFacade.getPerformanceListForCompetition(selectedCompetitionId);
        boolean excludeLatestWeek = selectedCompetitionIsActive(request, selectedCompetitionId);
        CompetitionInsights insights = competitionInsightsService.buildInsights(performances, loggedInAthlete.getId(), excludeLatestWeek);
        Map<String, AthleteProfileBlurb> profileBlurbs = profileBlurbs(insights);

        PrintWriter out = response.getWriter();
        out.println("<div class='container'>");
        out.println("<h2 class='history-heading'><i class='bi bi-lightbulb-fill'></i> Competition Insights</h2>");
        out.println("<p class='history-subheading'>Trends, standings, and athlete highlights. Distances stay private. Active competitions use completed weeks only.</p>");
        String profileSummaryAction = request.getContextPath() + "/app/AthleteProfileSummary";
        String profileSummaryGenerateUrl = request.getContextPath() + "/app/AthleteProfileSummary";
        renderProfile(out, insights, profileBlurbs, loggedInAthlete.getId(), profileSummaryAction, profileSummaryGenerateUrl);
        renderRankTrend(out, insights);
        renderWeeklyHistory(out, insights);
        renderSportStandings(out, insights);
        renderProfileModal(out, insights, profileBlurbs, loggedInAthlete.getId(), profileSummaryAction, profileSummaryGenerateUrl);
        out.println("</div>");
    }

    private void renderProfile(PrintWriter out, CompetitionInsights insights, Map<String, AthleteProfileBlurb> profileBlurbs, String loggedInAthleteId, String profileSummaryAction, String profileSummaryGenerateUrl) {
        CompetitionInsights.AthleteProfileSummary profile = insights.selectedAthleteProfile();
        if (profile == null) {
            out.println("<div class='alert alert-info'>No insights are available yet.</div>");
            return;
        }

        out.println("<section class='card mb-4'><div class='card-body'>");
        out.println("<h3 class='h5'>" + escape(profile.athleteName()) + " profile</h3>");
        renderProfileContent(out, profile, profileBlurbs.get(profile.athleteId()), loggedInAthleteId, profileSummaryAction, profileSummaryGenerateUrl);
        out.println("<div class='mt-3'><label for='athleteProfileSelect' class='form-label fw-bold'>View another athlete</label>");
        out.println("<select id='athleteProfileSelect' class='form-select' style='max-width: 24rem;'>");
        out.println("<option value=''>Choose an athlete...</option>");
        for (CompetitionInsights.AthleteProfileSummary athleteProfile : insights.athleteProfiles()) {
            out.println("<option value='" + escapeAttribute(athleteProfile.athleteId()) + "' data-athlete-profile-name='" + escapeAttribute(athleteProfile.athleteName()) + "'>"
                    + escape(athleteProfile.athleteName()) + "</option>");
        }
        out.println("</select></div>");
        out.println("</div></section>");
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
        out.println("<div class='table-responsive'><table class='table table-sm table-striped align-middle'>");
        out.println("<thead><tr><th>Week</th><th>Leader</th><th>Points</th><th>Top three</th></tr></thead><tbody>");
        for (CompetitionInsights.WeeklyLeaderboard leaderboard : insights.weeklyLeaderboards()) {
            if (leaderboard.entries().isEmpty()) {
                continue;
            }
            CompetitionInsights.RankedAthleteMetric leader = leaderboard.entries().get(0);
            out.println("<tr><td>Week " + leaderboard.weekNumber() + "</td><td>" + escape(leader.athleteName())
                    + "</td><td>" + format(leader.sortValue()) + "</td><td>" + topThree(leaderboard) + "</td></tr>");
        }
        out.println("</tbody></table></div></section>");
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

    private void renderProfileModal(PrintWriter out, CompetitionInsights insights, Map<String, AthleteProfileBlurb> profileBlurbs, String loggedInAthleteId, String profileSummaryAction, String profileSummaryGenerateUrl) {
        out.println("<div class='modal fade' id='athleteProfileModal' tabindex='-1' aria-labelledby='athleteProfileModalTitle' aria-hidden='true'>");
        out.println("<div class='modal-dialog modal-lg modal-dialog-scrollable'><div class='modal-content'>");
        out.println("<div class='modal-header'><h5 class='modal-title' id='athleteProfileModalTitle'>Athlete profile</h5>");
        out.println("<button type='button' class='btn-close' data-bs-dismiss='modal' aria-label='Close'></button></div>");
        out.println("<div class='modal-body'>");
        for (CompetitionInsights.AthleteProfileSummary profile : insights.athleteProfiles()) {
            out.println("<div class='athlete-profile-modal-body d-none' data-athlete-profile-body='" + escapeAttribute(profile.athleteId()) + "'>");
            renderProfileContent(out, profile, profileBlurbs.get(profile.athleteId()), loggedInAthleteId, profileSummaryAction, profileSummaryGenerateUrl);
            out.println("</div>");
        }
        out.println("</div></div></div></div>");
        out.println("<script>");
        out.println("(function(){");
        out.println("var modalElement=document.getElementById('athleteProfileModal');");
        out.println("if(!modalElement || modalElement.dataset.bound==='true') return;");
        out.println("if(modalElement.parentElement!==document.body){document.body.appendChild(modalElement);}");
        out.println("modalElement.dataset.bound='true';");
        out.println("document.addEventListener('click',function(event){");
        out.println("var trigger=event.target.closest('[data-athlete-profile-id]');");
        out.println("if(!trigger) return;");
        out.println("event.preventDefault();");
        out.println("var athleteId=trigger.getAttribute('data-athlete-profile-id');");
        out.println("var athleteName=trigger.getAttribute('data-athlete-profile-name') || 'Athlete profile';");
        out.println("showAthleteProfile(athleteId, athleteName);");
        out.println("});");
        out.println("document.addEventListener('change',function(event){");
        out.println("if(event.target.id!=='athleteProfileSelect' || !event.target.value) return;");
        out.println("var option=event.target.options[event.target.selectedIndex];");
        out.println("showAthleteProfile(event.target.value, option.getAttribute('data-athlete-profile-name') || option.textContent);");
        out.println("event.target.value='';");
        out.println("});");
        out.println("function showAthleteProfile(athleteId, athleteName){");
        out.println("modalElement.querySelectorAll('.athlete-profile-modal-body').forEach(function(body){body.classList.add('d-none');});");
        out.println("var body=null;");
        out.println("modalElement.querySelectorAll('.athlete-profile-modal-body').forEach(function(candidate){if(candidate.getAttribute('data-athlete-profile-body')===athleteId){body=candidate;}});");
        out.println("if(!body) return;");
        out.println("body.classList.remove('d-none');");
        out.println("bindProfileForms(body);");
        out.println("loadGeneratedBlurbs(body);");
        out.println("var title=document.getElementById('athleteProfileModalTitle');");
        out.println("if(title) title.textContent=athleteName + ' profile';");
        out.println("bootstrap.Modal.getOrCreateInstance(modalElement).show();");
        out.println("}");
        out.println("function loadGeneratedBlurbs(root){");
        out.println("(root || document).querySelectorAll('.ai-profile-blurb[data-ai-blurb-athlete-id]').forEach(function(card){");
        out.println("if(card.dataset.aiLoaded==='true' || card.closest('.athlete-profile-modal-body.d-none')) return;");
        out.println("card.dataset.aiLoaded='true';");
        out.println("var url=card.getAttribute('data-ai-blurb-url');");
        out.println("var textTarget=card.querySelector('[data-ai-blurb-text]');");
        out.println("var textarea=card.querySelector('textarea[name=\"summaryText\"]');");
        out.println("var submit=card.querySelector('button[type=\"submit\"]');");
        out.println("var request=new XMLHttpRequest();");
        out.println("request.open('GET', url, true);");
        out.println("request.onreadystatechange=function(){");
        out.println("if(request.readyState!==4) return;");
        out.println("var text='Could not generate a profile summary right now. Very mysterious, probably cardio-related.';");
        out.println("if(request.status>=200 && request.status<300){try{text=JSON.parse(request.responseText).text || text;}catch(e){}}");
        out.println("if(textTarget) textTarget.textContent=text;");
        out.println("if(textarea){textarea.value=text;textarea.disabled=false;}");
        out.println("if(textarea){var form=textarea.closest('form[data-profile-form]');if(form){form.dataset.initial='';updateProfileButton(form);}}");
        out.println("if(submit && !textarea) submit.disabled=false;");
        out.println("};");
        out.println("request.send();");
        out.println("});");
        out.println("}");
        out.println("function bindProfileForms(root){");
        out.println("(root || document).querySelectorAll('form[data-profile-form]').forEach(function(form){");
        out.println("if(form.dataset.bound==='true') return;");
        out.println("form.dataset.bound='true';");
        out.println("var textarea=form.querySelector('textarea[name=\"summaryText\"]');");
        out.println("if(textarea && form.dataset.initial===undefined){form.dataset.initial=textarea.value;}");
        out.println("if(textarea){textarea.addEventListener('input',function(){updateProfileButton(form);});}");
        out.println("updateProfileButton(form);");
        out.println("});");
        out.println("}");
        out.println("function updateProfileButton(form){");
        out.println("var textarea=form.querySelector('textarea[name=\"summaryText\"]');");
        out.println("var submit=form.querySelector('button[type=\"submit\"]');");
        out.println("if(!textarea || !submit) return;");
        out.println("submit.disabled=textarea.disabled || textarea.value===(form.dataset.initial || '');");
        out.println("}");
        out.println("bindProfileForms(document);");
        out.println("loadGeneratedBlurbs(document);");
        out.println("})();");
        out.println("</script>");
    }

    private void renderProfileContent(PrintWriter out, CompetitionInsights.AthleteProfileSummary profile, AthleteProfileBlurb blurb, String loggedInAthleteId, String profileSummaryAction, String profileSummaryGenerateUrl) {
        renderProfileBlurb(out, profile, blurb, loggedInAthleteId, profileSummaryAction, profileSummaryGenerateUrl);
        out.println("<div class='row g-3'>");
        metric(out, "Overall rank", "#" + profile.overallRank());
        metric(out, "Total points", format(profile.totalScore()));
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

    private Map<String, AthleteProfileBlurb> profileBlurbs(CompetitionInsights insights) {
        Map<String, AthleteProfileBlurb> blurbs = new LinkedHashMap<>();
        for (CompetitionInsights.AthleteProfileSummary profile : insights.athleteProfiles()) {
            AthleteProfileBlurb verified = null;
            try {
                verified = dbService.getVerifiedAthleteProfileBlurb(profile.athleteId());
            } catch (SQLException e) {
                log.warn("Could not load verified athlete profile blurb for " + profile.athleteId(), e);
            }
            if (verified != null) {
                blurbs.put(profile.athleteId(), verified);
            }
        }
        return blurbs;
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
}
