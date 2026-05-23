package com.frankies.bootcamp.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.annotation.PostConstruct;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ExternalAuthConfig {
    private static final Logger log = Logger.getLogger(ExternalAuthConfig.class);

    @PostConstruct
    void logResolvedConfig() {
        log.infof("Auth0 config resolved: domain=%s clientId=%s baseUrl=%s clientSecret=%s",
                getDomain(),
                getClientId(),
                getBaseUrl(),
                describeSecret(getClientSecret()));
    }

    public String getDomain() {
        return readRequired("AUTH0_DOMAIN");
    }

    public String getClientId() {
        return readRequired("AUTH0_CLIENT_ID");
    }

    public String getClientSecret() {
        return readRequired("AUTH0_CLIENT_SECRET");
    }

    public String getBaseUrl() {
        return readRequired("AUTH0_BASE_URL");
    }

    private String readRequired(String key) {
        String value = System.getProperty(key);
        if (value == null || value.isBlank()) {
            value = System.getenv(key);
        }
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing config: " + key);
        }
        return value.trim();
    }

    private String describeSecret(String value) {
        int visible = Math.min(4, value.length());
        return "len=" + value.length() + ",suffix=" + value.substring(value.length() - visible);
    }
}
