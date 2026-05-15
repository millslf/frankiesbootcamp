package com.frankies.bootcamp.servlet;

import com.frankies.bootcamp.model.BootcampAthlete;
import com.frankies.bootcamp.service.DBService;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Set;

@WebServlet(name = "tabAudit", value = "/app/TabAudit")
public class TabAuditServlet extends BootcampServlet {
    private static final Set<String> ALLOWED_TABS = Set.of("landing", "history", "leaderboard", "honour-roll", "summary");

    @Inject
    private DBService dbService;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String tab = request.getParameter("tab");
        if (tab == null || !ALLOWED_TABS.contains(tab)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        Object athlete = request.getAttribute("athlete");
        if (!(athlete instanceof BootcampAthlete bootcampAthlete)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        try {
            String eventType = "landing".equals(tab) ? "page-landing" : "tab-click";
            dbService.saveAthleteAuditEvent(bootcampAthlete.getId(), eventType, tab);
        } catch (SQLException e) {
            throw new IOException("Unable to persist tab audit", e);
        }

        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }
}
