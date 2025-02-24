package com.frankies.bootcamp.utils;

import com.frankies.bootcamp.model.EmailAccess;
import com.frankies.bootcamp.service.DBService;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;

import jakarta.inject.Inject;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.apache.commons.codec.binary.Base64;

/* Class to demonstrate the use of Gmail Send Message API */
public class SendMessage {
    static EmailAccess email;
    @Inject
    private DBService db;
    /**
     * Send an email from the user's mailbox to its recipient.
     *
     * @return the sent message, {@code null} otherwise.
     * @throws MessagingException - if a wrongly formatted address is encountered.
     * @throws IOException        - if service account credentials file not found.
     */
    public Message sendEmail(MimeMessage message)
            throws MessagingException, IOException, SQLException {
        /* Load pre-authorized user credentials from the environment.
           TODO(developer) - See https://developers.google.com/identity for
            guides on implementing OAuth2 for your application.*/

        GoogleCredential credentials = new GoogleCredential().setAccessToken(getAccessToken());

        // Create the gmail API client
        Gmail service = new Gmail.Builder(new NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credentials)
                .setApplicationName("Gmail samples")
                .build();

        // Encode and wrap the MIME message into a gmail message
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        message.writeTo(buffer);
        byte[] rawMessageBytes = buffer.toByteArray();
        String encodedEmail = Base64.encodeBase64URLSafeString(rawMessageBytes);
        Message messageToSend = new Message();
        messageToSend.setRaw(encodedEmail);

        try {
            // Create send message
            messageToSend = service.users().messages().send("me", messageToSend).execute();
            System.out.println("Message id: " + messageToSend.getId());
            System.out.println(messageToSend.toPrettyString());
            return messageToSend;
        } catch (GoogleJsonResponseException e) {
            // TODO(developer) - handle error appropriately
            GoogleJsonError error = e.getDetails();
            if (error.getCode() == 403) {
                System.err.println("Unable to send message: " + e.getDetails());
            } else {
                throw e;
            }
        }
        return null;
    }

    private String refreshAccessToken(String refreshToken, String clientId, String clientSecret) throws IOException {
        try {
            TokenResponse response = new GoogleRefreshTokenRequest(
                    new NetHttpTransport(), new GsonFactory(),
                    refreshToken, clientId, clientSecret).execute();
            System.out.println("Access token: " + response.getAccessToken());
            return response.getAccessToken();
        } catch (TokenResponseException e) {
            if (e.getDetails() != null) {
                System.err.println("Error: " + e.getDetails().getError());
                if (e.getDetails().getErrorDescription() != null) {
                    System.err.println(e.getDetails().getErrorDescription());
                }
                if (e.getDetails().getErrorUri() != null) {
                    System.err.println(e.getDetails().getErrorUri());
                }
            } else {
                System.err.println(e.getMessage());
            }
        }
        return null;
    }

    private String getAccessToken() throws SQLException, IOException {
        if(email == null || email.getAccess_token() == null){
            email = db.getEmailAccess();
        }
        if(email != null && email.getLast_refresh() < System.currentTimeMillis()-3600000){
            email.setAccess_token(refreshAccessToken(email.getRefresh_token(), email.getClient_ID(), email.getClient_secret()));
            email.setLast_refresh(System.currentTimeMillis());
            db.updateEmail(email);
        }
        return email != null ? email.getAccess_token() : null;
    }
}