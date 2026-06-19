package com.frankies.bootcamp.service;

import com.frankies.bootcamp.constant.BootcampConstants;
import com.frankies.bootcamp.model.BootcampAthlete;
import com.frankies.bootcamp.model.CompetitionSetupView;
import com.frankies.bootcamp.model.CompetitionSummaryView;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompetitionSetupServiceTest {

    private final RecordingAthleteRefresh athleteRefresh = new RecordingAthleteRefresh();

    @Test
    void loadViewReturnsSuggestedGoalAndJoinableCompetitions() throws Exception {
        FakeCompetitionDbService dbService = new FakeCompetitionDbService();
        dbService.joinableCompetitions.add(new CompetitionSummaryView(2L, "Autumn Challenge", "Australia/Sydney", BootcampConstants.START_TIMESTAMP, null, "active"));

        CompetitionSetupService service = new CompetitionSetupService(dbService, athleteRefresh);
        CompetitionSetupView view = service.loadView(athlete("athlete-1", 22.5));

        assertEquals("athlete-1", view.getAthleteId());
        assertEquals("athlete-1", dbService.listJoinableCompetitionsAthleteId);
        assertEquals(22.5, view.getSuggestedStartingGoal(), 0.0001);
        assertEquals(1, view.getActiveCompetitions().size());
    }

    @Test
    void createCompetitionAndJoinDelegatesValidatedValues() throws Exception {
        FakeCompetitionDbService dbService = new FakeCompetitionDbService();
        CompetitionSetupService service = new CompetitionSetupService(dbService, athleteRefresh);

        BootcampAthlete athlete = athlete("athlete-1", 18.0);

        long competitionId = service.createCompetitionAndJoin(athlete, "Winter Bootcamp", "Australia/Sydney", "2026-07-01", "2026-09-30", "25");

        assertEquals("Winter Bootcamp", dbService.createdCompetitionName);
        assertEquals("Australia/Sydney", dbService.createdCompetitionTimezone);
        assertEquals("athlete-1", dbService.createdCompetitionAthleteId);
        assertEquals(25.0, dbService.createdCompetitionGoal, 0.0001);
        assertEquals(9L, competitionId);
        assertEquals(LocalDate.parse("2026-09-30").atStartOfDay(ZoneId.of("Australia/Sydney")).toEpochSecond(), dbService.createdCompetitionEndTimestamp);
        assertEquals(athlete, athleteRefresh.lastAthlete);
        assertEquals(1, athleteRefresh.refreshCalls);
        long expectedStart = LocalDate.parse("2026-07-01").atStartOfDay(ZoneId.of("Australia/Sydney")).toEpochSecond();
        assertEquals(expectedStart, dbService.createdCompetitionStartTimestamp);
    }

    @Test
    void joinCompetitionUsesFallbackGoalWhenBlank() throws Exception {
        FakeCompetitionDbService dbService = new FakeCompetitionDbService();
        CompetitionSetupService service = new CompetitionSetupService(dbService, athleteRefresh);

        BootcampAthlete athlete = athlete("athlete-1", 19.0);

        service.joinCompetition(athlete, "3", " ");

        assertEquals(3L, dbService.joinCompetitionId);
        assertEquals("athlete-1", dbService.joinAthleteId);
        assertEquals(19.0, dbService.joinGoal, 0.0001);
        assertEquals(athlete, athleteRefresh.lastAthlete);
        assertEquals(1, athleteRefresh.refreshCalls);
    }

    @Test
    void createCompetitionRejectsBlankName() throws Exception {
        CompetitionSetupService service = new CompetitionSetupService(new FakeCompetitionDbService(), athleteRefresh);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.createCompetitionAndJoin(athlete("athlete-1", 20.0), "   ", "Australia/Sydney", "2026-07-01", null, "20"));

        assertTrue(error.getMessage().contains("Competition name"));
    }

    @Test
    void createCompetitionRejectsEndDateBeforeStartDate() {
        CompetitionSetupService service = new CompetitionSetupService(new FakeCompetitionDbService(), athleteRefresh);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.createCompetitionAndJoin(athlete("athlete-1", 20.0), "Spring", "Australia/Sydney", "2026-07-10", "2026-07-01", "20"));

        assertTrue(error.getMessage().contains("End date"));
    }

    private static BootcampAthlete athlete(String athleteId, Double goal) {
        BootcampAthlete athlete = new BootcampAthlete();
        athlete.setId(athleteId);
        athlete.setFirstname("Casey");
        athlete.setLastname("Runner");
        athlete.setEmail("casey@example.com");
        athlete.setGoal(goal);
        return athlete;
    }

    private static final class FakeCompetitionDbService implements CompetitionSetupService.CompetitionRepository {
        private final List<CompetitionSummaryView> joinableCompetitions = new ArrayList<>();
        private String listJoinableCompetitionsAthleteId;
        private String createdCompetitionName;
        private String createdCompetitionTimezone;
        private long createdCompetitionStartTimestamp;
        private Long createdCompetitionEndTimestamp;
        private String createdCompetitionAthleteId;
        private double createdCompetitionGoal;
        private long joinCompetitionId;
        private String joinAthleteId;
        private double joinGoal;

        @Override
        public List<CompetitionSummaryView> listJoinableCompetitions(String athleteId) {
            this.listJoinableCompetitionsAthleteId = athleteId;
            return joinableCompetitions;
        }

        @Override
        public long createCompetitionWithAdmin(String name, String timezone, long startTimestamp, Long endTimestamp, String athleteId, double startingGoal) {
            this.createdCompetitionName = name;
            this.createdCompetitionTimezone = timezone;
            this.createdCompetitionStartTimestamp = startTimestamp;
            this.createdCompetitionEndTimestamp = endTimestamp;
            this.createdCompetitionAthleteId = athleteId;
            this.createdCompetitionGoal = startingGoal;
            return 9L;
        }

        @Override
        public void joinCompetition(long competitionId, String athleteId, double startingGoal) {
            this.joinCompetitionId = competitionId;
            this.joinAthleteId = athleteId;
            this.joinGoal = startingGoal;
        }
    }

    private static final class RecordingAthleteRefresh implements CompetitionSetupService.AthleteRefresh {
        private BootcampAthlete lastAthlete;
        private int refreshCalls;

        @Override
        public void refresh(BootcampAthlete athlete) {
            this.lastAthlete = athlete;
            this.refreshCalls++;
        }
    }
}
