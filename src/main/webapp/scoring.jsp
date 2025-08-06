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
    <title>Scoring - Friendly Exercise Program</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="styles/main.css" rel="stylesheet">  <!-- Link to external stylesheet -->
</head>
<body>
    <div class="container">
        <h1>🎯 Scoring System</h1>
        <p>The aim of the game is to motivate. Not everyone enjoys the same sports, but most people enjoy a little friendly competition. We’ve designed this to level the playing field so everyone can enjoy the activities they prefer.</p>

        <p>Your most important goal is your <strong>weekly personal distance goal</strong>. It's based on scaled distances (see below) and adjusts weekly:</p>

        <ul>
            <li>❌ Missed by more than half → Goal decreases by 10%</li>
            <li>✅ Exceeded by more than 50% → Goal increases by 10%</li>
            <li>🏆 Doubled your goal? → Earn a bonus and goal increases by 20%</li>
        </ul>

        <p>This system supports multiple forms of competition: total distances, personal targets, and overachievement.</p>

        <h2>Scoring</h2>
        <ul>
            <li>🎯 Goal met: <strong>1 point</strong></li>
            <li>⚠️ Missed by less than half: <strong>0.5 points</strong></li>
            <li>💪 Exceeded by more than half: <strong>1.5 points</strong></li>
            <li>🔥 Goal doubled: <strong>1.75 points</strong></li>
        </ul>

        <h2>Relative Distance Conversion</h2>
        <p>Different activities are scaled to normalize effort. Here's how each activity counts toward your goal:</p>

        <ul>
            <li>🏃 Run: 1 km per km</li>
            <li>🏞️ Trail run: 1.2 km per km</li>
            <li>🚴 Gravel ride: 0.4 km per km</li>
            <li>🚵 Mountain bike ride: 0.5 km per km</li>
            <li>🚴 Road ride: 0.33 km per km</li>
            <li>🚵‍♀️ E-MTB ride: 0.4 km per km</li>
            <li>🚲 E-Bike ride: 0.33 km per km</li>
            <li>🏌️ Golf: 0.75 km per km</li>
            <li>🚶 Walk: 0.75 km per km</li>
            <li>🥾 Hike: 0.85 km per km</li>
            <li>🏋️ Weight training: 5 km per hour</li>
            <li>💪 Workout: 5 km per hour</li>
            <li>🏄 Surfing: 7.5 km per hour</li>
            <li>⚽ Hockey/Soccer: 1.5 km per km</li>
            <li>🚣 Virtual Row: 1.0 km per km</li>
            <li>🛶 Kayaking: 2.5 km per km</li>
            <li>🏊 Swim: 5 km per km</li>
            <li>🏄 Stand up paddling: 7.5 km per hour</li>
            <li>🖥️ Virtual Ride: 0.33 km per km</li>
        </ul>
    </div>
</body>
</html>
