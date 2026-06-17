package com.frankies.bootcamp.model;

public enum CompetitionInvitationStatus {
    PENDING,
    ACCEPTED,
    DECLINED,
    EXPIRED;

    public String dbValue() {
        return name().toLowerCase();
    }

    public static CompetitionInvitationStatus fromDbValue(String value) {
        if (value == null) {
            return null;
        }
        return CompetitionInvitationStatus.valueOf(value.trim().toUpperCase());
    }
}
