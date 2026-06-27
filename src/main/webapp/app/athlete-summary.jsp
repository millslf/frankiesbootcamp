<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.frankies.bootcamp.model.CompetitionSummaryView" %>
<%@ page import="com.frankies.bootcamp.utils.WildflyUtils" %>
<%@ page import="java.util.List" %>
<%
    String summaryContent = (String) request.getAttribute("summaryContent");
    Long selectedCompetitionId = (Long) request.getAttribute("selectedCompetitionId");
    Object activeCompetitionsAttr = request.getAttribute("activeCompetitions");
    Object pastCompetitionsAttr = request.getAttribute("pastCompetitions");
    List<CompetitionSummaryView> activeCompetitions = activeCompetitionsAttr instanceof List<?> ? (List<CompetitionSummaryView>) activeCompetitionsAttr : List.of();
    List<CompetitionSummaryView> pastCompetitions = pastCompetitionsAttr instanceof List<?> ? (List<CompetitionSummaryView>) pastCompetitionsAttr : List.of();
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
    <%@ include file="/WEB-INF/jspf/head-common.jspf" %>
    <title>Summary - Frankies Bootcamp</title>
</head>
<body>
<%@ include file="/WEB-INF/jspf/header.jspf" %>

<main class="container my-4">
    <div class="card shadow-sm border-0 mb-3">
        <div class="card-body py-3">
            <form class="row g-2 align-items-end" method="get" action="<%=request.getContextPath()%>/app/select-competition">
                <input type="hidden" name="returnTo" value="/app/AthleteSummary">
                <div class="col-md-9">
                    <label class="form-label mb-1" for="competitionId">Switch competition</label>
                    <select class="form-select" id="competitionId" name="competitionId">
                        <% if (!activeCompetitions.isEmpty()) { %>
                        <optgroup label="Current competitions">
                            <% for (CompetitionSummaryView competition : activeCompetitions) { %>
                            <option value="<%= competition.getId() %>" <%= selectedCompetitionId != null && selectedCompetitionId == competition.getId() ? "selected" : "" %>><%= WildflyUtils.escape(competition.getName()) %></option>
                            <% } %>
                        </optgroup>
                        <% } %>
                        <% if (!pastCompetitions.isEmpty()) { %>
                        <optgroup label="Past competitions">
                            <% for (CompetitionSummaryView competition : pastCompetitions) { %>
                            <option value="<%= competition.getId() %>" <%= selectedCompetitionId != null && selectedCompetitionId == competition.getId() ? "selected" : "" %>><%= WildflyUtils.escape(competition.getName()) %></option>
                            <% } %>
                        </optgroup>
                        <% } %>
                    </select>
                </div>
                <div class="col-md-3 d-grid">
                    <button class="btn btn-outline-primary" type="submit">Open</button>
                </div>
            </form>
        </div>
    </div>

    <div class="card shadow-sm border-0">
        <div class="card-body p-4">
            <h1 class="h4 mb-3"><i class="bi bi-trophy-fill me-2"></i>Performance Summary</h1>
            <div class="mt-2">
                <%= WildflyUtils.escape(summaryContent == null ? "" : summaryContent) %>
            </div>
            <hr/>
            <p class="text-muted mb-0"><i class="bi bi-award-fill text-warning me-2"></i>Keep training hard and breaking limits!</p>
        </div>
    </div>
</main>

<%@ include file="/WEB-INF/jspf/footer.jspf" %>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
