package com.frankies.bootcamp.servlet;

import com.frankies.bootcamp.service.ActivityProcessFacade;
import com.frankies.bootcamp.model.CompetitionSummaryView;
import com.frankies.bootcamp.utils.WildflyUtils;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.wildfly.security.credential.store.CredentialStoreException;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.List;

@WebServlet(name = "athleteSummary", value = "/app/AthleteSummary")
public class AthleteSummaryServlet extends BootcampServlet {
    @Inject
    private ActivityProcessFacade activityProcessFacade;
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        String summaryContent = "";
        Long selectedCompetitionId = null;

        try {
            com.frankies.bootcamp.model.BootcampAthlete loggedInAthlete = (com.frankies.bootcamp.model.BootcampAthlete) request.getAttribute("athlete");
            selectedCompetitionId = (Long) request.getAttribute("selectedCompetitionId");
            summaryContent = selectedCompetitionId != null
                    ? activityProcessFacade.getLoggedInAthleteSummaryForCompetition(selectedCompetitionId, loggedInAthlete.getId())
                    : activityProcessFacade.getLoggedInAthleteSummary(loggedInAthlete.getId());
        } catch (IOException | CredentialStoreException | NoSuchAlgorithmException | SQLException e) {
            throw new IOException("Failed to load athlete summary", e);
        }

        out.println("<!DOCTYPE html>");
        out.println("<html lang=\"en\">");
        out.println("<head>");
        out.println("  <meta charset=\"UTF-8\">");
        out.println("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">");
        out.println("  <title>Athlete Summary</title>");
        out.println("</head>");
        out.println("<body>");
        out.println("  <div class=\"container\">");

        renderCompetitionSwitcher(request, out, selectedCompetitionId);

        out.println("    <p><i class=\"bi bi-trophy-fill trophy-icon\"></i><strong>Performance Summary:</strong></p>");

        // Output escaped or raw summaryContent depending on its nature
        out.println("    <div class=\"mt-2\">");
        out.println(WildflyUtils.escape(summaryContent));
        out.println("    </div>");

        // Optional: Add another trophy or reward footer
        out.println("    <hr/>");
        out.println("    <p class=\"text-muted\"><i class=\"bi bi-award-fill text-warning\"></i> Keep training hard and breaking limits!</p>");

        out.println("  </div>");
        out.println("</body>");
        out.println("</html>");
    }

    private void renderCompetitionSwitcher(HttpServletRequest request, PrintWriter out, Long selectedCompetitionId) {
        List<CompetitionSummaryView> activeCompetitions = competitions(request, "activeCompetitions");
        List<CompetitionSummaryView> pastCompetitions = competitions(request, "pastCompetitions");
        if ((activeCompetitions == null || activeCompetitions.isEmpty()) && (pastCompetitions == null || pastCompetitions.isEmpty())) {
            return;
        }

        out.println("    <div class=\"card shadow-sm border-0 mb-3\">");
        out.println("      <div class=\"card-body py-3\">");
        out.println("        <form class=\"row g-2 align-items-end\" method=\"get\" action=\"" + request.getContextPath() + "/app/select-competition\">");
        out.println("          <input type=\"hidden\" name=\"returnTo\" value=\"/app/\">");
        out.println("          <div class=\"col-md-9\">");
        out.println("            <label class=\"form-label mb-1\" for=\"competitionId\">Switch competition</label>");
        out.println("            <select class=\"form-select\" id=\"competitionId\" name=\"competitionId\">");
        if (activeCompetitions != null && !activeCompetitions.isEmpty()) {
            out.println("              <optgroup label=\"Current competitions\">");
            for (CompetitionSummaryView competition : activeCompetitions) {
                out.println(renderCompetitionOption(competition, selectedCompetitionId));
            }
            out.println("              </optgroup>");
        }
        if (pastCompetitions != null && !pastCompetitions.isEmpty()) {
            out.println("              <optgroup label=\"Past competitions\">");
            for (CompetitionSummaryView competition : pastCompetitions) {
                out.println(renderCompetitionOption(competition, selectedCompetitionId));
            }
            out.println("              </optgroup>");
        }
        out.println("            </select>");
        out.println("          </div>");
        out.println("          <div class=\"col-md-3 d-grid\">");
        out.println("            <button class=\"btn btn-outline-primary\" type=\"submit\">Open</button>");
        out.println("          </div>");
        out.println("        </form>");
        out.println("      </div>");
        out.println("    </div>");
    }

    private List<CompetitionSummaryView> competitions(HttpServletRequest request, String attributeName) {
        Object competitionsAttr = request.getAttribute(attributeName);
        if (competitionsAttr instanceof List<?> list) {
            @SuppressWarnings("unchecked")
            List<CompetitionSummaryView> cast = (List<CompetitionSummaryView>) list;
            return cast;
        }
        return List.of();
    }

    private String renderCompetitionOption(CompetitionSummaryView competition, Long selectedCompetitionId) {
        boolean selected = selectedCompetitionId != null && selectedCompetitionId == competition.getId();
        return "              <option value=\"" + competition.getId() + "\"" + (selected ? " selected" : "") + ">" + WildflyUtils.escape(competition.getName()) + "</option>";
    }
}
