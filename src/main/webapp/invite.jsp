<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.frankies.bootcamp.model.CompetitionInvitationView" %>
<%@ page import="com.frankies.bootcamp.model.AuthenticatedUser" %>
<%
    CompetitionInvitationView invitation = (CompetitionInvitationView) request.getAttribute("invitation");
    String error = (String) request.getAttribute("inviteError");
    String pageContextPath = request.getContextPath();
    AuthenticatedUser authUser = (AuthenticatedUser) session.getAttribute("authUser");
    boolean loggedIn = authUser != null && authUser.getUserId() != null && !authUser.getUserId().isBlank();
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
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <%@ include file="/WEB-INF/jspf/head-common.jspf" %>
    <title>Competition invitation</title>
</head>
<body class="bg-light">
<header class="d-flex align-items-center justify-content-between text-white px-3 py-2 shadow-sm"
        style="min-height:56px; background-color: #0d6efd;">
    <a href="<%=pageContextPath%>/" class="text-white text-decoration-none d-flex align-items-center gap-2">
        <span aria-hidden="true">&#x1F3CB;&#xFE0F;</span>
        <span>Frankies Bootcamp!</span>
    </a>
</header>

<div class="container py-5">
    <div class="row justify-content-center">
        <div class="col-lg-8">
            <div class="card shadow-sm border-0">
                <div class="card-body p-4 p-md-5">
                    <div class="text-center mb-4">
                        <div class="display-5 text-primary mb-3"><i class="bi bi-envelope-paper"></i></div>
                        <h1 class="h3 mb-3">You have a competition invitation</h1>
                    </div>

                    <% if (error != null && !error.isBlank()) { %>
                    <div class="alert alert-warning" role="alert"><%= escapeHtml(error) %></div>
                    <% } else if (invitation != null) { %>
                    <div class="alert alert-primary" role="alert">
                        <strong><%= escapeHtml(invitation.getCompetitionName()) %></strong>
                        <% if (invitation.getInvitedEmail() != null) { %>
                        was sent to <strong><%= escapeHtml(invitation.getInvitedEmail()) %></strong>.
                        <% } %>
                    </div>
                    <% if (loggedIn) { %>
                    <p class="text-muted">
                        Accept this invitation to join the competition. If you do not have Strava linked yet, you will be sent there after accept.
                    </p>
                    <form method="post" action="<%=pageContextPath%>/app/invitations/respond" class="mt-4">
                        <input type="hidden" name="invitationId" value="<%= invitation.getId() %>">
                        <input type="hidden" name="token" value="<%= invitation.getToken() %>">
                        <div class="d-grid gap-2 d-sm-flex justify-content-sm-center">
                            <button class="btn btn-primary btn-lg" type="submit" name="action" value="accept">Accept invite</button>
                            <button class="btn btn-outline-secondary btn-lg" type="submit" name="action" value="decline">Decline</button>
                        </div>
                    </form>
                    <% } else { %>
                    <p class="text-muted">
                        Sign in or create your account, then link Strava if needed. We will keep this invitation with your session.
                    </p>
                    <% } %>
                    <% } else { %>
                    <div class="alert alert-info" role="alert">
                        This invitation link is missing or invalid.
                    </div>
                    <% } %>

                    <% if (!loggedIn) { %>
                    <div class="d-grid gap-2 d-sm-flex justify-content-sm-center mt-4">
                        <a class="btn btn-primary btn-lg" href="<%=pageContextPath%>/login?prompt=login">Sign in</a>
                        <a class="btn btn-outline-secondary btn-lg" href="<%=pageContextPath%>/">Back to home</a>
                    </div>
                    <% } %>
                </div>
            </div>
        </div>
    </div>
</div>
</body>
</html>
