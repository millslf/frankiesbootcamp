package com.frankies.bootcamp.service;

import com.frankies.bootcamp.model.BootcampAthlete;
import com.frankies.bootcamp.model.StravaActivityResponse;
import com.frankies.bootcamp.model.StravaAuthResponse;
import com.frankies.bootcamp.model.StravaRefreshResponse;
import com.frankies.bootcamp.utils.WildflyUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import okhttp3.*;
import org.wildfly.security.credential.store.CredentialStoreException;

import java.io.IOException;
import java.lang.reflect.Type;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class StravaService {

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
                //Rather inject !!!!!
                DBService db = new DBService();
                db.saveAthlete(data.getBootcampAthlete());
            }
            return response.isSuccessful();
        }
    }

    public BootcampAthlete refreshToken(BootcampAthlete athlete) throws CredentialStoreException, NoSuchAlgorithmException, IOException, SQLException {
        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
        WildflyUtils wf = new WildflyUtils();
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
                //Rather inject !!!!!
                DBService db = new DBService();
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
            List<StravaActivityResponse> outputList;
            try (Response response = client.newCall(request).execute()) {
                Type listOfMyClassObject = new TypeToken<ArrayList<StravaActivityResponse>>() {
                }.getType();

                outputList = new Gson().fromJson(response.body().string(), listOfMyClassObject);
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

}
