<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.frankies.bootcamp.model.AuthenticatedUser" %>
<%@ page import="com.frankies.bootcamp.model.OnboardingStatus" %>
<%@ page import="com.frankies.bootcamp.model.CompetitionSummaryView" %>
<%@ page import="java.time.Instant" %>
<%@ page import="java.time.ZoneId" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%@ page import="java.util.List" %>
<%
    AuthenticatedUser onboardingUser = (AuthenticatedUser) session.getAttribute("authUser");
    OnboardingStatus onboardingStatus = (OnboardingStatus) request.getAttribute("onboardingStatus");
    Object selectionActiveCompetitionsAttr = request.getAttribute("activeCompetitions");
    Object selectionPastCompetitionsAttr = request.getAttribute("pastCompetitions");
    List<CompetitionSummaryView> selectionActiveCompetitions = onboardingStatus == null
            ? (selectionActiveCompetitionsAttr instanceof List<?> ? (List<CompetitionSummaryView>) selectionActiveCompetitionsAttr : List.of())
            : onboardingStatus.getActiveCompetitions();
    List<CompetitionSummaryView> selectionPastCompetitions = onboardingStatus == null
            ? (selectionPastCompetitionsAttr instanceof List<?> ? (List<CompetitionSummaryView>) selectionPastCompetitionsAttr : List.of())
            : onboardingStatus.getPastCompetitions();
    String displayName = (onboardingUser == null || onboardingUser.getDisplayName() == null || onboardingUser.getDisplayName().isBlank())
            ? "there"
            : onboardingUser.getDisplayName();
    String pageContextPath = request.getContextPath();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <%@ include file="/WEB-INF/jspf/head-common.jspf" %>
    <title>Manage competitions</title>
</head>
<body class="bg-light">
<%@ include file="/WEB-INF/jspf/header.jspf" %>

<div class="container py-4">
    <div class="row justify-content-center">
        <div class="col-lg-9">
            <div class="card shadow-sm border-0">
                <div class="card-body p-4 p-md-5">
                    <div class="text-center mb-4">
                        <div class="display-5 text-primary mb-3"><i class="bi bi-list-check"></i></div>
                        <h1 class="h3 mb-3">Manage your competitions, <%= displayName %></h1>
                        <p class="text-muted mb-0">
                            Pick which competition context you want to view now. Current competitions affect your active dashboard; past competitions show historical outcomes.
                        </p>
                    </div>

                    <div class="d-grid gap-2 d-sm-flex justify-content-sm-center mb-4">
                        <a class="btn btn-primary" href="<%=pageContextPath%>/app/competition-setup">Create or join competition</a>
                        <% if (request.getAttribute("selectedCompetitionAdmin") instanceof Boolean selectedAdmin && selectedAdmin
                                && request.getAttribute("selectedCompetitionId") instanceof Long selectedId) { %>
                        <a class="btn btn-outline-warning" href="<%=pageContextPath%>/app/competition-invitations?competitionId=<%= selectedId %>">Manage invites for selected competition</a>
                        <% } %>
                    </div>

                    <h2 class="h5 mt-4">Current competitions</h2>
                    <% if (selectionActiveCompetitions.isEmpty()) { %>
                    <div class="alert alert-info">You do not have any current active competitions.</div>
                    <% } else { %>
                    <ul class="list-group mb-4">
                        <% for (CompetitionSummaryView competition : selectionActiveCompetitions) {
                            String startLabel = Instant.ofEpochSecond(competition.getStartTimestamp()).atZone(ZoneId.of(competition.getTimezone())).format(formatter);
                            String endLabel = competition.getEndTimestamp() == null ? "No end date" : Instant.ofEpochSecond(competition.getEndTimestamp()).atZone(ZoneId.of(competition.getTimezone())).format(formatter);
                        %>
                        <li class="list-group-item d-flex justify-content-between align-items-start">
                            <div>
                                <div class="fw-semibold"><%= competition.getName() %></div>
                                <div class="text-muted small">Start: <%= startLabel %> | End: <%= endLabel %></div>
                            </div>
                            <a class="btn btn-sm btn-outline-primary" href="<%=pageContextPath%>/app/select-competition?competitionId=<%= competition.getId() %>">Open</a>
                        </li>
                        <% } %>
                    </ul>
                    <% } %>

                    <% if (!selectionPastCompetitions.isEmpty()) { %>
                    <h2 class="h5 mt-4">Past competitions</h2>
                    <ul class="list-group mb-4">
                        <% for (CompetitionSummaryView competition : selectionPastCompetitions) {
                            String startLabel = Instant.ofEpochSecond(competition.getStartTimestamp()).atZone(ZoneId.of(competition.getTimezone())).format(formatter);
                            String endLabel = competition.getEndTimestamp() == null ? "No end date" : Instant.ofEpochSecond(competition.getEndTimestamp()).atZone(ZoneId.of(competition.getTimezone())).format(formatter);
                        %>
                        <li class="list-group-item d-flex justify-content-between align-items-start">
                            <div>
                               <div class="fw-semibold"><%= competition.getName() %></div>
                               <div class="text-muted small">Start: <%= startLabel %> | End: <%= endLabel %></div>
                            </div>
                            <a class="btn btn-sm btn-outline-secondary" href="<%=pageContextPath%>/app/select-competition?competitionId=<%= competition.getId() %>">View outcome</a>
                        </li>
                        <% } %>
                    </ul>
                    <% } %>

                    <div class="d-grid gap-2 d-sm-flex justify-content-sm-center mt-4">
                        <a class="btn btn-outline-secondary btn-lg" href="<%=pageContextPath%>/app/">Back to dashboard</a>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
</body>
</html>
