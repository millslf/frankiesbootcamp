package com.frankies.bootcamp.factories;

import com.frankies.bootcamp.model.BootcampAthlete;
import net.datafaker.Faker;
import org.mockito.Answers;

import java.time.Instant;
import java.util.Locale;
import java.util.Random;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

public final class BootcampAthleteTestFactory {
    private final Faker faker;
    public BootcampAthleteTestFactory(long seed) {
        this.faker = new Faker(new Locale("en-AU"), new Random(seed)); // reproducible
    }

    /** Mockito mock with randomized, but deterministic, stubbed getters. */
    public BootcampAthlete athleteMock(long expiresInSecondsFromNow) {
        BootcampAthlete a = mock(BootcampAthlete.class, Answers.RETURNS_DEEP_STUBS);

        String first = faker.name().firstName();
        String last  = faker.name().lastName();

        when(a.getId()).thenReturn(faker.number().digits(8));
        when(a.getFirstname()).thenReturn(first);
        when(a.getLastname()).thenReturn(last);
        when(a.getEmail()).thenReturn(faker.internet().safeEmailAddress());
        when(a.getGoal()).thenReturn(faker.number().numberBetween(10, 100) * 1.0); // 10..99
        when(a.isSick(anyInt())).thenReturn(false);
        when(a.getAccessToken()).thenReturn(faker.internet().uuid());

        long expiry = Instant.now().getEpochSecond() + expiresInSecondsFromNow;
        when(a.getExpiresAt()).thenReturn(expiry);

        return a;
    }

    public void expireNow(BootcampAthlete a) {
        when(a.getExpiresAt()).thenReturn(Instant.now().getEpochSecond() - 10);
    }
}

