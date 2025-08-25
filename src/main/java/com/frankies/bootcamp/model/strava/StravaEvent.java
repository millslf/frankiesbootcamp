package com.frankies.bootcamp.model.strava;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class StravaEvent {
    public String objectType;         // maps from "object_type"
    public Long objectId;             // "object_id"
    public String aspectType;         // "aspect_type"
    public Long ownerId;              // "owner_id"
    public Long subscriptionId;    // "subscription_id"
    public Long eventTime;            // "event_time"
    public Map<String,String> updates;

    @JsonIgnore
    public boolean isHidden() {
        if (updates == null) return false;
        String priv = updates.get("private");
        String vis  = updates.get("visibility");
        return "true".equalsIgnoreCase(priv) || "only_me".equalsIgnoreCase(vis);
    }
}

