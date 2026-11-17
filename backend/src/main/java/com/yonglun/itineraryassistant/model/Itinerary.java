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
        @JsonPropertyDescription("Chronological time or window, e.g., '09:00 AM' or '01:30 PM'")
        String time,

        @JsonPropertyDescription("Clean venue or landmark name only, without labels or district prefixes, e.g. 'Osaka Castle'")
        String location,

        @JsonPropertyDescription("Concise, original synthesized itinerary description of what the traveler does here (15-25 words). Strictly do not copy verbatim text or metadata tags from reference documents.")
        String description,

        @JsonPropertyDescription("Practical transit directions or walking distance, e.g., '5 min walk from Osakajokoen Station Exit 1'")
        String gettingThere,

        @JsonPropertyDescription("Visiting hours, e.g., '09:00 AM - 05:00 PM' or 'Open 24 hours'")
        String operatingHours,

        @JsonPropertyDescription("Activity type: SIGHTSEEING for attractions, DINING for meals, ARRIVAL for airport arrival, CHECK_IN for hotel check-in, CHECK_OUT for hotel check-out, DEPARTURE for airport departure")
        Category category
    ) {
        public Activity(String time, String location, String description) {
            this(time, location, description, null, null, null);
        }
    }

    public enum Category {
        SIGHTSEEING, DINING, ARRIVAL, CHECK_IN, CHECK_OUT, DEPARTURE
    }
}
