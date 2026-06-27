package com.frankies.bootcamp.util;

public final class EmailDisplayUtil {
    private EmailDisplayUtil() {
    }

    public static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "";
        }
        int at = email.indexOf('@');
        if (at <= 0 || at == email.length() - 1) {
            return email;
        }
        String localPart = email.substring(0, at);
        String domain = email.substring(at);
        if (localPart.length() <= 2) {
            return localPart.substring(0, 1) + "***" + domain;
        }
        return localPart.substring(0, 1) + "***" + localPart.substring(localPart.length() - 1) + domain;
    }
}
