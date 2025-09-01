<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
    response.setHeader("Expires", "Sun, 7 May 1995 12:00:00 GMT");
    response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
    response.addHeader("Cache-Control", "post-check=0, pre-check=0");
    response.setHeader("Pragma", "no-cache");
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Frankies Bootcamp</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css" rel="stylesheet">
    <link href="/styles/main.css" rel="stylesheet">
    <script>
        function openTab(evt, tabName) {
            var i, tabcontent, tablinks;
            tabcontent = document.getElementsByClassName("tab-content");
            for (i = 0; i < tabcontent.length; i++) {
                tabcontent[i].style.display = "none";
            }
            tablinks = document.getElementsByClassName("tab-button");
            for (i = 0; i < tablinks.length; i++) {
                tablinks[i].classList.remove("active");
            }
            document.getElementById(tabName).style.display = "block";
            evt.currentTarget.classList.add("active");
        }
    </script>
</head>
<body>
<%@ include file="/WEB-INF/jspf/header.jspf" %>

<!-- Tabs Container -->
<div id="tabsContainer" class="overflow-auto"
     style="position: sticky; top: 56px; z-index: 1020; background-color: white; border-bottom: 1px solid #ddd;">
    <div class="d-flex flex-row flex-nowrap">
        <button class="tab-button active" onclick="openTab(event, 'Tab1Content')"><i
                class="bi bi-hourglass-bottom header-icon"></i>Weekly History
        </button>
        <button class="tab-button" onclick="openTab(event, 'Tab3Content')"><i
                class='bi bi-bar-chart-line-fill header-icon'></i>Leaderboard
        </button>
        <button class="tab-button" onclick="openTab(event, 'Tab2Content')"><i class="bi bi-trophy-fill header-icon"></i>Honour
            Roll
        </button>
        <button class="tab-button" onclick="openTab(event, 'Tab4Content')"><i class="bi bi-list-ol header-icon"></i>Summary
        </button>
    </div>
</div>

<div class="main-content-wrapper">
    <div class="container-fluid">
        <div id="Tab1Content" class="tab-content active-tab">
            <jsp:include page="/app/AthleteHistory"/>
        </div>
        <div id="Tab2Content" class="tab-content">
            <jsp:include page="/app/HonourRoll"/>
        </div>
        <div id="Tab3Content" class="tab-content">
            <jsp:include page="/app/LeaderBoard"/>
        </div>
        <div id="Tab4Content" class="tab-content">
            <jsp:include page="/app/AthleteSummary"/>
        </div>
        <div id="Tab5Content" class="tab-content">
            <%@ include file="/WEB-INF/jspf/scoring-content.jspf" %>
        </div>
        <div id="Tab6Content" class="tab-content">
            <%@ include file="/WEB-INF/jspf/disclaimer-content.jspf" %>
        </div>
        <div id="Tab7Content" class="tab-content">
            <%@ include file="/WEB-INF/jspf/privacy-content.jspf" %>
        </div>
    </div>
</div>

<script>
    document.getElementsByClassName("tab-button")[0].click(); // Default to first tab
</script>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
<script>
    function openTab(evt, tabName) {
        var i, tabcontent, tablinks;
        tabcontent = document.getElementsByClassName("tab-content");
        for (i = 0; i < tabcontent.length; i++) {
            tabcontent[i].style.display = "none";
        }
        tablinks = document.getElementsByClassName("tab-button");
        for (i = 0; i < tablinks.length; i++) {
            tablinks[i].classList.remove("active");
        }
        document.getElementById(tabName).style.display = "block";
        if (evt.currentTarget.classList.contains("tab-button")) {
            evt.currentTarget.classList.add("active");
        }
    }
</script>
</body>
</html>
