package com.frankies.bootcamp.service;

public enum ActivityProcessingMode {
    IN_MEMORY,
    PERSISTENT;

    public static ActivityProcessingMode fromProperty(String raw) {
        if (raw == null || raw.isBlank()) {
            return IN_MEMORY;
        }
        return switch (raw.trim().toLowerCase()) {
            case "persistent", "db", "database" -> PERSISTENT;
            default -> IN_MEMORY;
        };
    }
}
