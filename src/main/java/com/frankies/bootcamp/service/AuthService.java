package com.frankies.bootcamp.service;

import com.frankies.bootcamp.model.AuthenticatedUser;
import com.frankies.bootcamp.model.BootcampAthlete;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.sql.SQLException;
import java.util.Locale;

@ApplicationScoped
public class AuthService {
    // Identity model notes:
    // - app_users is the canonical application identity used by session auth and future RBAC.
    // - auth_identities allows multiple login providers to point at the same logical user later.
    // - athletes remains the current domain/person record for compatibility with the existing app.
    private DBService db;

    @Inject
    public AuthService(DBService db) {
        this.db = db;
    }

    protected AuthService() {
    }

    public AuthenticatedUser resolveAuthenticatedUser(String email) throws SQLException {
        if (email == null || email.isBlank()) {
            return null;
        }
        return db.findAuthUserByEmail(normalizeEmail(email));
    }

    public AuthenticatedUser loginOrProvisionExternal(String provider, String providerSubject, String email, String displayName) throws SQLException {
        if (provider == null || provider.isBlank() || providerSubject == null || providerSubject.isBlank()) {
            throw new IllegalArgumentException("External identity details are required.");
        }

        AuthenticatedUser byIdentity = db.findAuthUserByProvider(provider, providerSubject);
        if (byIdentity != null) {
            db.linkAthleteToUser(byIdentity.getAthleteId(), byIdentity.getUserId());
            return byIdentity;
        }

        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail != null) {
            AuthenticatedUser existing = db.findAuthUserByEmail(normalizedEmail);
            if (existing != null) {
                db.linkIdentity(existing.getUserId(), provider, providerSubject, normalizedEmail, null);
                db.linkAthleteToUser(existing.getAthleteId(), existing.getUserId());
                return db.findAuthUserByEmail(normalizedEmail);
            }
        }

        if (normalizedEmail == null || normalizedEmail.isBlank()) {
            throw new IllegalArgumentException("External login did not return an email address.");
        }

        BootcampAthlete athlete = db.findAthleteByEmail(normalizedEmail);

        String handle = suggestHandle(normalizedEmail, displayName);
        String athleteId = athlete == null ? null : athlete.getId();
        String userId = db.createAuthUser(athleteId, normalizedEmail, handle, displayName == null || displayName.isBlank() ? handle : displayName.trim());
        if (athleteId != null) {
            db.linkAthleteToUser(athleteId, userId);
        }
        db.linkIdentity(userId, provider, providerSubject, normalizedEmail, null);
        return db.findAuthUserByEmail(normalizedEmail);
    }

    public BootcampAthlete loadAthleteForUser(AuthenticatedUser user) throws SQLException {
        if (user == null) {
            return null;
        }
        BootcampAthlete athlete = db.findAthleteByUserId(user.getUserId());
        if (athlete == null && user.getAthleteId() != null) {
            athlete = db.findAthleteByStravaID(user.getAthleteId());
            if (athlete != null) {
                db.linkAthleteToUser(athlete.getId(), user.getUserId());
                athlete.setUserId(user.getUserId());
            }
        }
        if (athlete != null && (athlete.getEmail() == null || athlete.getEmail().isBlank())) {
            athlete.setEmail(user.getEmail());
        }
        return athlete;
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private String suggestHandle(String email, String displayName) {
        if (displayName != null && !displayName.isBlank()) {
            return displayName.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
        }
        int at = email.indexOf('@');
        return at > 0 ? email.substring(0, at) : email;
    }

    private String extractFirstName(String displayName, String email) {
        if (displayName != null && !displayName.isBlank()) {
            String[] parts = displayName.trim().split("\\s+");
            if (parts.length > 0) {
                return parts[0];
            }
        }
        int at = email.indexOf('@');
        return at > 0 ? email.substring(0, at) : email;
    }

    private String extractLastName(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            return null;
        }
        String[] parts = displayName.trim().split("\\s+", 2);
        return parts.length > 1 ? parts[1] : null;
    }
}
