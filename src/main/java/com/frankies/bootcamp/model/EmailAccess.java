package com.frankies.bootcamp.model;

public class EmailAccess {
    private String client_ID;

    private String client_secret;

    private String access_token;

    private String refresh_token;

    private Long last_refresh;

    public String getClient_ID() {
        return client_ID;
    }

    public void setClient_ID(String client_ID) {
        this.client_ID = client_ID;
    }

    public String getClient_secret() {
        return client_secret;
    }

    public void setClient_secret(String client_secret) {
        this.client_secret = client_secret;
    }

    public String getAccess_token() {
        return access_token;
    }

    public void setAccess_token(String access_token) {
        this.access_token = access_token;
    }

    public String getRefresh_token() {
        return refresh_token;
    }

    public void setRefresh_token(String refresh_token) {
        this.refresh_token = refresh_token;
    }

    public Long getLast_refresh() {
        return last_refresh;
    }

    public void setLast_refresh(Long last_refresh) {
        this.last_refresh = last_refresh;
    }
}
