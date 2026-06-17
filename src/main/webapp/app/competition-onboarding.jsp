<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.frankies.bootcamp.model.AuthenticatedUser" %>
<%@ page import="com.frankies.bootcamp.model.OnboardingStatus" %>
<%
    AuthenticatedUser onboardingUser = (AuthenticatedUser) session.getAttribute("authUser");
    OnboardingStatus onboardingStatus = (OnboardingStatus) request.getAttribute("onboardingStatus");
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
    <title>Finish onboarding</title>
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
                        <div class="display-5 text-primary mb-3"><i class="bi bi-flag"></i></div>
                        <h1 class="h3 mb-3">One more step, <%= displayName %></h1>
                        <p class="text-muted mb-0">
                            Your account is authenticated and linked to Strava, but you are not in an active competition yet.
                        </p>
                    </div>

                    <div class="alert alert-primary" role="alert">
                        Current onboarding state: <strong><%= onboardingStatus == null ? "competition-pending" : onboardingStatus.getState() %></strong>
                    </div>

                    <p class="mb-3">
                        Your account is ready. Wait for a competition invitation from an admin, or open an invite link if you already have one.
                    </p>

                    <ul class="text-muted">
                        <li>Your app user is ready.</li>
                        <li>Your Strava link is complete.</li>
                        <li>Your next step is to accept a competition invitation.</li>
                    </ul>

                    <div class="d-grid gap-2 d-sm-flex justify-content-sm-center mt-4">
                        <a class="btn btn-outline-secondary btn-lg" href="<%=pageContextPath%>/logout">Sign out</a>
                        <a class="btn btn-primary btn-lg" href="<%=pageContextPath%>/app/invitations">View invitations</a>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
</body>
</html>
