package com.frankies.bootcamp.model;

import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Double;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Object;
import java.lang.String;
import java.util.List;

public class StravaActivityResponse implements Serializable {
  private Integer comment_count;

  private Double max_heartrate;

  private Boolean has_kudoed;

  private String type;

  private Double average_heartrate;

  private List<Double> end_latlng;

  private String upload_id_str;

  private Long id;

  private Integer kudos_count;

  private String visibility;

  private Athlete athlete;

  private Integer athlete_count;

  private Integer resource_state;

  private Double max_speed;

  private Boolean from_accepted_tag;

  private List<Double> start_latlng;

  private Integer achievement_count;

  private String name;

  private Boolean commute;

  private Double utc_offset;

  private Double average_cadence;

  private Long upload_id;

  private Double distance;

  private String timezone;

  private String location_country;

  private Boolean has_heartrate;

  private String external_id;

  private Boolean isPrivate;

  private Object location_state;

  private Boolean manual;

  private String gear_id;

  private Double elev_low;

  private Boolean flagged;

  private Boolean trainer;

  private Object workout_type;

  private Object location_city;

  private Integer total_photo_count;

  private Integer elapsed_time;

  private Boolean heartrate_opt_out;

  private Boolean display_hide_heartrate_option;

  private Map map;

  private Double average_speed;

  private Integer average_temp;

  private Integer moving_time;

  private String sport_type;

  private String start_date;

  private Integer pr_count;

  private String start_date_local;

  private Double total_elevation_gain;

  private Integer photo_count;

  private Double elev_high;

  private Double suffer_score;

  public Integer getComment_count() {
    return this.comment_count;
  }

  public void setComment_count(Integer comment_count) {
    this.comment_count = comment_count;
  }

  public Double getMax_heartrate() {
    return this.max_heartrate;
  }

  public void setMax_heartrate(Double max_heartrate) {
    this.max_heartrate = max_heartrate;
  }

  public Boolean getHas_kudoed() {
    return this.has_kudoed;
  }

  public void setHas_kudoed(Boolean has_kudoed) {
    this.has_kudoed = has_kudoed;
  }

  public String getType() {
    return this.type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public Double getAverage_heartrate() {
    return this.average_heartrate;
  }

  public void setAverage_heartrate(Double average_heartrate) {
    this.average_heartrate = average_heartrate;
  }

  public List<Double> getEnd_latlng() {
    return this.end_latlng;
  }

  public void setEnd_latlng(List<Double> end_latlng) {
    this.end_latlng = end_latlng;
  }

  public String getUpload_id_str() {
    return this.upload_id_str;
  }

  public void setUpload_id_str(String upload_id_str) {
    this.upload_id_str = upload_id_str;
  }

  public Long getId() {
    return this.id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Integer getKudos_count() {
    return this.kudos_count;
  }

  public void setKudos_count(Integer kudos_count) {
    this.kudos_count = kudos_count;
  }

  public String getVisibility() {
    return this.visibility;
  }

  public void setVisibility(String visibility) {
    this.visibility = visibility;
  }

  public Athlete getAthlete() {
    return this.athlete;
  }

  public void setAthlete(Athlete athlete) {
    this.athlete = athlete;
  }

  public Integer getAthlete_count() {
    return this.athlete_count;
  }

  public void setAthlete_count(Integer athlete_count) {
    this.athlete_count = athlete_count;
  }

  public Integer getResource_state() {
    return this.resource_state;
  }

  public void setResource_state(Integer resource_state) {
    this.resource_state = resource_state;
  }

  public Double getMax_speed() {
    return this.max_speed;
  }

  public void setMax_speed(Double max_speed) {
    this.max_speed = max_speed;
  }

  public Boolean getFrom_accepted_tag() {
    return this.from_accepted_tag;
  }

  public void setFrom_accepted_tag(Boolean from_accepted_tag) {
    this.from_accepted_tag = from_accepted_tag;
  }

  public List<Double> getStart_latlng() {
    return this.start_latlng;
  }

  public void setStart_latlng(List<Double> start_latlng) {
    this.start_latlng = start_latlng;
  }

  public Integer getAchievement_count() {
    return this.achievement_count;
  }

  public void setAchievement_count(Integer achievement_count) {
    this.achievement_count = achievement_count;
  }

  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Boolean getCommute() {
    return this.commute;
  }

  public void setCommute(Boolean commute) {
    this.commute = commute;
  }

  public Double getUtc_offset() {
    return this.utc_offset;
  }

  public void setUtc_offset(Double utc_offset) {
    this.utc_offset = utc_offset;
  }

  public Double getAverage_cadence() {
    return this.average_cadence;
  }

  public void setAverage_cadence(Double average_cadence) {
    this.average_cadence = average_cadence;
  }

  public Long getUpload_id() {
    return this.upload_id;
  }

  public void setUpload_id(Long upload_id) {
    this.upload_id = upload_id;
  }

  public Double getDistance() {
    return this.distance;
  }

  public void setDistance(Double distance) {
    this.distance = distance;
  }

  public String getTimezone() {
    return this.timezone;
  }

  public void setTimezone(String timezone) {
    this.timezone = timezone;
  }

  public String getLocation_country() {
    return this.location_country;
  }

  public void setLocation_country(String location_country) {
    this.location_country = location_country;
  }

  public Boolean getHas_heartrate() {
    return this.has_heartrate;
  }

  public void setHas_heartrate(Boolean has_heartrate) {
    this.has_heartrate = has_heartrate;
  }

  public String getExternal_id() {
    return this.external_id;
  }

  public void setExternal_id(String external_id) {
    this.external_id = external_id;
  }

  public Boolean getIsPrivate() {
    return this.isPrivate;
  }

  public void setIsPrivate(Boolean isPrivate) {
    this.isPrivate = isPrivate;
  }

  public Object getLocation_state() {
    return this.location_state;
  }

  public void setLocation_state(Object location_state) {
    this.location_state = location_state;
  }

  public Boolean getManual() {
    return this.manual;
  }

  public void setManual(Boolean manual) {
    this.manual = manual;
  }

  public String getGear_id() {
    return this.gear_id;
  }

  public void setGear_id(String gear_id) {
    this.gear_id = gear_id;
  }

  public Double getElev_low() {
    return this.elev_low;
  }

  public void setElev_low(Double elev_low) {
    this.elev_low = elev_low;
  }

  public Boolean getFlagged() {
    return this.flagged;
  }

  public void setFlagged(Boolean flagged) {
    this.flagged = flagged;
  }

  public Boolean getTrainer() {
    return this.trainer;
  }

  public void setTrainer(Boolean trainer) {
    this.trainer = trainer;
  }

  public Object getWorkout_type() {
    return this.workout_type;
  }

  public void setWorkout_type(Object workout_type) {
    this.workout_type = workout_type;
  }

  public Object getLocation_city() {
    return this.location_city;
  }

  public void setLocation_city(Object location_city) {
    this.location_city = location_city;
  }

  public Integer getTotal_photo_count() {
    return this.total_photo_count;
  }

  public void setTotal_photo_count(Integer total_photo_count) {
    this.total_photo_count = total_photo_count;
  }

  public Integer getElapsed_time() {
    return this.elapsed_time;
  }

  public void setElapsed_time(Integer elapsed_time) {
    this.elapsed_time = elapsed_time;
  }

  public Boolean getHeartrate_opt_out() {
    return this.heartrate_opt_out;
  }

  public void setHeartrate_opt_out(Boolean heartrate_opt_out) {
    this.heartrate_opt_out = heartrate_opt_out;
  }

  public Boolean getDisplay_hide_heartrate_option() {
    return this.display_hide_heartrate_option;
  }

  public void setDisplay_hide_heartrate_option(Boolean display_hide_heartrate_option) {
    this.display_hide_heartrate_option = display_hide_heartrate_option;
  }

  public Map getMap() {
    return this.map;
  }

  public void setMap(Map map) {
    this.map = map;
  }

  public Double getAverage_speed() {
    return this.average_speed;
  }

  public void setAverage_speed(Double average_speed) {
    this.average_speed = average_speed;
  }

  public Integer getAverage_temp() {
    return this.average_temp;
  }

  public void setAverage_temp(Integer average_temp) {
    this.average_temp = average_temp;
  }

  public Integer getMoving_time() {
    return this.moving_time;
  }

  public void setMoving_time(Integer moving_time) {
    this.moving_time = moving_time;
  }

  public String getSport_type() {
    return this.sport_type;
  }

  public void setSport_type(String sport_type) {
    this.sport_type = sport_type;
  }

  public String getStart_date() {
    return this.start_date;
  }

  public void setStart_date(String start_date) {
    this.start_date = start_date;
  }

  public Integer getPr_count() {
    return this.pr_count;
  }

  public void setPr_count(Integer pr_count) {
    this.pr_count = pr_count;
  }

  public String getStart_date_local() {
    return this.start_date_local;
  }

  public void setStart_date_local(String start_date_local) {
    this.start_date_local = start_date_local;
  }

  public Double getTotal_elevation_gain() {
    return this.total_elevation_gain;
  }

  public void setTotal_elevation_gain(Double total_elevation_gain) {
    this.total_elevation_gain = total_elevation_gain;
  }

  public Integer getPhoto_count() {
    return this.photo_count;
  }

  public void setPhoto_count(Integer photo_count) {
    this.photo_count = photo_count;
  }

  public Double getElev_high() {
    return this.elev_high;
  }

  public void setElev_high(Double elev_high) {
    this.elev_high = elev_high;
  }

  public Double getSuffer_score() {
    return this.suffer_score;
  }

  public void setSuffer_score(Double suffer_score) {
    this.suffer_score = suffer_score;
  }

  public static class Athlete implements Serializable {
    private Integer resource_state;

    private Integer id;

    public Integer getResource_state() {
      return this.resource_state;
    }

    public void setResource_state(Integer resource_state) {
      this.resource_state = resource_state;
    }

    public Integer getId() {
      return this.id;
    }

    public void setId(Integer id) {
      this.id = id;
    }
  }

  public static class Map implements Serializable {
    private String summary_polyline;

    private Integer resource_state;

    private String id;

    public String getSummary_polyline() {
      return this.summary_polyline;
    }

    public void setSummary_polyline(String summary_polyline) {
      this.summary_polyline = summary_polyline;
    }

    public Integer getResource_state() {
      return this.resource_state;
    }

    public void setResource_state(Integer resource_state) {
      this.resource_state = resource_state;
    }

    public String getId() {
      return this.id;
    }

    public void setId(String id) {
      this.id = id;
    }
  }
}
