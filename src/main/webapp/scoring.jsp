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
    <meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
    <title>Scoring - Friendly Exercise Program</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="/styles/main.css" rel="stylesheet">  <!-- Link to external stylesheet -->
</head>
<body>
    <%@ include file="/WEB-INF/jspf/header.jspf" %>
    <%@ include file="/WEB-INF/jspf/scoring-content.jspf" %>
    <%@ include file="/WEB-INF/jspf/footer.jspf" %>
    <!-- Load Bootstrap bundle ONCE per page, at the bottom -->
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
