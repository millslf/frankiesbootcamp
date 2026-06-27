<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.frankies.bootcamp.model.CompetitionInvitationView" %>
<%@ page import="com.frankies.bootcamp.model.CompetitionSetupView" %>
<%@ page import="com.frankies.bootcamp.model.CompetitionSummaryView" %>
<%@ page import="java.time.Instant" %>
<%@ page import="java.time.ZoneId" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%@ page import="java.util.TreeSet" %>
<%
    CompetitionSetupView setupView = (CompetitionSetupView) request.getAttribute("competitionSetupView");
    List<CompetitionInvitationView> pendingInvitations = (List<CompetitionInvitationView>) request.getAttribute("pendingCompetitionInvitations");
    String pageContextPath = request.getContextPath();
    String error = (String) request.getAttribute("competitionSetupError");
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
    TreeSet<String> availableTimezones = new TreeSet<>(ZoneId.getAvailableZoneIds());
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <%@ include file="/WEB-INF/jspf/head-common.jspf" %>
    <title>Competition setup</title>
</head>
<body class="bg-light">
<%@ include file="/WEB-INF/jspf/header.jspf" %>

<div class="container py-5">
    <div class="row justify-content-center">
        <div class="col-xl-10">
            <div class="text-center mb-4">
                <h1 class="h3 mb-2">Competition setup</h1>
                <p class="text-muted mb-0">Create a competition for your group or join one that already exists.</p>
            </div>

            <% if (error != null && !error.isBlank()) { %>
            <div class="alert alert-danger" role="alert"><%= error %></div>
            <% } %>

            <div class="row g-4">
                <div class="col-lg-6">
                    <div class="card h-100 shadow-sm border-0">
                        <div class="card-body p-4">
                            <h2 class="h5 mb-3">Create a competition</h2>
                            <form method="post" action="<%=pageContextPath%>/app/competition-setup">
                                <input type="hidden" name="action" value="create">

                                <div class="mb-3">
                                    <label class="form-label" for="competitionName">Competition name</label>
                                    <input class="form-control" id="competitionName" name="competitionName" required maxlength="255"
                                           value="Frankies Bootcamp - <%= setupView == null ? "New Competition" : setupView.getAthleteDisplayName() %>">
                                </div>

                                <div class="mb-3">
                                    <label class="form-label" for="timezone">Timezone</label>
                                    <select class="form-select" id="timezone" name="timezone" required>
                                        <% for (String timezone : availableTimezones) { %>
                                        <option value="<%= timezone %>"><%= timezone %></option>
                                        <% } %>
                                    </select>
                                    <div class="form-text">Your device timezone will be selected automatically.</div>
                                </div>

                                <div class="mb-3">
                                    <label class="form-label" for="startDate">Start date</label>
                                    <input class="form-control" id="startDate" name="startDate" type="date" required>
                                </div>

                                <div class="mb-3">
                                    <label class="form-label" for="endDate">End date</label>
                                    <input class="form-control" id="endDate" name="endDate" type="date">
                                </div>

                                <div class="mb-3">
                                    <label class="form-label" for="startingGoal">Default distance</label>
                                    <input class="form-control" id="startingGoal" name="startingGoal" type="number" min="0" step="0.1"
                                           value="<%= setupView == null || setupView.getSuggestedStartingGoal() == null ? "20" : setupView.getSuggestedStartingGoal() %>">
                                </div>

                                <button class="btn btn-primary" type="submit">Create competition and continue to invites</button>
                            </form>
                        </div>
                    </div>
                </div>

                <div class="col-lg-6">
                    <div class="card h-100 shadow-sm border-0">
                        <div class="card-body p-4">
                            <h2 class="h5 mb-3">Join with an invite</h2>
                            <% if (pendingInvitations == null || pendingInvitations.isEmpty()) { %>
                            <div class="alert alert-info mb-0">No pending invitations right now.</div>
                            <% } else { %>
                            <div class="list-group">
                                <% for (CompetitionInvitationView invitation : pendingInvitations) { %>
                                <div class="list-group-item">
                                    <div class="fw-semibold"><%= invitation.getCompetitionName() %></div>
                                    <div class="text-muted small">
                                        <% if (invitation.getInvitedUserId() != null && !invitation.getInvitedUserId().isBlank()) { %>
                                        This invite is linked to your account.
                                        <% } else { %>
                                        Use the invite from your email to join this competition.
                                        <% } %>
                                    </div>
                                    <div class="d-grid d-sm-flex gap-2 mt-3">
                                        <a class="btn btn-primary" href="<%=pageContextPath%>/invite?token=<%= invitation.getToken() %>">Use invite</a>
                                    </div>
                                </div>
                                <% } %>
                            </div>
                            <% } %>
                        </div>
                    </div>
                </div>

            </div>
        </div>
    </div>
</div>
<script>
    (function () {
        const timezoneSelect = document.getElementById('timezone');
        if (!timezoneSelect || !window.Intl || !Intl.DateTimeFormat) {
            return;
        }
        const detectedTimezone = Intl.DateTimeFormat().resolvedOptions().timeZone;
        if (detectedTimezone) {
            timezoneSelect.value = detectedTimezone;
        }
    })();
</script>
</body>
</html>
