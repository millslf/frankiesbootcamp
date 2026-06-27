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

    private String formatInvitedName(CompetitionInvitationView invitation) {
        if (invitation == null) {
            return "";
        }
        String firstName = invitation.getInvitedFirstname() == null ? "" : invitation.getInvitedFirstname().trim();
        String lastName = invitation.getInvitedLastname() == null ? "" : invitation.getInvitedLastname().trim();
        String fullName = (firstName + " " + lastName).trim();
        if (!fullName.isBlank()) {
            return fullName;
        }
        String displayName = invitation.getInvitedDisplayName() == null ? "" : invitation.getInvitedDisplayName().trim();
        if (!displayName.isBlank()) {
            return displayName;
        }
        return "Invited user";
    }

    private String iconOnlyRewriteButtonClass() {
        return "btn btn-light btn-sm border shadow-sm position-absolute bottom-0 end-0 me-2 mb-2 rounded-circle d-inline-flex align-items-center justify-content-center invite-rewrite-button";
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <%@ include file="/WEB-INF/jspf/head-common.jspf" %>
    <title>Manage invitations</title>
    <style>
        .invite-message-wrap {
            position: relative;
        }

        .invite-message-wrap .form-control {
            padding-right: 3rem;
            padding-bottom: 3rem;
        }

        .invite-rewrite-button {
            width: 2rem;
            height: 2rem;
            padding: 0;
            opacity: 0.75;
        }

        .invite-rewrite-button:hover {
            opacity: 1;
        }
    </style>
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
                            <div class="d-flex align-items-center justify-content-between gap-2 flex-wrap mb-1">
                                <label class="form-label mb-0" for="emails">Invite by email</label>
                            </div>
                            <textarea class="form-control" id="emails" name="emails" rows="4" placeholder="comma, newline, or semicolon separated emails"></textarea>
                        </div>
                        <div class="mb-3">
                            <label class="form-label mb-1" for="message">Invite message</label>
                            <div class="invite-message-wrap">
                                <textarea class="form-control" id="message" name="message" rows="4" placeholder="Add a friendly note for the invite email"><%= inviteMessage == null ? "" : escapeHtml(inviteMessage) %></textarea>
                                <button class="<%= iconOnlyRewriteButtonClass() %>" type="button" id="rewriteInviteMessageBtn" aria-label="AI rewrite invite message" title="AI rewrite invite message">
                                    <i class="bi bi-magic"></i>
                                </button>
                            </div>
                            <div class="form-text">Keep your voice; AI can polish it or draft one from the bootcamp motto.</div>
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
                            <div class="d-flex justify-content-between align-items-start gap-3 flex-wrap">
                                <div>
                                    <div class="fw-semibold">
                                        <% if (invitation.getInvitedUserId() != null && !invitation.getInvitedUserId().isBlank()) { %>
                                        <%= escapeHtml(formatInvitedName(invitation)) %>
                                        <% } else { %>
                                        <%= escapeHtml(invitation.getInvitedEmail()) %>
                                        <% } %>
                                    </div>
                                    <div class="text-muted small">Status: <%= escapeHtml(invitation.getStatus()) %></div>
                                </div>
                                <form method="post" action="<%=pageContextPath%>/app/competition-invitations" onsubmit="return confirm('Remove this pending invitation?');">
                                    <input type="hidden" name="action" value="revokeInvite">
                                    <input type="hidden" name="competitionId" value="<%= view.getCompetitionId() %>">
                                    <input type="hidden" name="invitationId" value="<%= invitation.getId() %>">
                                    <button class="btn btn-outline-danger btn-sm" type="submit">Remove</button>
                                </form>
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

<div class="modal fade" id="rewriteInviteMessageModal" tabindex="-1" aria-labelledby="rewriteInviteMessageModalLabel" aria-hidden="true">
    <div class="modal-dialog modal-lg modal-dialog-scrollable">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title" id="rewriteInviteMessageModalLabel">AI rewrite invite message</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body">
                <p class="text-muted small mb-3" id="rewriteInviteMessageHint">Refine what you wrote, or generate a fresh invite from the Frankies Bootcamp motto.</p>
                <div class="alert alert-info d-none" id="rewriteInviteMessageStatus"></div>
                <textarea class="form-control mb-3" id="rewriteInviteMessageSuggestion" rows="5"></textarea>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-outline-secondary" id="rewriteInviteMessageRegenerateBtn">Regenerate</button>
                <button type="button" class="btn btn-primary" id="rewriteInviteMessageUseBtn">Use this text</button>
            </div>
        </div>
    </div>
</div>
<script>
    (function () {
        function initRewriteInviteMessage() {
        const messageField = document.getElementById('message');
        const rewriteButton = document.getElementById('rewriteInviteMessageBtn');
        const modalElement = document.getElementById('rewriteInviteMessageModal');
        const modalHint = document.getElementById('rewriteInviteMessageHint');
        const modalStatus = document.getElementById('rewriteInviteMessageStatus');
        const suggestionField = document.getElementById('rewriteInviteMessageSuggestion');
        const useButton = document.getElementById('rewriteInviteMessageUseBtn');
        const regenerateButton = document.getElementById('rewriteInviteMessageRegenerateBtn');
        if (!messageField || !rewriteButton || !modalElement || !suggestionField || !useButton || !regenerateButton) {
            return;
        }
        if (!window.bootstrap || !bootstrap.Modal) {
            return;
        }
        const modal = bootstrap.Modal.getOrCreateInstance(modalElement);
        let sourceMessage = '';

        function setStatus(text, isError) {
            if (!modalStatus) {
                return;
            }
            modalStatus.textContent = text || '';
            modalStatus.classList.toggle('d-none', !text);
            modalStatus.classList.toggle('alert-danger', !!isError);
            modalStatus.classList.toggle('alert-info', !isError);
        }

        function loadRewriteSuggestion() {
            const params = new URLSearchParams();
            params.set('action', 'rewriteMessage');
            params.set('competitionId', '<%= view == null ? "" : view.getCompetitionId() %>');
            params.set('message', sourceMessage);
            setStatus('Thinking…', false);
            suggestionField.value = '';
            fetch('<%=pageContextPath%>/app/competition-invitations?' + params.toString(), {
                credentials: 'include',
                cache: 'no-store',
                headers: { 'Accept': 'application/json' }
            })
                .then(function (response) {
                    if (!response.ok) {
                        throw new Error('Could not rewrite invite message right now.');
                    }
                    return response.json();
                })
                .then(function (data) {
                    suggestionField.value = data && data.message ? data.message : '';
                    setStatus(data && data.source === 'generated' ? 'Fresh invite generated from the bootcamp motto.' : 'Invite polished while keeping your message.', false);
                })
                .catch(function () {
                    suggestionField.value = sourceMessage;
                    setStatus('Could not rewrite the message right now.', true);
                });
        }

        rewriteButton.addEventListener('click', function () {
            sourceMessage = messageField.value || '';
            if (modalHint) {
                modalHint.textContent = sourceMessage.trim()
                    ? 'AI will keep your meaning and voice, then polish the invite.'
                    : 'AI will draft a fresh invite from the Frankies Bootcamp motto.';
            }
            loadRewriteSuggestion();
            modal.show();
        });

        regenerateButton.addEventListener('click', function () {
            loadRewriteSuggestion();
        });

        useButton.addEventListener('click', function () {
            messageField.value = suggestionField.value || sourceMessage;
            messageField.dispatchEvent(new Event('input', { bubbles: true }));
            modal.hide();
        });

        document.querySelectorAll('form[data-invite-form="candidate"]').forEach(function (form) {
            form.addEventListener('submit', function () {
                const hiddenMessage = form.querySelector('input[name="message"]');
                if (hiddenMessage) {
                    hiddenMessage.value = messageField.value;
                }
            });
        });
        }

        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', initRewriteInviteMessage);
        } else {
            initRewriteInviteMessage();
        }
    })();
</script>
</body>
</html>
