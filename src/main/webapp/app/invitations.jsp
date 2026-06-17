<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.frankies.bootcamp.model.CompetitionInvitationView" %>
<%@ page import="com.frankies.bootcamp.model.BootcampAthlete" %>
<%@ page import="java.util.List" %>
<%
    List<CompetitionInvitationView> invitations = (List<CompetitionInvitationView>) request.getAttribute("pendingCompetitionInvitations");
    CompetitionInvitationView pendingInvitation = (CompetitionInvitationView) request.getAttribute("pendingInvitation");
    BootcampAthlete athlete = (BootcampAthlete) request.getAttribute("athlete");
    Double suggestedGoal = athlete == null ? null : athlete.getGoal();
    String pageContextPath = request.getContextPath();
    String error = request.getParameter("error");
    String status = request.getParameter("status");
%>
<%!
    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "";
        }
        if (email.length() <= 5) {
            return email;
        }
        int at = email.indexOf('@');
        if (at <= 0) {
            return email;
        }
        String localPart = email.substring(0, at);
        String domain = email.substring(at);
        if (localPart.length() <= 2) {
            return localPart.substring(0, 1) + "***" + domain;
        }
        return localPart.substring(0, 1) + "***" + localPart.substring(localPart.length() - 1) + domain;
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <%@ include file="/WEB-INF/jspf/head-common.jspf" %>
    <title>Invitations</title>
</head>
<body class="bg-light">
<%@ include file="/WEB-INF/jspf/header.jspf" %>

<div class="container py-4">
    <div class="row justify-content-center">
        <div class="col-lg-9">
            <% if (status != null && !status.isBlank()) { %>
            <div class="alert alert-success" role="alert">Invitation <%= escapeHtml(status) %>.</div>
            <% } %>
            <% if (error != null && !error.isBlank()) { %>
            <div class="alert alert-danger" role="alert"><%= escapeHtml(error) %></div>
            <% } %>

            <div class="card shadow-sm border-0 mb-4">
                <div class="card-body p-4">
                    <h1 class="h4 mb-3">Pending invitations</h1>
                    <% if (invitations == null || invitations.isEmpty()) { %>
                    <div class="alert alert-info mb-0">No pending invitations right now.</div>
                    <% } else { %>
                    <div class="row g-3">
                        <% for (CompetitionInvitationView invitation : invitations) { %>
                        <div class="col-12">
                            <div class="border rounded p-3 bg-white">
                                <div class="d-flex justify-content-between align-items-start gap-3 flex-wrap">
                                    <div>
                                        <div class="fw-semibold"><%= escapeHtml(invitation.getCompetitionName()) %></div>
                                        <div class="text-muted small"><%= escapeHtml(maskEmail(invitation.getInvitedEmail())) %></div>
                                    </div>
                                    <span class="badge text-bg-warning">Pending</span>
                                </div>
                                <form class="mt-3" method="post" action="<%=pageContextPath%>/app/invitations/respond">
                                    <input type="hidden" name="invitationId" value="<%= invitation.getId() %>">
                                    <input type="hidden" name="token" value="<%= invitation.getToken() %>">
                                    <div class="row g-2 align-items-end">
                                        <div class="col-sm-4">
                                            <label class="form-label" for="startingGoal-<%=invitation.getId()%>">Starting goal</label>
                                            <input class="form-control" id="startingGoal-<%=invitation.getId()%>" name="startingGoal" type="number" min="0" step="0.1"
                                                   value="<%= suggestedGoal == null ? "0" : suggestedGoal %>">
                                        </div>
                                        <div class="col-sm-8 text-sm-end">
                                            <button class="btn btn-primary" type="submit" name="action" value="accept">Accept</button>
                                            <button class="btn btn-outline-secondary" type="submit" name="action" value="decline">Decline</button>
                                        </div>
                                    </div>
                                </form>
                            </div>
                        </div>
                        <% } %>
                    </div>
                    <% } %>
                </div>
            </div>

            <% if (pendingInvitation != null && (invitations == null || invitations.stream().noneMatch(inv -> inv.getId() == pendingInvitation.getId()))) { %>
            <div class="alert alert-primary" role="alert">
                Invitation loaded for <strong><%= escapeHtml(pendingInvitation.getCompetitionName()) %></strong>.
            </div>
            <% } %>

            <div class="d-grid gap-2 d-sm-flex justify-content-sm-center">
                <a class="btn btn-outline-secondary btn-lg" href="<%=pageContextPath%>/app/">Back to dashboard</a>
            </div>
        </div>
    </div>
</div>
</body>
</html>
