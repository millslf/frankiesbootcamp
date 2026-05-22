package com.frankies.bootcamp.servlet;

import com.frankies.bootcamp.service.AiMessageService;
import com.frankies.bootcamp.service.ActivityProcessService;
import com.frankies.bootcamp.service.DBService;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;

@WebServlet(name = "zenBot", value = "/app/ZenBot")
public class ZenBotServlet extends BootcampServlet {
    @Inject
    private AiMessageService aiMessageService;
    @Inject
    private ActivityProcessService activityProcessService;
    @Inject
    private DBService dbService;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json; charset=UTF-8");
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        String question = request.getParameter("question");
        HttpSession session = request.getSession(false);
        int conversationTurn = 1;
        String athleteId = null;
        String athleteName = (String) request.getAttribute("athleteName");
        String athleteEmail = (String) request.getAttribute("athleteEmail");
        Object athlete = request.getAttribute("athlete");
        if (athlete instanceof com.frankies.bootcamp.model.BootcampAthlete bootcampAthlete) {
            athleteId = bootcampAthlete.getId();
        }
        String statsContext = activityProcessService != null
            ? activityProcessService.getZenBotStatsContext(athleteEmail)
            : "No athlete stats are currently available.";

        if (session != null) {
            Integer existingTurn = (Integer) session.getAttribute("zenBotConversationTurn");
            conversationTurn = existingTurn == null ? 1 : existingTurn + 1;
            session.setAttribute("zenBotConversationTurn", conversationTurn);
        }

        boolean usedLookup = aiMessageService != null && aiMessageService.shouldUseWebLookup(question);
        String reply = aiMessageService != null
            ? (usedLookup
                ? aiMessageService.getZenBotReplyWithLookup(question, athleteName, conversationTurn, statsContext)
                : aiMessageService.getZenBotReply(question, athleteName, conversationTurn, statsContext))
            : "The tiny coach is resting. A short walk is still available to you.";

        if (dbService != null) {
            try {
                dbService.saveZenBotMessage(athleteId, athleteEmail, athleteName, conversationTurn, question, reply);
            } catch (SQLException e) {
                throw new IOException("Unable to persist ZenBot message", e);
            }
        }

        try (PrintWriter out = response.getWriter()) {
            out.write("{\"reply\":\"" + escapeJson(reply) + "\",\"usedLookup\":" + usedLookup + "}");
        }
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\r", "")
            .replace("\n", "\\n");
    }
}
