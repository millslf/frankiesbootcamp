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
    <meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
    <%@ include file="/WEB-INF/jspf/head-common.jspf" %>
    <title>Frankies Bootcamp</title>
    <style>
        #appBottomTabs {
            position: fixed;
            left: 0;
            right: 0;
            bottom: 0;
            z-index: 1030;
            background: #fff;
            border-top: 1px solid #dee2e6;
            box-shadow: 0 -0.25rem 0.75rem rgba(0, 0, 0, 0.08);
        }

        #appBottomTabs .tab-button {
            flex: 1 1 20%;
            min-width: 0;
            border: 0;
            background: transparent;
            color: #6c757d;
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            gap: 0.1rem;
            padding: 0.45rem 0.15rem calc(0.45rem + env(safe-area-inset-bottom, 0px));
            font-size: 0.72rem;
            line-height: 1.1;
            text-align: center;
            white-space: nowrap;
        }

        #appBottomTabs .tab-button .header-icon {
            font-size: 1rem;
            margin-right: 0;
        }

        #appBottomTabs .tab-button.active,
        #appBottomTabs .tab-button:hover {
            color: #0d6efd;
            border-bottom: 0;
            background: rgba(13, 110, 253, 0.08);
        }

        #appBottomTabs + .main-content-wrapper {
            padding-bottom: calc(4.8rem + env(safe-area-inset-bottom, 0px));
        }
    </style>
</head>
<body class="has-bottom-tabs">
<%@ include file="/WEB-INF/jspf/header.jspf" %>

<div id="appBottomTabs" class="d-flex flex-row flex-nowrap">
    <button class="tab-button active" data-tab-content="Tab1Content" data-audit-tab="history" onclick="openTab(event, 'Tab1Content', 'history')"><i class="bi bi-hourglass-bottom header-icon"></i><span>History</span></button>
    <button class="tab-button" data-tab-content="Tab2Content" data-audit-tab="leaderboard" onclick="openTab(event, 'Tab2Content', 'leaderboard')"><i class="bi bi-bar-chart-line-fill header-icon"></i><span>Board</span></button>
    <button class="tab-button" data-tab-content="Tab3Content" data-audit-tab="honour-roll" onclick="openTab(event, 'Tab3Content', 'honour-roll')"><i class="bi bi-trophy-fill header-icon"></i><span>Roll</span></button>
    <button class="tab-button" data-tab-content="Tab4Content" data-audit-tab="insights" onclick="openTab(event, 'Tab4Content', 'insights')"><i class="bi bi-lightbulb-fill header-icon"></i><span>Insights</span></button>
    <button class="tab-button" data-tab-content="Tab5Content" data-audit-tab="help" onclick="openTab(event, 'Tab5Content', 'help')"><i class="bi bi-info-circle header-icon"></i><span>Help</span></button>
</div>

<div class="main-content-wrapper">
    <div class="container-fluid">
        <div id="Tab1Content" class="tab-content active-tab">
            <jsp:include page="/app/AthleteHistory"/>
        </div>
        <div id="Tab2Content" class="tab-content">
            <jsp:include page="/app/LeaderBoard"/>
        </div>
        <div id="Tab3Content" class="tab-content">
            <jsp:include page="/app/HonourRoll"/>
        </div>
        <div id="Tab4Content" class="tab-content">
            <jsp:include page="/app/Insights"/>
        </div>
        <div id="Tab5Content" class="tab-content">
            <div class="container py-3">
                <div class="row justify-content-center">
                    <div class="col-lg-8">
                        <div class="card shadow-sm border-0">
                            <div class="card-body p-4">
                                <h1 class="h4 mb-3">Help</h1>
                                <p class="text-muted mb-4">Tap a section to open it in a clean modal.</p>
                                <div class="d-grid gap-2">
                                    <button class="btn btn-outline-primary btn-lg" type="button" data-bs-toggle="modal" data-bs-target="#helpScoringModal">
                                        Scoring
                                    </button>
                                    <button class="btn btn-outline-primary btn-lg" type="button" data-bs-toggle="modal" data-bs-target="#helpTermsModal">
                                        Terms of Service
                                    </button>
                                    <button class="btn btn-outline-primary btn-lg" type="button" data-bs-toggle="modal" data-bs-target="#helpPrivacyModal">
                                        Privacy Policy
                                    </button>
                                    <button class="btn btn-outline-primary btn-lg" type="button" data-bs-toggle="modal" data-bs-target="#helpContactModal">
                                        Contact us
                                    </button>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>

<%@ include file="/WEB-INF/jspf/zenbot.jspf" %>

<div class="modal fade" id="helpScoringModal" tabindex="-1" aria-labelledby="helpScoringModalLabel" aria-hidden="true">
    <div class="modal-dialog modal-xl modal-dialog-scrollable">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title" id="helpScoringModalLabel">Scoring</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body p-0">
                <%@ include file="/WEB-INF/jspf/scoring-content.jspf" %>
            </div>
        </div>
    </div>
</div>

<div class="modal fade" id="helpTermsModal" tabindex="-1" aria-labelledby="helpTermsModalLabel" aria-hidden="true">
    <div class="modal-dialog modal-xl modal-dialog-scrollable">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title" id="helpTermsModalLabel">Terms of Service</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body p-0">
                <%@ include file="/WEB-INF/jspf/terms-content.jspf" %>
            </div>
        </div>
    </div>
</div>

<div class="modal fade" id="helpPrivacyModal" tabindex="-1" aria-labelledby="helpPrivacyModalLabel" aria-hidden="true">
    <div class="modal-dialog modal-xl modal-dialog-scrollable">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title" id="helpPrivacyModalLabel">Privacy Policy</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body p-0">
                <%@ include file="/WEB-INF/jspf/privacy-content.jspf" %>
            </div>
        </div>
    </div>
</div>

<div class="modal fade" id="helpContactModal" tabindex="-1" aria-labelledby="helpContactModalLabel" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title" id="helpContactModalLabel">Contact us</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body">
                <p class="mb-3">Questions, bugs, or gentle roast requests:</p>
                <a class="btn btn-primary" href="mailto:support@mail.frankiesbootamp.com">support@mail.frankiesbootamp.com</a>
            </div>
        </div>
    </div>
</div>

<script>
    function openTab(evt, tabName, auditTab) {
        var i, tabcontent, tablinks;
        tabcontent = document.getElementsByClassName("tab-content");
        for (i = 0; i < tabcontent.length; i++) {
            tabcontent[i].style.display = "none";
        }
        tablinks = document.getElementsByClassName("tab-button");
        for (i = 0; i < tablinks.length; i++) {
            tablinks[i].classList.remove("active");
        }

        var tabElement = document.getElementById(tabName);
        if (!tabElement) {
            return;
        }

        tabElement.style.display = "block";
        if (evt && evt.currentTarget && evt.currentTarget.classList.contains("tab-button")) {
            evt.currentTarget.classList.add("active");
        } else {
            var matchingButton = document.querySelector('.tab-button[data-tab-content="' + tabName + '"]');
            if (matchingButton) {
                matchingButton.classList.add("active");
            }
        }

        if (auditTab && evt && evt.isTrusted) {
            rememberActiveTab(auditTab);
            fetch('<%=request.getContextPath()%>/app/TabAudit', {
                method: 'POST',
                credentials: 'include',
                cache: 'no-store',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
                body: 'tab=' + encodeURIComponent(auditTab)
            }).catch(function () {
                // keep tab navigation resilient even if audit logging fails
            });
        }
    }

    function rememberActiveTab(tabName) {
        if (!window.history || !window.URL) {
            return;
        }
        var url = new URL(window.location.href);
        url.searchParams.set('tab', tabName);
        url.hash = '';
        window.history.replaceState({}, '', url);
    }

    function requestedTab() {
        var url = new URL(window.location.href);
        var tab = url.searchParams.get('tab');
        if (!tab && window.location.hash) {
            tab = window.location.hash.substring(1);
        }
        return tab || 'history';
    }

    function activateRequestedTab() {
        var tab = requestedTab();
        var button = document.querySelector('.tab-button[data-audit-tab="' + tab + '"]')
            || document.querySelector('.tab-button[data-audit-tab="history"]');
        if (!button) {
            return;
        }
        openTab({ currentTarget: button, isTrusted: false }, button.getAttribute('data-tab-content'), button.getAttribute('data-audit-tab'));
    }

    activateRequestedTab();
    fetch('<%=request.getContextPath()%>/app/TabAudit', {
        method: 'POST',
        credentials: 'include',
        cache: 'no-store',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
        body: 'tab=' + encodeURIComponent('landing')
    }).catch(function () {
        // keep landing resilient even if audit logging fails
    });
</script>
</body>
</html>
