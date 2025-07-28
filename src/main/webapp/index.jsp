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
    <link href="styles/main.css" rel="stylesheet">  <!-- Link to external stylesheet -->
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
    <div class="main-header d-flex justify-content-between align-items-center flex-wrap">
        <div>
            <h1>🏋️ Frankies Bootcamp!</h1>
            <button class="tab-button active" onclick="openTab(event, 'Tab1Content')">Weekly History</button>
            <button class="tab-button" onclick="openTab(event, 'Tab2Content')">Honour Roll</button>
            <button class="tab-button" onclick="openTab(event, 'Tab3Content')">Leaderboard</button>
            <button class="tab-button" onclick="openTab(event, 'Tab4Content')">Summary</button>
            <button class="tab-button" onclick="openTab(event, 'Tab5Content')">Scoring</button>
            <button class="tab-button" onclick="openTab(event, 'Tab6Content')">Disclaimer</button>
            <button class="tab-button" onclick="openTab(event, 'Tab7Content')">Privacy</button>
        </div>
        <div class="btn-group mt-2 mt-md-0">
            <button class="btn btn-outline-secondary me-2" onclick="location.href='/auth/login'">Login</button>
            <button class="btn btn-outline-secondary me-2" onclick="location.href='/auth/logout'">Logout</button>
            <button class="btn btn-outline-primary" onclick="location.href='https://www.strava.com/oauth/authorize?client_id=143025&redirect_uri=https://www.frankiesbootcamp.com/api/Auth&response_type=code&approval_prompt=auto&scope=activity:read'">
                Link Strava
            </button>
        </div>
    </div>

    <div class="container-fluid">
        <div id="Tab1Content" class="tab-content active-tab">
            <jsp:include page="/AthleteHistory" />
        </div>
        <div id="Tab2Content" class="tab-content">
            <jsp:include page="/HonourRoll" />
        </div>
        <div id="Tab3Content" class="tab-content">
            <jsp:include page="/LeaderBoard" />
        </div>
        <div id="Tab4Content" class="tab-content">
            <jsp:include page="/AthleteSummary" />
        </div>
        <div id="Tab5Content" class="tab-content">
            <jsp:include page="/scoring.jsp" />
        </div>
        <div id="Tab6Content" class="tab-content active-tab">
            <jsp:include page="/disclaimer.jsp" />
        </div>
        <div id="Tab7Content" class="tab-content">
            <jsp:include page="/privacy.jsp" />
        </div>
    </div>

    <script>
        // Optional: Ensure Disclaimer tab is shown by default
        document.getElementsByClassName("tab-button")[0].click();
    </script>
</body>
</html>
