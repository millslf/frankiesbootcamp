package com.frankies.bootcamp.service;

import com.frankies.bootcamp.constant.BootcampConstants;
import com.frankies.bootcamp.model.BootcampAthlete;
import com.frankies.bootcamp.model.CompetitionSetupView;
import com.frankies.bootcamp.model.CompetitionSummaryView;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.wildfly.security.credential.store.CredentialStoreException;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@ApplicationScoped
public class CompetitionSetupService {
    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Australia/Sydney");

    interface CompetitionRepository {
        List<CompetitionSummaryView> listJoinableCompetitions(String athleteId) throws SQLException;

        long createCompetitionWithAdmin(String name, String timezone, long startTimestamp, Long endTimestamp, String athleteId, double startingGoal) throws SQLException;

        void joinCompetition(long competitionId, String athleteId, double startingGoal) throws SQLException;
    }

    private CompetitionRepository competitionRepository;
    private AthleteRefresh athleteRefresh;

    @FunctionalInterface
    interface AthleteRefresh {
        void refresh(BootcampAthlete athlete) throws SQLException, CredentialStoreException, NoSuchAlgorithmException, IOException;
    }

    @Inject
    public CompetitionSetupService(DBService dbService, ActivityProcessFacade activityProcessFacade) {
        this.competitionRepository = new CompetitionRepository() {
            @Override
            public List<CompetitionSummaryView> listJoinableCompetitions(String athleteId) throws SQLException {
                return dbService.listJoinableCompetitions(athleteId);
            }

            @Override
            public long createCompetitionWithAdmin(String name, String timezone, long startTimestamp, Long endTimestamp, String athleteId, double startingGoal) throws SQLException {
                return dbService.createCompetitionWithAdmin(name, timezone, startTimestamp, endTimestamp, athleteId, startingGoal);
            }

            @Override
            public void joinCompetition(long competitionId, String athleteId, double startingGoal) throws SQLException {
                dbService.joinCompetition(competitionId, athleteId, startingGoal);
            }
        };
        this.athleteRefresh = activityProcessFacade::prepareAthleteSummary;
    }

    protected CompetitionSetupService() {
    }

    CompetitionSetupService(CompetitionRepository competitionRepository, AthleteRefresh athleteRefresh) {
        this.competitionRepository = competitionRepository;
        this.athleteRefresh = athleteRefresh;
    }

    public CompetitionSetupView loadView(BootcampAthlete athlete) throws SQLException {
        return new CompetitionSetupView(
                athlete.getId(),
                buildDisplayName(athlete),
                athlete.getGoal(),
                competitionRepository.listJoinableCompetitions(athlete.getId())
        );
    }

    public void createCompetitionAndJoin(BootcampAthlete athlete,
                                         String competitionName,
                                         String timezone,
                                         String startDate,
                                         String endDate,
                                         String startingGoal) throws SQLException, CredentialStoreException, NoSuchAlgorithmException, IOException {
        String trimmedName = trimToNull(competitionName);
        if (trimmedName == null) {
            throw new IllegalArgumentException("Competition name is required.");
        }

        String resolvedTimezone = trimToNull(timezone);
        if (resolvedTimezone == null) {
            resolvedTimezone = DEFAULT_ZONE.getId();
        }

        long startTimestamp = parseStartDate(startDate, resolvedTimezone);
        Long endTimestamp = parseEndDate(endDate, resolvedTimezone, startTimestamp);
        double goal = parseStartingGoal(startingGoal, athlete.getGoal());

        competitionRepository.createCompetitionWithAdmin(trimmedName, resolvedTimezone, startTimestamp, endTimestamp, athlete.getId(), goal);
        athleteRefresh.refresh(athlete);
    }

    public void joinCompetition(BootcampAthlete athlete,
                                String competitionId,
                                String startingGoal) throws SQLException, CredentialStoreException, NoSuchAlgorithmException, IOException {
        long parsedCompetitionId;
        try {
            parsedCompetitionId = Long.parseLong(competitionId);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Competition selection is required.");
        }

        double goal = parseStartingGoal(startingGoal, athlete.getGoal());
        competitionRepository.joinCompetition(parsedCompetitionId, athlete.getId(), goal);
        athleteRefresh.refresh(athlete);
    }

    private long parseStartDate(String startDate, String timezone) {
        String trimmed = trimToNull(startDate);
        if (trimmed == null) {
            return BootcampConstants.START_TIMESTAMP;
        }
        LocalDate localDate = LocalDate.parse(trimmed);
        return localDate.atStartOfDay(ZoneId.of(timezone)).toEpochSecond();
    }

    private Long parseEndDate(String endDate, String timezone, long startTimestamp) {
        String trimmed = trimToNull(endDate);
        if (trimmed == null) {
            return null;
        }

        long endTimestamp = LocalDate.parse(trimmed).atStartOfDay(ZoneId.of(timezone)).toEpochSecond();
        if (endTimestamp < startTimestamp) {
            throw new IllegalArgumentException("End date must be on or after the start date.");
        }
        return endTimestamp;
    }

    private double parseStartingGoal(String startingGoal, Double fallbackGoal) {
        String trimmed = trimToNull(startingGoal);
        if (trimmed == null) {
            return fallbackGoal == null ? 0.0 : fallbackGoal;
        }

        double value = Double.parseDouble(trimmed);
        if (value < 0.0) {
            throw new IllegalArgumentException("Starting goal must be zero or greater.");
        }
        return value;
    }

    private String buildDisplayName(BootcampAthlete athlete) {
        String first = athlete.getFirstname() == null ? "" : athlete.getFirstname().trim();
        String last = athlete.getLastname() == null ? "" : athlete.getLastname().trim();
        String combined = (first + " " + last).trim();
        if (!combined.isEmpty()) {
            return combined;
        }
        return athlete.getEmail() == null ? "Athlete" : athlete.getEmail();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
