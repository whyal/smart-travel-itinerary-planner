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
        @JsonPropertyDescription("Clean venue or landmark name only, without labels or district prefixes, e.g. 'Osaka Castle'")
        String location,

        @JsonPropertyDescription("Transit directions from the PREVIOUS activity's location to this one, e.g. 'From Osaka Castle: 15 min by JR Loop Line to Tsuruhashi, then 5 min walk'")
        String gettingThere,

        @JsonPropertyDescription("Realistic door-to-door minutes from the previous activity's location to this one; 0 for the first activity of the day")
        Integer travelMinutesFromPrevious,

        @JsonPropertyDescription("Start time, no earlier than the previous activity's start + its durationMinutes + this travelMinutesFromPrevious, e.g. '01:30 PM'")
        String time,

        @JsonPropertyDescription("Realistic minutes spent at this activity, e.g. 120 for a large museum, 60 for lunch, 45 for hotel check-in")
        Integer durationMinutes,

        @JsonPropertyDescription("Concise, original synthesized itinerary description of what the traveler does here (15-25 words). Strictly do not copy verbatim text or metadata tags from reference documents.")
        String description,

        @JsonPropertyDescription("Visiting hours, e.g., '09:00 AM - 05:00 PM' or 'Open 24 hours'")
        String operatingHours,

        @JsonPropertyDescription("Activity type: SIGHTSEEING for attractions, DINING for meals, ARRIVAL for airport arrival, CHECK_IN for hotel check-in, CHECK_OUT for hotel check-out, DEPARTURE for airport departure")
        Category category
    ) {
        public Activity(String time, String location, String description) {
            this(location, null, null, time, null, description, null, null);
        }
    }

    public enum Category {
        SIGHTSEEING, DINING, ARRIVAL, CHECK_IN, CHECK_OUT, DEPARTURE
    }
}
