<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.frankies.bootcamp.model.AuthenticatedUser" %>
<%@ page import="com.frankies.bootcamp.model.CompetitionSummaryView" %>
<%@ page import="com.frankies.bootcamp.model.CompetitionInvitationView" %>
<%@ page import="java.time.Instant" %>
<%@ page import="java.time.ZoneId" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%@ page import="java.util.List" %>
<%
    AuthenticatedUser onboardingUser = (AuthenticatedUser) session.getAttribute("authUser");
    Object selectionActiveCompetitionsAttr = request.getAttribute("activeCompetitions");
    Object selectionPastCompetitionsAttr = request.getAttribute("pastCompetitions");
    Object selectionAvailableCompetitionsAttr = request.getAttribute("availableCompetitions");
    Object pendingCompetitionInvitationsAttr = request.getAttribute("pendingCompetitionInvitations");
    Object upcomingCompetitionsAttr = request.getAttribute("upcomingCompetitions");
    Object managedCompetitionIdsAttr = request.getAttribute("managedCompetitionIds");
    Object selectedCompetitionAdminAttr = request.getAttribute("selectedCompetitionAdmin");
    List<CompetitionSummaryView> selectionActiveCompetitions = selectionActiveCompetitionsAttr instanceof List<?> ? (List<CompetitionSummaryView>) selectionActiveCompetitionsAttr : List.of();
    List<CompetitionSummaryView> selectionPastCompetitions = selectionPastCompetitionsAttr instanceof List<?> ? (List<CompetitionSummaryView>) selectionPastCompetitionsAttr : List.of();
    List<CompetitionSummaryView> selectionAvailableCompetitions = selectionAvailableCompetitionsAttr instanceof List<?> ? (List<CompetitionSummaryView>) selectionAvailableCompetitionsAttr : List.of();
    List<CompetitionInvitationView> pendingCompetitionInvitations = pendingCompetitionInvitationsAttr instanceof List<?> ? (List<CompetitionInvitationView>) pendingCompetitionInvitationsAttr : List.of();
    List<CompetitionSummaryView> upcomingCompetitions = upcomingCompetitionsAttr instanceof List<?> ? (List<CompetitionSummaryView>) upcomingCompetitionsAttr : List.of();
    java.util.Set<Long> managedCompetitionIds = managedCompetitionIdsAttr instanceof java.util.Set<?> ? (java.util.Set<Long>) managedCompetitionIdsAttr : java.util.Set.of();
    boolean selectedCompetitionAdmin = selectedCompetitionAdminAttr instanceof Boolean && (Boolean) selectedCompetitionAdminAttr;
    String leaveError = request.getParameter("leaveError");
    String leaveErrorMessage = "last-admin".equals(leaveError) ? "You are the last admin for this competition." : null;
    String displayName = (onboardingUser == null || onboardingUser.getDisplayName() == null || onboardingUser.getDisplayName().isBlank())
            ? "there"
            : onboardingUser.getDisplayName();
    String pageContextPath = request.getContextPath();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
    java.util.Map<Long, CompetitionInvitationView> pendingInviteByCompetitionId = pendingCompetitionInvitations.stream().collect(java.util.stream.Collectors.toMap(CompetitionInvitationView::getCompetitionId, inv -> inv, (left, right) -> left));
    java.util.Set<Long> pendingInviteCompetitionIds = pendingInviteByCompetitionId.keySet();
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

                    <% if (leaveErrorMessage != null) { %>
                    <div class="alert alert-danger mb-4"><%= leaveErrorMessage %></div>
                    <% } %>

                    <h2 class="h5">Current competitions</h2>
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
                            <div class="d-flex gap-2 flex-wrap">
                                <a class="btn btn-sm btn-outline-primary" href="<%=pageContextPath%>/app/select-competition?competitionId=<%= competition.getId() %>">Open</a>
                                <% if (managedCompetitionIds.contains(competition.getId())) { %>
                                <a class="btn btn-sm btn-outline-warning" href="<%=pageContextPath%>/app/competition-invitations?competitionId=<%= competition.getId() %>">Manage invites</a>
                                <form method="post" action="<%=pageContextPath%>/app/competitions" class="m-0" data-loading-submit="true" data-loading-message="Ending competition...">
                                    <input type="hidden" name="action" value="end">
                                    <input type="hidden" name="competitionId" value="<%= competition.getId() %>">
                                    <button type="submit" class="btn btn-sm btn-outline-dark" onclick="return confirm('End <%= competition.getName() %>?');">End competition</button>
                                </form>
                                <% } %>
                                <form method="post" action="<%=pageContextPath%>/app/competitions" class="m-0" onsubmit="return confirm('Leave <%= competition.getName() %>?');" data-loading-submit="true" data-loading-message="Leaving competition...">
                                    <input type="hidden" name="action" value="leave">
                                    <input type="hidden" name="competitionId" value="<%= competition.getId() %>">
                                    <button type="submit" class="btn btn-sm btn-outline-danger">Leave</button>
                                </form>
                            </div>
                        </li>
                        <% } %>
                    </ul>
                    <% } %>

                    <h2 class="h5">Upcoming competitions</h2>
                    <% if (upcomingCompetitions.isEmpty()) { %>
                    <div class="alert alert-info">No upcoming competitions for you right now.</div>
                    <% } else { %>
                    <ul class="list-group mb-4">
                        <% for (CompetitionSummaryView competition : upcomingCompetitions) {
                            String startLabel = Instant.ofEpochSecond(competition.getStartTimestamp()).atZone(ZoneId.of(competition.getTimezone())).format(formatter);
                            String endLabel = competition.getEndTimestamp() == null ? "No end date" : Instant.ofEpochSecond(competition.getEndTimestamp()).atZone(ZoneId.of(competition.getTimezone())).format(formatter);
                        %>
                        <li class="list-group-item d-flex justify-content-between align-items-start">
                            <div>
                                <div class="fw-semibold"><%= competition.getName() %></div>
                                <div class="text-muted small">Start: <%= startLabel %> | End: <%= endLabel %></div>
                            </div>
                            <div class="d-flex gap-2 flex-wrap">
                                <a class="btn btn-sm btn-outline-primary" href="<%=pageContextPath%>/app/select-competition?competitionId=<%= competition.getId() %>">Open</a>
                                <% if (managedCompetitionIds.contains(competition.getId())) { %>
                                <a class="btn btn-sm btn-outline-warning" href="<%=pageContextPath%>/app/competition-invitations?competitionId=<%= competition.getId() %>">Manage invites</a>
                                <form method="post" action="<%=pageContextPath%>/app/competitions" class="m-0" data-loading-submit="true" data-loading-message="Ending competition...">
                                    <input type="hidden" name="action" value="end">
                                    <input type="hidden" name="competitionId" value="<%= competition.getId() %>">
                                    <button type="submit" class="btn btn-sm btn-outline-dark" onclick="return confirm('End <%= competition.getName() %>?');">End competition</button>
                                </form>
                                <% } %>
                            </div>
                        </li>
                        <% } %>
                    </ul>
                    <% } %>

                    <h2 class="h5">Available competitions to join</h2>
                    <% if (selectionAvailableCompetitions.isEmpty()) { %>
                    <div class="alert alert-info">No available competitions right now.</div>
                    <% } else { %>
                    <ul class="list-group mb-4">
                        <% for (CompetitionSummaryView competition : selectionAvailableCompetitions) {
                            String startLabel = Instant.ofEpochSecond(competition.getStartTimestamp()).atZone(ZoneId.of(competition.getTimezone())).format(formatter);
                            String endLabel = competition.getEndTimestamp() == null ? "No end date" : Instant.ofEpochSecond(competition.getEndTimestamp()).atZone(ZoneId.of(competition.getTimezone())).format(formatter);
                        %>
                        <li class="list-group-item d-flex justify-content-between align-items-start">
                            <div>
                                <div class="fw-semibold"><%= competition.getName() %></div>
                                <div class="text-muted small">Open for joining. Start: <%= startLabel %> | End: <%= endLabel %></div>
                            </div>
                            <form method="post" action="<%=pageContextPath%>/app/competitions" class="m-0" data-loading-submit="true" data-loading-message="<%= pendingInviteCompetitionIds.contains(competition.getId()) ? "Accepting invite..." : "Joining competition..." %>">
                                <input type="hidden" name="action" value="<%= pendingInviteCompetitionIds.contains(competition.getId()) ? "accept-invite" : "join" %>">
                                <input type="hidden" name="competitionId" value="<%= competition.getId() %>">
                                <input type="hidden" name="invitationToken" value="<%= pendingInviteCompetitionIds.contains(competition.getId()) ? pendingInviteByCompetitionId.get(competition.getId()).getToken() : "" %>">
                                <button type="submit" class="btn btn-sm btn-outline-primary"><%= pendingInviteCompetitionIds.contains(competition.getId()) ? "Accept invite" : "Join" %></button>
                            </form>
                        </li>
                        <% } %>
                    </ul>
                    <% } %>

                    <h2 class="h5">Past competitions</h2>
                    <% if (selectionPastCompetitions.isEmpty()) { %>
                    <div class="alert alert-info">No past competitions yet.</div>
                    <% } else { %>
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

<div class="modal fade" id="competitionLeaveErrorModal" tabindex="-1" aria-labelledby="competitionLeaveErrorModalLabel" aria-hidden="true">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title" id="competitionLeaveErrorModalLabel">Can't leave competition</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body">
                <div class="alert alert-danger mb-0"><%= leaveErrorMessage == null ? "" : leaveErrorMessage %></div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-primary" data-bs-dismiss="modal">OK</button>
            </div>
        </div>
    </div>
</div>

<script>
    (function () {
        var leaveErrorMessage = <%= leaveErrorMessage == null ? "null" : ("'" + leaveErrorMessage.replace("\\", "\\\\").replace("'", "\\'") + "'") %>;
        if (!leaveErrorMessage) {
            return;
        }
        var modalElement = document.getElementById('competitionLeaveErrorModal');
        if (modalElement && window.bootstrap) {
            bootstrap.Modal.getOrCreateInstance(modalElement).show();
        }
    })();
</script>

</body>
</html>
