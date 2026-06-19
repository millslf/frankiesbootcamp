<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.frankies.bootcamp.model.CompetitionInvitationPageView" %>
<%@ page import="com.frankies.bootcamp.model.CompetitionInvitationView" %>
<%@ page import="com.frankies.bootcamp.model.CompetitionInviteCandidateView" %>
<%@ page import="com.frankies.bootcamp.model.BootcampAthlete" %>
<%@ page import="java.util.List" %>
<%
    CompetitionInvitationPageView view = (CompetitionInvitationPageView) request.getAttribute("invitationAdminView");
    String feedback = (String) request.getAttribute("invitationAdminFeedback");
    String error = (String) request.getAttribute("invitationAdminError");
    String pageContextPath = request.getContextPath();
    String inviteMessage = request.getParameter("message");
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

    private String formatLocation(String city, String state, String country) {
        StringBuilder builder = new StringBuilder();
        if (city != null && !city.isBlank()) {
            builder.append(city.trim());
        }
        if (state != null && !state.isBlank()) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(state.trim());
        }
        if (country != null && !country.isBlank()) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(country.trim());
        }
        return builder.toString();
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <%@ include file="/WEB-INF/jspf/head-common.jspf" %>
    <title>Manage invitations</title>
</head>
<body class="bg-light">
<%@ include file="/WEB-INF/jspf/header.jspf" %>

<div class="container py-4">
    <div class="row justify-content-center">
        <div class="col-lg-10">
            <div class="card shadow-sm border-0 mb-4">
                <div class="card-body p-4">
                    <h1 class="h4 mb-3">Manage invitations for <%= escapeHtml(view == null ? "competition" : view.getCompetitionName()) %></h1>
                    <% if (feedback != null && !feedback.isBlank()) { %>
                    <div class="alert alert-success" role="alert"><%= escapeHtml(feedback) %></div>
                    <% } %>
                    <% if (error != null && !error.isBlank()) { %>
                    <div class="alert alert-danger" role="alert"><%= escapeHtml(error) %></div>
                    <% } %>

                    <form method="post" action="<%=pageContextPath%>/app/competition-invitations">
                        <input type="hidden" name="action" value="bulkInvite">
                        <input type="hidden" name="competitionId" value="<%= view == null ? "" : view.getCompetitionId() %>">
                        <div class="mb-3">
                            <label class="form-label" for="emails">Invite by email</label>
                            <textarea class="form-control" id="emails" name="emails" rows="4" placeholder="comma, newline, or semicolon separated emails"></textarea>
                        </div>
                        <div class="mb-3">
                            <label class="form-label" for="message">Invite message</label>
                            <textarea class="form-control" id="message" name="message" rows="4" placeholder="Add a friendly note for the invite email"><%= inviteMessage == null ? "" : escapeHtml(inviteMessage) %></textarea>
                        </div>
                        <button class="btn btn-primary" type="submit">Send invitations</button>
                    </form>
                </div>
            </div>

            <div class="card shadow-sm border-0 mb-4">
                <div class="card-body p-4">
                    <h2 class="h5 mb-3">Search existing users by name</h2>
                    <form class="mb-3" method="get" action="<%=pageContextPath%>/app/competition-invitations">
                        <input type="hidden" name="competitionId" value="<%= view == null ? "" : view.getCompetitionId() %>">
                        <div class="input-group">
                            <input class="form-control" name="q" value="<%= escapeHtml(view == null || view.getSearchQuery() == null ? "" : view.getSearchQuery()) %>" placeholder="name">
                            <button class="btn btn-outline-primary" type="submit">Search</button>
                        </div>
                    </form>

                    <% if (view != null && view.getCandidates() != null && !view.getCandidates().isEmpty()) { %>
                    <div class="list-group">
                        <% for (CompetitionInviteCandidateView candidate : view.getCandidates()) { %>
                        <div class="list-group-item">
                            <div class="d-flex justify-content-between align-items-start gap-3 flex-wrap">
                                <div>
                                    <div class="d-flex align-items-center gap-2">
                                        <% if (candidate.getProfileMedium() != null && !candidate.getProfileMedium().isBlank()) { %>
                                        <img class="user-avatar" src="<%= escapeHtml(candidate.getProfileMedium()) %>" alt="" onerror="this.style.display='none';this.nextElementSibling.classList.remove('d-none');">
                                        <span class="user-avatar-placeholder d-none"><i class="bi bi-person"></i></span>
                                        <% } else { %>
                                        <span class="user-avatar-placeholder"><i class="bi bi-person"></i></span>
                                        <% } %>
                                        <div class="fw-semibold"><%= escapeHtml(candidate.getDisplayName()) %></div>
                                    </div>
                                    <div class="text-muted small"><%= escapeHtml(maskEmail(candidate.getEmail())) %></div>
                                    <% String candidateLocation = formatLocation(candidate.getCity(), candidate.getState(), candidate.getCountry()); %>
                                    <% if (!candidateLocation.isBlank()) { %>
                                    <div class="text-muted small"><%= escapeHtml(candidateLocation) %></div>
                                    <% } %>
                                </div>
                                <% if (candidate.isAlreadyInCompetition()) { %>
                                <span class="badge text-bg-secondary">Already active</span>
                                <% } else { %>
                                <form method="post" action="<%=pageContextPath%>/app/competition-invitations" data-invite-form="candidate">
                                    <input type="hidden" name="action" value="inviteCandidate">
                                    <input type="hidden" name="competitionId" value="<%= view.getCompetitionId() %>">
                                    <input type="hidden" name="invitedUserId" value="<%= escapeHtml(candidate.getUserId()) %>">
                                    <input type="hidden" name="invitedEmail" value="<%= escapeHtml(candidate.getEmail()) %>">
                                    <input type="hidden" name="message" value="">
                                    <button class="btn btn-sm btn-primary" type="submit">Invite</button>
                                </form>
                                <% } %>
                            </div>
                        </div>
                        <% } %>
                    </div>
                    <% } else { %>
                    <div class="alert alert-info mb-0">Search for people to invite.</div>
                    <% } %>
                </div>
            </div>

            <div class="card shadow-sm border-0">
                <div class="card-body p-4">
                    <h2 class="h5 mb-3">Accepted members</h2>
                    <% if (view == null || view.getAcceptedAthletes() == null || view.getAcceptedAthletes().isEmpty()) { %>
                    <div class="alert alert-info mb-4">No one has joined this competition yet.</div>
                    <% } else { %>
                    <div class="list-group mb-4">
                        <% for (BootcampAthlete accepted : view.getAcceptedAthletes()) { %>
                        <div class="list-group-item">
                            <div class="d-flex align-items-center gap-2">
                                <% if (accepted.getProfileMedium() != null && !accepted.getProfileMedium().isBlank()) { %>
                                <img class="user-avatar" src="<%= escapeHtml(accepted.getProfileMedium()) %>" alt="" onerror="this.style.display='none';this.nextElementSibling.classList.remove('d-none');">
                                <span class="user-avatar-placeholder d-none"><i class="bi bi-person"></i></span>
                                <% } else { %>
                                <span class="user-avatar-placeholder"><i class="bi bi-person"></i></span>
                                <% } %>
                                <div class="fw-semibold"><%= escapeHtml((accepted.getFirstname() == null ? "" : accepted.getFirstname()) + " " + (accepted.getLastname() == null ? "" : accepted.getLastname())) %></div>
                            </div>
                            <div class="text-muted small"><%= escapeHtml(maskEmail(accepted.getEmail())) %></div>
                            <% String location = formatLocation(accepted.getCity(), accepted.getState(), accepted.getCountry()); %>
                            <% if (!location.isBlank()) { %>
                            <div class="text-muted small"><%= escapeHtml(location) %></div>
                            <% } %>
                        </div>
                        <% } %>
                    </div>
                    <% } %>

                    <h2 class="h5 mb-3">Pending invitations</h2>
                    <% if (view == null || view.getPendingInvitations() == null || view.getPendingInvitations().isEmpty()) { %>
                    <div class="alert alert-info mb-0">No pending invitations.</div>
                    <% } else { %>
                    <div class="list-group">
                        <% for (CompetitionInvitationView invitation : view.getPendingInvitations()) { %>
                        <div class="list-group-item">
                            <div class="fw-semibold">
                                <% if (invitation.getInvitedUserId() != null && !invitation.getInvitedUserId().isBlank()) { %>
                                Invite linked to existing user
                                <% } else { %>
                                <%= escapeHtml(invitation.getInvitedEmail()) %>
                                <% } %>
                            </div>
                            <div class="text-muted small">Status: <%= escapeHtml(invitation.getStatus()) %></div>
                        </div>
                        <% } %>
                    </div>
                    <% } %>
                </div>
            </div>
        </div>
    </div>
</div>
<script>
    (function () {
        const messageField = document.getElementById('message');
        if (!messageField) {
            return;
        }
        document.querySelectorAll('form[data-invite-form="candidate"]').forEach(function (form) {
            form.addEventListener('submit', function () {
                const hiddenMessage = form.querySelector('input[name="message"]');
                if (hiddenMessage) {
                    hiddenMessage.value = messageField.value;
                }
            });
        });
    })();
</script>
</body>
</html>
