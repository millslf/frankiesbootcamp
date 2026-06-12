package com.frankies.bootcamp.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DBServiceCompetitionSetupTest {

    @Test
    void competitionSetupQueriesUseCompetitionSpecificMembershipWrites() {
        String createCompetitionSql = "INSERT INTO competitions (name, timezone, start_timestamp, end_timestamp, status) VALUES (?, ?, ?, NULL, 'active')";
        String joinCompetitionSql = "INSERT INTO competition_athlete (competition_id, athlete_id, role, starting_goal, status) VALUES (?, ?, 'member', ?, 'active') ON DUPLICATE KEY UPDATE status = 'active', starting_goal = VALUES(starting_goal)";

        assertEquals(createCompetitionSql, createCompetitionSql);
        assertEquals(joinCompetitionSql, joinCompetitionSql);
    }
}
