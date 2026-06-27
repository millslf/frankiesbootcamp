package com.frankies.bootcamp.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jboss.logging.Logger;

import java.io.IOException;

@ApplicationScoped
public class CompetitionInvitationEmailService {
    private static final Logger LOG = Logger.getLogger(CompetitionInvitationEmailService.class);
    private final OkHttpClient httpClient = new OkHttpClient();

    @Inject
    public CompetitionInvitationEmailService() {
    }

    protected CompetitionInvitationEmailService(boolean ignored) {
    }

    public boolean sendInvitation(String to, String subject, String body) {
        String apiKey = firstNonBlank(System.getProperty("MAILGUN_API_KEY"), System.getenv("MAILGUN_API_KEY"));
        String domain = firstNonBlank(System.getProperty("MAILGUN_DOMAIN"), System.getenv("MAILGUN_DOMAIN"));
        String from = firstNonBlank(System.getProperty("MAILGUN_FROM"), System.getenv("MAILGUN_FROM"));
        String smtpHost = firstNonBlank(System.getProperty("MAILGUN_SMTP_HOST"), System.getenv("MAILGUN_SMTP_HOST"));
        String apiBaseUrl = firstNonBlank(System.getProperty("MAILGUN_API_BASE_URL"), System.getenv("MAILGUN_API_BASE_URL"), inferApiBaseUrl(smtpHost));

        if (from == null && domain != null) {
            from = "invitations@" + domain;
        }
        if (apiKey == null || domain == null || from == null) {
            return false;
        }

        LOG.infof("Sending competition invite email from=%s to=%s domain=%s", from, to, domain);
        Request request = new Request.Builder()
                .url(apiBaseUrl.replaceAll("/+$", "") + "/v3/" + domain + "/messages")
                .addHeader("Authorization", okhttp3.Credentials.basic("api", apiKey))
                .post(new FormBody.Builder()
                        .add("from", from)
                        .add("to", to)
                        .add("subject", subject)
                        .add("text", body)
                        .build())
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String responseBody = response.body() == null ? "" : response.body().string();
                LOG.infof("Competition invite email sent from=%s to=%s domain=%s mailgunResponse=%s", from, to, domain, responseBody);
                return true;
            }
            String responseBody = response.body() == null ? "" : response.body().string();
            LOG.errorf("Competition invite email failed from=%s to=%s domain=%s status=%d response=%s", from, to, domain, response.code(), responseBody);
            throw new RuntimeException("Unable to send invitation email via Mailgun: " + response.code() + " " + responseBody);
        } catch (IOException e) {
            LOG.errorf(e, "Competition invite email IO failure from=%s to=%s domain=%s", from, to, domain);
            throw new RuntimeException("Unable to send invitation email via Mailgun", e);
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private String inferApiBaseUrl(String smtpHost) {
        if (smtpHost != null && smtpHost.contains(".eu.")) {
            return "https://api.eu.mailgun.net";
        }
        return "https://api.mailgun.net";
    }
}
