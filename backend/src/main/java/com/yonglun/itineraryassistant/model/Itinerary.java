package com.yonglun.itineraryassistant.model;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.List;

public record Itinerary(
    String destination,
    List<DayPlan> days
) {
    public record DayPlan(
        int dayNumber,
        String theme,
        List<Activity> activities
    ) {}

    public record Activity(
        @JsonPropertyDescription("Time of day, e.g., '09:00 AM'")
        String time,

        @JsonPropertyDescription("Name of the location or place")
        String location,

        @JsonPropertyDescription("Keep description under 15 words")
        String description
    ) {}
}
