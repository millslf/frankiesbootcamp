package com.frankies.bootcamp.service;

import com.frankies.bootcamp.model.AuthenticatedUser;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Locale;
import java.util.Set;

@ApplicationScoped
public class CompetitionAccessService {
    private static final Set<String> COMPETITION_SETUP_ALLOWLIST = Set.of(
            "millslf@gmail.com",
            "frankiesbootcamp@gmail.com",
            "conlik@gmail.com",
            "millslf@yahoo.com"
    );

    public boolean canAccessCompetitionSetup(AuthenticatedUser user) {
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            return false;
        }
        return COMPETITION_SETUP_ALLOWLIST.contains(user.getEmail().trim().toLowerCase(Locale.ROOT));
    }
}
