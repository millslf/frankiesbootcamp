<%@ page import="com.frankies.bootcamp.model.AuthenticatedUser" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
    response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
    response.addHeader("Cache-Control", "post-check=0, pre-check=0");
    response.setHeader("Pragma", "no-cache");
    boolean landingLoggedIn = session.getAttribute("authUser") instanceof AuthenticatedUser;
    String landingPrimaryHref = request.getContextPath() + (landingLoggedIn ? "/app/" : "/login");
    String landingPrimaryLabel = landingLoggedIn ? "Go to dashboard" : "Login";
    String landingAlertSuffix = landingLoggedIn
            ? " to open your dashboard."
            : " to sign in or create your account through Auth0, then head to your dashboard.";
%>
<!doctype html>
<html>
<head>
    <meta charset="utf-8"/>
    <%@ include file="/WEB-INF/jspf/head-common.jspf" %>
    <title>Frankies Bootcamp</title>
    <meta name="viewport" content="width=device-width, initial-scale=1"/>
    <style>
        /* Neutralize app tab rules on the public page */
        .landing .tab-content { display: block !important; }
        .landing .tab-content > .tab-pane { display: block !important; opacity: 1 !important; }
        /* If your app CSS added any left padding/margins */
        .landing .main-content-wrapper { margin-left: 0 !important; padding-left: 0 !important; }
    </style>
</head>
<body class="landing">
<%@ include file="/WEB-INF/jspf/header.jspf" %>

<main class="container my-4">
    <!-- Tabs (only one “Info” tab for now) -->
    <ul class="nav nav-tabs">
        <li class="nav-item">
            <button class="nav-link active" data-bs-toggle="tab" data-bs-target="#infoTab" type="button">Info</button>
        </li>
    </ul>

    <div class="tab-content border border-top-0 rounded-bottom p-3 bg-white">
        <div class="tab-pane fade show active" id="infoTab">
            <p class="lead mb-3">
                Friendly competition, weekly goals, and a dash of Strava-powered fun. Join in and see how you stack up!
            </p>

            <div class="mb-4 d-flex flex-wrap gap-2">
                <a class="btn btn-primary btn-lg" href="<%= landingPrimaryHref %>"><%= landingPrimaryLabel %></a>
                <a class="btn btn-outline-primary btn-lg" href="<%=request.getContextPath()%>/invite">Use an invitation</a>
                <a class="btn btn-outline-primary btn-lg" href="<%=request.getContextPath()%>/scoring">See how scoring works</a>
            </div>

            <div class="row g-3 mb-4">
                <div class="col-sm-6 col-lg-4">
                    <div class="card h-100">
                        <div class="card-body d-flex flex-column">
                            <h5 class="card-title">Scoring</h5>
                            <p class="card-text">How points work across running, cycling, hiking, and more.</p>
                            <a href="<%=request.getContextPath()%>/scoring" class="btn btn-outline-primary mt-auto align-self-start">View Scoring</a>
                        </div>
                    </div>
                </div>
                <div class="col-sm-6 col-lg-4">
                    <div class="card h-100">
                        <div class="card-body d-flex flex-column">
                            <h5 class="card-title">Terms of Service</h5>
                            <p class="card-text">Safety, fair play, and general participation rules.</p>
                            <a href="<%=request.getContextPath()%>/terms" class="btn btn-outline-primary mt-auto align-self-start">Read Terms of Service</a>
                        </div>
                    </div>
                </div>
                <div class="col-sm-6 col-lg-4">
                    <div class="card h-100">
                        <div class="card-body d-flex flex-column">
                            <h5 class="card-title">Privacy Policy</h5>
                            <p class="card-text">What we collect and how we use it.</p>
                            <a href="<%=request.getContextPath()%>/privacy" class="btn btn-outline-primary mt-auto align-self-start">Privacy Policy</a>
                        </div>
                    </div>
                </div>
            </div>

            <!-- info box: change /auth/login -> /app/ -->
            <div class="alert alert-info">
                Ready to join or already have an account?
                <a class="alert-link" href="<%= landingPrimaryHref %>"><%= landingPrimaryLabel %></a><%= landingAlertSuffix %>
            </div>
        </div>
    </div>
</main>

<%@ include file="/WEB-INF/jspf/footer.jspf" %>
<%@ include file="/WEB-INF/jspf/zenbot.jspf" %>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>


</body>
</html>
