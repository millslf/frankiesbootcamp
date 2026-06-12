package com.frankies.bootcamp.model;

public class CompetitionSummaryView {
    private final long id;
    private final String name;
    private final String timezone;
    private final long startTimestamp;
    private final Long endTimestamp;
    private final String status;

    public CompetitionSummaryView(long id, String name, String timezone, long startTimestamp, Long endTimestamp, String status) {
        this.id = id;
        this.name = name;
        this.timezone = timezone;
        this.startTimestamp = startTimestamp;
        this.endTimestamp = endTimestamp;
        this.status = status;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getTimezone() {
        return timezone;
    }

    public long getStartTimestamp() {
        return startTimestamp;
    }

    public Long getEndTimestamp() {
        return endTimestamp;
    }

    public String getStatus() {
        return status;
    }
}
