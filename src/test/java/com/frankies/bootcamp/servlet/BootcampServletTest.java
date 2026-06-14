package com.frankies.bootcamp.servlet;

import com.frankies.bootcamp.model.CompetitionSummaryView;
import com.frankies.bootcamp.model.OnboardingState;
import com.frankies.bootcamp.model.OnboardingStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BootcampServletTest {

    @Test
    void defaultSelectedCompetitionUsesOnlyCurrentActiveCompetition() {
        CompetitionSummaryView active = competition(6L, "Current");
        OnboardingStatus status = status(List.of(active), List.of(competition(7L, "Past")));

        Long selectedCompetitionId = BootcampServlet.defaultSelectedCompetitionId(null, status);

        assertEquals(6L, selectedCompetitionId);
    }

    @Test
    void defaultSelectedCompetitionKeepsExplicitPastSelection() {
        CompetitionSummaryView active = competition(6L, "Current");
        OnboardingStatus status = status(List.of(active), List.of(competition(7L, "Past")));

        Long selectedCompetitionId = BootcampServlet.defaultSelectedCompetitionId(7L, status);

        assertEquals(7L, selectedCompetitionId);
    }

    @Test
    void defaultSelectedCompetitionDoesNotPickBetweenMultipleActiveCompetitions() {
        OnboardingStatus status = status(List.of(competition(6L, "Current"), competition(8L, "Second Current")), List.of());

        Long selectedCompetitionId = BootcampServlet.defaultSelectedCompetitionId(null, status);

        assertNull(selectedCompetitionId);
    }

    private static OnboardingStatus status(List<CompetitionSummaryView> activeCompetitions,
                                           List<CompetitionSummaryView> pastCompetitions) {
        return new OnboardingStatus(
                OnboardingState.READY,
                true,
                true,
                !activeCompetitions.isEmpty(),
                activeCompetitions.isEmpty() ? null : activeCompetitions.get(0),
                activeCompetitions,
                pastCompetitions
        );
    }

    private static CompetitionSummaryView competition(long id, String name) {
        return new CompetitionSummaryView(
                id,
                name,
                "Australia/Sydney",
                Instant.now().getEpochSecond(),
                null,
                "active"
        );
    }
}
