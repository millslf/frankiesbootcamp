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
    List<CompetitionSummaryView> activeCompetitions = onboardingStatus == null ? List.of() : onboardingStatus.getActiveCompetitions();
    List<CompetitionSummaryView> pastCompetitions = onboardingStatus == null ? List.of() : onboardingStatus.getPastCompetitions();
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
    <title>Select competition</title>
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
                        <div class="display-5 text-primary mb-3"><i class="bi bi-list-check"></i></div>
                        <h1 class="h3 mb-3">Choose your competition, <%= displayName %></h1>
                        <p class="text-muted mb-0">
                            Pick which competition context you want to view now.
                        </p>
                    </div>

                    <h2 class="h5 mt-4">Current competitions</h2>
                    <ul class="list-group mb-4">
                        <% for (CompetitionSummaryView competition : activeCompetitions) {
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

                    <% if (!pastCompetitions.isEmpty()) { %>
                    <h2 class="h5 mt-4">Past competitions</h2>
                    <ul class="list-group mb-4">
                        <% for (CompetitionSummaryView competition : pastCompetitions) {
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
                        <a class="btn btn-outline-secondary btn-lg" href="<%=pageContextPath%>/logout">Sign out</a>
                        <a class="btn btn-primary btn-lg" href="<%=pageContextPath%>/app/competition-setup">Manage competitions</a>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
</body>
</html>
