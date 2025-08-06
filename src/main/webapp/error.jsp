<%@ page isErrorPage="true" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Oops! Something went wrong</title>
    <link rel="stylesheet" href="/css/main.css">
    <!-- Add Bootstrap Icons CDN -->
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.5/font/bootstrap-icons.css">
    <style>
        /* Flex container for h1 to wrap icon and text nicely */
        .container h1 {
            display: flex;
            align-items: center;
            justify-content: center;
            flex-wrap: wrap; /* allow wrapping on small screens */
            gap: 0.5rem; /* space between icon and text */
            font-size: clamp(1.5rem, 3vw, 2.5rem);
        }
        /* Style icon size and vertical alignment */
        .container h1 .bi {
            font-size: 2.5rem;
            line-height: 1;
            vertical-align: middle;
        }
    </style>
</head>
<body>
    <div class="container text-center" style="padding-top: 5rem;">
        <h1><i class="bi bi-person-fill"></i> Frankie's Bootcamp is experiencing issues</h1>
        <p>Please try again later.</p>
        <p style="color: grey;">Error code: 500 - Internal Server Error</p>
    </div>
</body>
</html>
