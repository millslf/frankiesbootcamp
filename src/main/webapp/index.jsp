<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %><%
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
      <link href="styles/main.css" rel="stylesheet">
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
      <header class="d-flex align-items-center justify-content-between text-white px-3 py-2 shadow-sm"
         style="min-height:56px; position: sticky; top: 0; z-index: 1030; background-color: #0d6efd;">
         <nav class="navbar navbar-light p-0 flex-shrink-0">
            <div class="container-fluid p-0">
            <button class="navbar-toggler border-white" type="button" data-bs-toggle="offcanvas" data-bs-target="#offcanvasMenu" aria-controls="offcanvasMenu" aria-label="Toggle navigation">
            <span class="navbar-toggler-icon"></span>
            </button>
            <div class="offcanvas offcanvas-start" tabindex="-1" id="offcanvasMenu" aria-labelledby="offcanvasMenuLabel">
               <div class="offcanvas-header">
                  <h5 class="offcanvas-title" id="offcanvasMenuLabel">Navigation & Tools</h5>
                  <button type="button" class="btn-close text-reset" data-bs-dismiss="offcanvas" aria-label="Close"></button>
               </div>
               <div class="offcanvas-body">
                  <div class="btn-group-vertical w-100">
                     <!-- Auth Buttons -->
                     <button class="btn btn-outline-secondary mb-2" onclick="location.href='/auth/login'">
                     <i class="bi bi-box-arrow-in-right me-1"></i> Login
                     </button>
                     <button class="btn btn-outline-secondary mb-2" onclick="location.href='/auth/logout'">
                     <i class="bi bi-box-arrow-right me-1"></i> Logout
                     </button>
                     <button class="btn btn-outline-primary mb-4" onclick="location.href='https://www.strava.com/oauth/authorize?client_id=143025&redirect_uri=https://www.frankiesbootcamp.com/api/Auth&response_type=code&approval_prompt=auto&scope=activity:read'">
                     <i class="bi bi-link-45deg me-1"></i> Link Strava
                     </button>
                     <!-- Extra Tabs -->
                     <button class="btn btn-outline-dark mb-2" onclick="openTab(event, 'Tab5Content');" data-bs-dismiss="offcanvas">
                     <i class="bi bi-star me-1"></i> Scoring
                     </button>
                     <button class="btn btn-outline-dark mb-2" onclick="openTab(event, 'Tab6Content');" data-bs-dismiss="offcanvas">
                     <i class="bi bi-exclamation-triangle me-1"></i> Disclaimer
                     </button>
                     <button class="btn btn-outline-dark" onclick="openTab(event, 'Tab7Content');" data-bs-dismiss="offcanvas">
                     <i class="bi bi-shield-lock me-1"></i> Privacy
                     </button>
                  </div>
               </div>
            </div>
         </nav>
         <h1 class="mb-0 ms-3 text-truncate" style="max-width: calc(100vw - 80px); font-size: clamp(1rem, 3vw, 1.5rem);">
            🏋️ Frankies Bootcamp!
         </h1>
      </header>
      <!-- Tabs Container -->
      <div id="tabsContainer" class="overflow-auto"
         style="position: sticky; top: 56px; z-index: 1020; background-color: white; border-bottom: 1px solid #ddd;">
         <div class="d-flex flex-row flex-nowrap">
            <button class="tab-button active" onclick="openTab(event, 'Tab1Content')"><i class="bi bi-hourglass-bottom header-icon"></i>Weekly History</button>
            <button class="tab-button" onclick="openTab(event, 'Tab3Content')"><i class='bi bi-bar-chart-line-fill header-icon'></i>Leaderboard</button>
            <button class="tab-button" onclick="openTab(event, 'Tab2Content')"><i class="bi bi-trophy-fill header-icon"></i>Honour Roll</button>
            <button class="tab-button" onclick="openTab(event, 'Tab4Content')"><i class="bi bi-list-ol header-icon"></i>Summary</button>
         </div>
      </div>
	  <div class="main-content-wrapper">
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
         <div id="Tab6Content" class="tab-content">
            <jsp:include page="/disclaimer.jsp" />
         </div>
         <div id="Tab7Content" class="tab-content">
            <jsp:include page="/privacy.jsp" />
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