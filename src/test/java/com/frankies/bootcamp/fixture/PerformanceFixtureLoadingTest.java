package com.frankies.bootcamp.fixture;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerformanceFixtureLoadingTest {

    private static final Gson GSON = new Gson();

    @Test
    void loadsSanitizedFixtureWithExpectedStructure() throws IOException {
        JsonArray performances = loadFixture("/fixtures/memory-summary-sanitized.json");

        assertEquals(3, performances.size());

        JsonObject first = performances.get(0).getAsJsonObject();
        assertEquals("Alex", first.getAsJsonObject("athlete").get("firstname").getAsString());
        assertEquals(52.86, first.get("distanceToDate").getAsDouble(), 0.0001);
        assertEquals(2.0, first.get("scoreToDate").getAsDouble(), 0.0001);
        assertEquals(3, first.getAsJsonObject("weeklyPerformances").size());
        assertEquals(6, first.getAsJsonArray("stravaActivityDetails").size());
    }

    @Test
    void sanitizedFixtureIncludesRepresentativeActivityDetailsForMutationTests() throws IOException {
        JsonArray performances = loadFixture("/fixtures/memory-summary-sanitized.json");

        JsonObject second = performances.get(1).getAsJsonObject();
        JsonArray secondDetails = second.getAsJsonArray("stravaActivityDetails");
        JsonObject kayak = secondDetails.get(3).getAsJsonObject();
        assertEquals(2002002L, kayak.get("stravaActivityId").getAsLong());
        assertEquals("Kayak", kayak.getAsJsonObject("sport").get("sportType").getAsString());

        JsonObject third = performances.get(2).getAsJsonObject();
        JsonArray thirdDetails = third.getAsJsonArray("stravaActivityDetails");
        JsonObject run = thirdDetails.get(3).getAsJsonObject();
        assertEquals(2, run.get("week").getAsInt());
        assertEquals("Run", run.getAsJsonObject("sport").get("sportType").getAsString());
    }

    @Test
    void sanitizedFixturePreservesAthleteTotalsAndRemainingDistanceScenarios() throws IOException {
        JsonArray performances = loadFixture("/fixtures/memory-summary-sanitized.json");

        JsonObject alex = performances.get(0).getAsJsonObject();
        JsonObject jordan = performances.get(1).getAsJsonObject();
        JsonObject casey = performances.get(2).getAsJsonObject();

        assertEquals(52.86, alex.get("distanceToDate").getAsDouble(), 0.0001);
        assertEquals(55.15, jordan.get("distanceToDate").getAsDouble(), 0.0001);
        assertEquals(24.6, casey.get("distanceToDate").getAsDouble(), 0.0001);

        assertEquals(2.14, remainingDistance(alex, "3"), 0.0001);
        assertEquals(0.1, remainingDistance(jordan, "2"), 0.0001);
        assertEquals(1.0, remainingDistance(casey, "2"), 0.0001);
    }

    @Test
    void sanitizedFixtureSupportsExpectedRankingByDistanceAndScore() throws IOException {
        JsonArray performances = loadFixture("/fixtures/memory-summary-sanitized.json");

        List<String> byDistance = performances.asList().stream()
                .map(JsonElement::getAsJsonObject)
                .sorted((left, right) -> Double.compare(right.get("distanceToDate").getAsDouble(), left.get("distanceToDate").getAsDouble()))
                .map(performance -> performance.getAsJsonObject("athlete").get("firstname").getAsString())
                .toList();

        List<String> byScore = performances.asList().stream()
                .map(JsonElement::getAsJsonObject)
                .sorted((left, right) -> Double.compare(right.get("scoreToDate").getAsDouble(), left.get("scoreToDate").getAsDouble()))
                .map(performance -> performance.getAsJsonObject("athlete").get("firstname").getAsString())
                .toList();

        assertIterableEquals(List.of("Jordan", "Alex", "Casey"), byDistance);
        assertIterableEquals(List.of("Alex", "Jordan", "Casey"), byScore);
    }

    @Test
    void goalCasesFixturePreservesDistanceRemainingAndScoreBands() throws IOException {
        JsonArray performances = loadFixture("/fixtures/memory-summary-goal-cases.json");

        JsonObject underGoal = performances.get(0).getAsJsonObject();
        JsonObject exactGoal = performances.get(1).getAsJsonObject();
        JsonObject overGoal = performances.get(2).getAsJsonObject();

        assertEquals(5.0, remainingDistance(underGoal, "1"), 0.0001);
        assertEquals(0.0, remainingDistance(exactGoal, "1"), 0.0001);
        assertTrue(remainingDistance(overGoal, "1") < 0.0);

        assertEquals(0.5, underGoal.get("scoreToDate").getAsDouble(), 0.0001);
        assertEquals(1.0, exactGoal.get("scoreToDate").getAsDouble(), 0.0001);
        assertEquals(1.5, overGoal.get("scoreToDate").getAsDouble(), 0.0001);
    }

    @Test
    void sanitizedFixtureShowsMixedSportsAndSickWeekState() throws IOException {
        JsonArray performances = loadFixture("/fixtures/memory-summary-sanitized.json");

        JsonObject first = performances.get(0).getAsJsonObject();
        JsonObject weeks = first.getAsJsonObject("weeklyPerformances");

        assertTrue(weeks.getAsJsonObject("3").get("isSick").getAsBoolean());
        assertFalse(weeks.getAsJsonObject("2").get("isSick").getAsBoolean());
        assertEquals(3, weeks.getAsJsonObject("1").getAsJsonObject("sports").size());
    }

    private static double remainingDistance(JsonObject performance, String weekKey) {
        JsonObject week = performance.getAsJsonObject("weeklyPerformances").getAsJsonObject(weekKey);
        return week.get("weekGoal").getAsDouble() - week.get("totalDistance").getAsDouble();
    }

    private static JsonArray loadFixture(String resourcePath) throws IOException {
        InputStream stream = PerformanceFixtureLoadingTest.class.getResourceAsStream(resourcePath);
        assertNotNull(stream, "Fixture not found: " + resourcePath);
        try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return GSON.fromJson(reader, JsonArray.class);
        }
    }
}
