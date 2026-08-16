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
        @JsonPropertyDescription("Time of day, e.g., '09:00 AM' or 'Afternoon'")
        String time,

        @JsonPropertyDescription("Name of the location, attraction, or place")
        String location,

        @JsonPropertyDescription("Keep description concise, under 20 words")
        String description,

        @JsonPropertyDescription("Transit directions or how to get there, e.g., 'JR Nara Line to Inari Station (5 min walk)' or '10 min walk from central station'")
        String gettingThere,

        @JsonPropertyDescription("Operating / opening hours or best visiting window, e.g., '09:00 AM - 05:00 PM' or 'Open 24 hours'")
        String operatingHours
    ) {
        public Activity(String time, String location, String description) {
            this(time, location, description, null, null);
        }
    }
}
