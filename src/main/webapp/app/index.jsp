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
</head>
<body>
<%@ include file="/WEB-INF/jspf/header.jspf" %>

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
            <div class="page-wrap">
                <div class="page-card">
                    <div class="page-body page-typography">
                        <h2 class="history-heading mb-3"><i class="bi bi-info-circle"></i> Help</h2>
                        <p class="history-subheading mb-4">Tap a section below to view the details.</p>
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

</body>
</html>
