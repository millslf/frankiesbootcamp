<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.frankies.bootcamp.model.AuthenticatedUser" %>
<%@ page import="com.frankies.bootcamp.model.OnboardingStatus" %>
<%@ page import="com.frankies.bootcamp.model.CompetitionSummaryView" %>
<%@ page import="java.time.Instant" %>
<%@ page import="java.time.ZoneId" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%
    AuthenticatedUser onboardingUser = (AuthenticatedUser) session.getAttribute("authUser");
    OnboardingStatus onboardingStatus = (OnboardingStatus) request.getAttribute("onboardingStatus");
    CompetitionSummaryView competition = onboardingStatus == null ? null : onboardingStatus.getActiveCompetition();
    String displayName = (onboardingUser == null || onboardingUser.getDisplayName() == null || onboardingUser.getDisplayName().isBlank())
            ? "there"
            : onboardingUser.getDisplayName();
    String pageContextPath = request.getContextPath();
    String competitionName = competition == null ? "your competition" : competition.getName();
    String startLabel = competition == null ? "soon" : Instant.ofEpochSecond(competition.getStartTimestamp())
            .atZone(ZoneId.of(competition.getTimezone()))
            .format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <%@ include file="/WEB-INF/jspf/head-common.jspf" %>
    <title>Competition starts soon</title>
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
                <div class="card-body p-4 p-md-5 text-center">
                    <div class="display-5 text-primary mb-3"><i class="bi bi-calendar-event"></i></div>
                    <h1 class="h3 mb-3">You are in, <%= displayName %></h1>
                    <p class="text-muted mb-4">
                        <strong><%= competitionName %></strong> has not started yet. This competition begins on <strong><%= startLabel %></strong>.
                    </p>

                    <div class="alert alert-info" role="alert">
                        We will show your normal competition dashboard once the competition start date arrives.
                    </div>

                    <div class="d-grid gap-2 d-sm-flex justify-content-sm-center mt-4">
                        <a class="btn btn-outline-secondary btn-lg" href="<%=pageContextPath%>/logout">Sign out</a>
                        <a class="btn btn-primary btn-lg" href="<%=pageContextPath%>/app/competition-setup">View competitions</a>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
</body>
</html>
