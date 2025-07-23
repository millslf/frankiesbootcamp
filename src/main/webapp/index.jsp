<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %><%
    response.setHeader("Expires", "Sun, 7 May 1995 12:00:00 GMT"); // Expire in the past
    response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate"); // HTTP 1.1 no-cache
    response.addHeader("Cache-Control", "post-check=0, pre-check=0"); // IE extended no-cache
    response.setHeader("Pragma", "no-cache"); // HTTP 1.0 no-cache
    %>
<!DOCTYPE html>
<html>
	<head>
		<title>JSP - Frankies Bootcamp</title>
		<style>
            .tab-content {
                display: none; /* Hide all tab content by default */
            }
            .active-tab {
                display: block; /* Show active tab content */
            }
            /* Add CSS for tab styling */
            .tab-button {
                padding: 10px;
                cursor: pointer;
                background-color: lightgray;
                border: 1px solid gray;
                border-bottom: none;
				font-size:18px;
            }
            .tab-button.active {
                background-color: white;
            }
            .main-header {
                position: sticky;
                top: 0;
                background-color: white;
                padding: 0px;
                z-index: 1000; /* Ensure it stays above other content */
                box-shadow: 0 2px 5px rgba(0,0,0,0.1);
            }
            .content {
                padding-top: 0px; /* Adjust based on header height to prevent content overlap */
            }
        </style>
		<script>
            function openTab(evt, tabName) {
                var i, tabcontent, tablinks;
                tabcontent = document.getElementsByClassName("tab-content");
                for (i = 0; i < tabcontent.length; i++) {
                    tabcontent[i].style.display = "none";
                }
                tablinks = document.getElementsByClassName("tab-button");
                for (i = 0; i < tablinks.length; i++) {
                    tablinks[i].className = tablinks[i].className.replace(" active", "");
                }
                document.getElementById(tabName).style.display = "block";
                evt.currentTarget.className += " active";
            }
		</script>
	</head>
	<body>
		<div class="main-header">
			<h1><%= "Frankies Bootcamp!" %>
			</h1>
			<button class="tab-button active" onclick="openTab(event, 'Tab1Content')">Weekly History</button>
			<button class="tab-button" onclick="openTab(event, 'Tab2Content')">Honour Roll</button>
			<button class="tab-button" onclick="openTab(event, 'Tab3Content')">Leaderboard</button>
			<button class="tab-button" onclick="openTab(event, 'Tab4Content')">Summary</button>
			<button class="tab-button" onclick="openTab(event, 'Tab5Content')">Scoring</button>
			<button class="tab-button" onclick="openTab(event, 'Tab6Content')">Disclaimer and Privacy</button>
			<button style="float: right;" type="button" onclick="location.href='/auth/logout'">Logout</button>
			<button style="float: right;" type="button" onclick="location.href='https://www.strava.com/oauth/authorize?client_id=143025&redirect_uri=https://frankies.ngrok.io/api/Auth&response_type=code&approval_prompt=auto&scope=activity:read'">Link Strava</button>
		</div>
		<div class="content">
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
				<jsp:include page="/Scoring" />
			</div>
			<div id="Tab6Content" class="tab-content">
				<jsp:include page="/Disclaimer" />
			</div>
			<script>
            // Optionally, set the default active tab on page load
            document.getElementsByClassName("tab-button")[0].click();
        </script>
		</div>
	</body>
</html>