package com.frankies.bootcamp.service;

import com.frankies.bootcamp.model.BootcampAthlete;
import com.frankies.bootcamp.model.strava.StravaActivityResponse;
import com.frankies.bootcamp.model.strava.StravaAuthResponse;
import com.frankies.bootcamp.model.strava.StravaRefreshResponse;
import com.frankies.bootcamp.utils.WildflyUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wildfly.security.credential.store.CredentialStoreException;

import java.io.IOException;
import java.lang.reflect.Type;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class StravaService {
    private final DBService db = new DBService();

    private static final Logger log = LoggerFactory.getLogger(StravaService.class);

    public boolean tokenExchange(String code) throws CredentialStoreException, NoSuchAlgorithmException, IOException, SQLException {
        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
        WildflyUtils wf = new WildflyUtils();

        RequestBody body = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("client_id", wf.giveMeAPass("stravaClientId"))
                .addFormDataPart("client_secret",wf.giveMeAPass("stravaClientSecret"))
                .addFormDataPart("code", code)
                .addFormDataPart("grant_type", "authorization_code")
                .build();
        Request request = new Request.Builder()
                .url("https://www.strava.com/api/v3/oauth/token")
                .method("POST", body)
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                StravaAuthResponse data = new Gson().fromJson(response.body().string(), StravaAuthResponse.class);
                db.saveAthlete(data.getBootcampAthlete());
            }
            return response.isSuccessful();
        }
    }

    public BootcampAthlete refreshToken(BootcampAthlete athlete) throws CredentialStoreException, NoSuchAlgorithmException, IOException, SQLException {
        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
        WildflyUtils wf = new WildflyUtils();
        log.info("StravaService, Refreshing token for athlete: " + athlete.getFirstname() + " " + athlete.getLastname());
        RequestBody body = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("client_id", wf.giveMeAPass("stravaClientId"))
                .addFormDataPart("client_secret",wf.giveMeAPass("stravaClientSecret"))
                .addFormDataPart("refresh_token", athlete.getRefreshToken())
                .addFormDataPart("grant_type", "refresh_token")
                .build();
        Request request = new Request.Builder()
                .url("https://www.strava.com/api/v3/oauth/token")
                .method("POST", body)
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                StravaRefreshResponse data = new Gson().fromJson(response.body().string(), StravaRefreshResponse.class);
                db.saveAthlete(data.getBootcampAthlete(athlete));
                athlete.setAccessToken(data.getAccess_token());
            }
        }
        return athlete;
    }

    public List<StravaActivityResponse> getAthleteActivitiesForPeriod(long after, String bearer) throws IOException {
        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
        List<StravaActivityResponse> stravaActivityResponses = new ArrayList<>();
        int page = 1;
        boolean hasMoreData = true;
        while (hasMoreData) {
            Request request = new Request.Builder()
                    .url("https://www.strava.com/api/v3/athlete/activities?after=" + after + "&per_page=100&page=" + page)
                    .method("GET", null)
                    .addHeader("Authorization", "Bearer " + bearer)
                    .build();
            List<StravaActivityResponse> outputList = List.of();
            try (Response response = client.newCall(request).execute()) {
                Type listOfMyClassObject = new TypeToken<ArrayList<StravaActivityResponse>>() {
                }.getType();
                if(response.isSuccessful()){
                    outputList = new Gson().fromJson(response.body().string(), listOfMyClassObject);
                    log.info("Page number: {}", page);
                }else{
                    log.error("StravaService, Something went wrong retrieving strava data. \nBody:{}\nResponse: {}", response.body().string(), response);
                }
            }catch (Exception e) {
                log.error("StravaService, Something went badly wrong processing strava data", e);
            }
            if (!outputList.isEmpty()) {
                stravaActivityResponses.addAll(outputList);
                page++;
                if (outputList.size() < 100) {
                    hasMoreData = false;
                }
            } else {
                hasMoreData = false;
            }
        }
        return stravaActivityResponses;
    }

    public StravaActivityResponse getActivityById(long activityId, String bearer, boolean includeAllEfforts) throws IOException {
        OkHttpClient client = new OkHttpClient.Builder().build();

        HttpUrl url = HttpUrl.parse("https://www.strava.com/api/v3/activities/" + activityId)
                .newBuilder()
                .addQueryParameter("include_all_efforts", String.valueOf(includeAllEfforts))
                .build();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Authorization", "Bearer " + bearer)
                .build();

        try (Response response = client.newCall(request).execute()) {
            int code = response.code();
            String body = response.body() != null ? response.body().string() : "";

            if (response.isSuccessful()) {
                // parse single activity
                return new Gson().fromJson(body, StravaActivityResponse.class);
            }

            if (code == 404) {
                // Activity might be deleted/hidden or wrong ID
                log.warn("StravaService: activity " + activityId + " not found. Response: " + response);
                return null;
            }

            // Helpful log for other failures (401 scopes, 403 visibility, 429 rate limit, etc.)
            log.error("StravaService: failed to fetch activity " + activityId + ". Code: " + code + " Body: " + body);
            throw new IOException("Strava activity fetch failed, HTTP " + code);
        } catch (IOException ioe) {
            log.error("StravaService: IO error fetching activity " + activityId, ioe);
            throw ioe;
        } catch (Exception e) {
            log.error("StravaService: unexpected error fetching activity " + activityId, e);
            throw new IOException(e);
        }
    }


}
