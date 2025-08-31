<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
    response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
    response.addHeader("Cache-Control", "post-check=0, pre-check=0");
    response.setHeader("Pragma", "no-cache");
%>
<!doctype html>
<html>
<head>
    <meta charset="utf-8"/>
    <title>Frankies Bootcamp</title>
    <meta name="viewport" content="width=device-width, initial-scale=1"/>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css" rel="stylesheet">
    <link href="/styles/main.css" rel="stylesheet">
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

            <div class="row g-3 mb-4">
                <div class="col-sm-6 col-lg-4">
                    <div class="card h-100">
                        <div class="card-body">
                            <h5 class="card-title">Scoring</h5>
                            <p class="card-text">How points work across running, cycling, hiking, and more.</p>
                            <a href="/scoring.jsp" class="btn btn-outline-primary">View Scoring</a>
                        </div>
                    </div>
                </div>
                <div class="col-sm-6 col-lg-4">
                    <div class="card h-100">
                        <div class="card-body">
                            <h5 class="card-title">Disclaimer</h5>
                            <p class="card-text">Safety, fair play, and general participation rules.</p>
                            <a href="/disclaimer.jsp" class="btn btn-outline-primary">Read Disclaimer</a>
                        </div>
                    </div>
                </div>
                <div class="col-sm-6 col-lg-4">
                    <div class="card h-100">
                        <div class="card-body">
                            <h5 class="card-title">Privacy</h5>
                            <p class="card-text">What we collect and how we use it.</p>
                            <a href="/privacy.jsp" class="btn btn-outline-primary">Privacy Policy</a>
                        </div>
                    </div>
                </div>
            </div>

            <!-- info box: change /auth/login -> /app/ -->
            <div class="alert alert-info">
                Already linked with Strava?
                <a class="alert-link" href="/app/">Login</a> to see your dashboard.
            </div>
        </div>
    </div>
</main>

<%@ include file="/WEB-INF/jspf/footer.jspf" %>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
<script>
    (async function () {
        try {
            const res = await fetch('<%=ctx%>/app/whoami.jsp', { credentials: 'include' });

            let authed = false, data = null;
            if (res.redirected && (res.url.includes('/_ngrok/login') || res.url.includes('/ngrok/login'))) {
                authed = false;
            } else if (res.ok) {
                data = await res.json().catch(() => null);
                authed = !!(data && (data.auth === true || data.auth === "true"));
            }

            const group      = document.querySelector('#offcanvasMenu .btn-group-vertical');
            const homeBtn    = document.getElementById('homeBtn');
            const loginBtn   = document.getElementById('loginBtn');
            const logoutBtn  = document.getElementById('logoutBtn');

            const chip       = document.getElementById('userChip');
            const chipName   = document.getElementById('userChipName');
            const signedBox  = document.getElementById('signedInAs');
            const signedName = document.getElementById('signedInName');

            const name = data?.name || data?.displayName || (data?.email ? data.email.split('@')[0] : null);

            if (authed) {
                // header/offcanvas name
                if (chip && chipName) { chipName.textContent = name || 'You'; chip.classList.remove('d-none'); }
                if (signedBox && signedName) { signedName.textContent = name || data?.email || ''; signedBox.classList.remove('d-none'); }

                // nav buttons
                if (loginBtn) {
                    loginBtn.innerHTML = '<i class="bi bi-speedometer2 me-1"></i> Go to dashboard';
                    loginBtn.href = '<%=ctx%>/app/';
                    if (group && group.firstElementChild !== loginBtn) group.insertBefore(loginBtn, group.firstElementChild);
                }
                if (logoutBtn) logoutBtn.classList.remove('d-none');

            } else {
                // hide name
                if (chip) chip.classList.add('d-none');
                if (signedBox) signedBox.classList.add('d-none');

                // reset nav buttons
                if (loginBtn) {
                    loginBtn.innerHTML = '<i class="bi bi-box-arrow-in-right me-1"></i> Login';
                    loginBtn.href = '/app/';
                    if (group && homeBtn && homeBtn.nextElementSibling !== loginBtn) group.insertBefore(loginBtn, homeBtn.nextElementSibling);
                }
                if (logoutBtn) logoutBtn.classList.add('d-none');
            }
        } catch (_) { /* ignore */ }
    })();
</script>


</body>
</html>
