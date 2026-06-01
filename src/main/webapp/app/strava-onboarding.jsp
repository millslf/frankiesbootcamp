<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.frankies.bootcamp.model.AuthenticatedUser" %>
<%
    AuthenticatedUser onboardingUser = (AuthenticatedUser) request.getAttribute("stravaOnboardingUser");
    String displayName = (onboardingUser == null || onboardingUser.getDisplayName() == null || onboardingUser.getDisplayName().isBlank())
            ? "there"
            : onboardingUser.getDisplayName();
    String pageContextPath = request.getContextPath();
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <%@ include file="/WEB-INF/jspf/head-common.jspf" %>
    <title>Link Strava</title>
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
                        <div class="display-5 text-primary mb-3"><i class="bi bi-link-45deg"></i></div>
                        <h1 class="h3 mb-3">Link your Strava account</h1>
                        <p class="text-muted mb-0">
                            You are signed in as <strong><%= displayName %></strong>, but your Bootcamp account is not linked to Strava yet.
                        </p>
                    </div>

                    <div class="alert alert-primary" role="alert">
                        Link Strava to unlock weekly history, leaderboard, honour roll, and performance summaries.
                    </div>

                    <div class="d-grid gap-2 d-sm-flex justify-content-sm-center mt-4">
                                        <button class="btn btn-primary btn-lg" onclick="linkStravaPopup()">
                            <i class="bi bi-link-45deg me-2"></i>Link Strava
                        </button>
                        <a class="btn btn-outline-secondary btn-lg" href="<%=pageContextPath%>/">Back to home</a>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>

<script>
    function linkStravaPopup() {
        const callback = '<%= request.getAttribute("stravaCallback") %>';
        const clientId = '<%= request.getAttribute("stravaClientId") %>';
        const authUrl = 'https://www.strava.com/oauth/authorize'
            + '?client_id=' + encodeURIComponent(clientId)
            + '&redirect_uri=' + encodeURIComponent(callback)
            + '&response_type=code'
            + '&scope=activity:read'
            + '&state=popup';

        const w = window.open(authUrl, 'stravaAuth', 'width=520,height=720');
        function onMsg(e) {
            try {
                if (e.origin === window.location.origin && e.data === 'strava-linked') {
                    window.removeEventListener('message', onMsg);
                    location.reload();
                }
            } catch (err) { /* ignore */ }
        }
        window.addEventListener('message', onMsg);
    }
</script>
</body>
</html>
