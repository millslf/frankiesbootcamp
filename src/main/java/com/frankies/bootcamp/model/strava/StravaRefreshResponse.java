package com.frankies.bootcamp.model.strava;

import com.frankies.bootcamp.model.BootcampAthlete;

import java.io.Serializable;

public class StravaRefreshResponse implements Serializable {
    private String access_token;

    private String refresh_token;

    private Long expires_at;

    private String token_type;

    private Integer expires_in;

    public String getAccess_token() {
        return this.access_token;
    }

    public void setAccess_token(String access_token) {
        this.access_token = access_token;
    }

    public String getRefresh_token() {
        return this.refresh_token;
    }

    public void setRefresh_token(String refresh_token) {
        this.refresh_token = refresh_token;
    }

    public Long getExpires_at() {
        return this.expires_at;
    }

    public void setExpires_at(Long expires_at) {
        this.expires_at = expires_at;
    }

    public String getToken_type() {
        return this.token_type;
    }

    public void setToken_type(String token_type) {
        this.token_type = token_type;
    }

    public Integer getExpires_in() {
        return this.expires_in;
    }

    public void setExpires_in(Integer expires_in) {
        this.expires_in = expires_in;
    }

    public static class Athlete implements Serializable {
        private String country;

        private String profile_medium;

        private String firstname;

        private Object follower;

        private String city;

        private Integer resource_state;

        private String sex;

        private String profile;

        private String bio;

        private String created_at;

        private Double weight;

        private Boolean summit;

        private String lastname;

        private Boolean premium;

        private String updated_at;

        private Integer badge_type_id;

        private Object friend;

        private String id;

        private String state;

        private Object username;

        public String getCountry() {
            return this.country;
        }

        public void setCountry(String country) {
            this.country = country;
        }

        public String getProfile_medium() {
            return this.profile_medium;
        }

        public void setProfile_medium(String profile_medium) {
            this.profile_medium = profile_medium;
        }

        public String getFirstname() {
            return this.firstname;
        }

        public void setFirstname(String firstname) {
            this.firstname = firstname;
        }

        public Object getFollower() {
            return this.follower;
        }

        public void setFollower(Object follower) {
            this.follower = follower;
        }

        public String getCity() {
            return this.city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public Integer getResource_state() {
            return this.resource_state;
        }

        public void setResource_state(Integer resource_state) {
            this.resource_state = resource_state;
        }

        public String getSex() {
            return this.sex;
        }

        public void setSex(String sex) {
            this.sex = sex;
        }

        public String getProfile() {
            return this.profile;
        }

        public void setProfile(String profile) {
            this.profile = profile;
        }

        public String getBio() {
            return this.bio;
        }

        public void setBio(String bio) {
            this.bio = bio;
        }

        public String getCreated_at() {
            return this.created_at;
        }

        public void setCreated_at(String created_at) {
            this.created_at = created_at;
        }

        public Double getWeight() {
            return this.weight;
        }

        public void setWeight(Double weight) {
            this.weight = weight;
        }

        public Boolean getSummit() {
            return this.summit;
        }

        public void setSummit(Boolean summit) {
            this.summit = summit;
        }

        public String getLastname() {
            return this.lastname;
        }

        public void setLastname(String lastname) {
            this.lastname = lastname;
        }

        public Boolean getPremium() {
            return this.premium;
        }

        public void setPremium(Boolean premium) {
            this.premium = premium;
        }

        public String getUpdated_at() {
            return this.updated_at;
        }

        public void setUpdated_at(String updated_at) {
            this.updated_at = updated_at;
        }

        public Integer getBadge_type_id() {
            return this.badge_type_id;
        }

        public void setBadge_type_id(Integer badge_type_id) {
            this.badge_type_id = badge_type_id;
        }

        public Object getFriend() {
            return this.friend;
        }

        public void setFriend(Object friend) {
            this.friend = friend;
        }

        public String getId() {
            return this.id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getState() {
            return this.state;
        }

        public void setState(String state) {
            this.state = state;
        }

        public Object getUsername() {
            return this.username;
        }

        public void setUsername(Object username) {
            this.username = username;
        }
    }

    public BootcampAthlete getBootcampAthlete(BootcampAthlete currentAthlete){
        BootcampAthlete bootcampAthlete = new BootcampAthlete();
        bootcampAthlete.setFirstname(currentAthlete.getFirstname());
        bootcampAthlete.setLastname(currentAthlete.getLastname());
        bootcampAthlete.setExpiresIn(this.expires_in);
        bootcampAthlete.setAccessToken(this.access_token);
        bootcampAthlete.setRefreshToken(this.refresh_token);
        bootcampAthlete.setTokenType(this.token_type);
        bootcampAthlete.setExpiresAt(this.expires_at);
        bootcampAthlete.setId(currentAthlete.getId());
        return bootcampAthlete;
    }
}
