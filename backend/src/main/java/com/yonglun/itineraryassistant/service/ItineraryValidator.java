package com.yonglun.itineraryassistant.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yonglun.itineraryassistant.model.Itinerary.Category;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ItineraryValidator {

    private static final Pattern DAYS_LINE = Pattern.compile("(?m)^Days:\\s*(\\d+)");
    private static final List<String> REQUIRED_FIELDS = List.of("time", "location", "description", "gettingThere", "operatingHours", "durationMinutes", "travelMinutesFromPrevious");
    private static final Pattern CLOCK = Pattern.compile("(\\d{1,2}):(\\d{2})\\s*([AaPp][Mm])?");
    private static final int TIMING_TOLERANCE_MINUTES = 10;

    private final ObjectMapper objectMapper;

    public ItineraryValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public static Integer expectedDays(String prompt) {
        Matcher m = DAYS_LINE.matcher(prompt);
        return m.find() ? Integer.valueOf(m.group(1)) : null;
    }

    public List<String> violations(String output, Integer expectedDays) {
        List<String> problems = new ArrayList<>();
        JsonNode days;
        try {
            String clean = output.trim().replaceAll("^```(?:json)?\\s*", "").replaceAll("\\s*```$", "");
            days = objectMapper.readTree(clean).path("days");
        } catch (Exception e) {
            return List.of("Output is not valid JSON. Return only the raw JSON object.");
        }
        if (!days.isArray() || days.isEmpty()) {
            return List.of("The 'days' array is missing or empty.");
        }
        if (expectedDays != null && days.size() != expectedDays) {
            problems.add("The 'days' array has " + days.size() + " days but exactly " + expectedDays + " were requested."
                    + (days.size() < expectedDays
                    ? " Add " + (days.size() + 1 == expectedDays ? "day " + expectedDays : "days " + (days.size() + 1) + " to " + expectedDays) + " with new venues, and move CHECK_OUT and DEPARTURE to day " + expectedDays + "."
                    : " Remove every day after day " + expectedDays + " and end day " + expectedDays + " with CHECK_OUT and DEPARTURE."));
        }

        Set<String> seenVenues = new HashSet<>();
        for (int d = 0; d < days.size(); d++) {
            int dayNo = d + 1;
            JsonNode activities = days.get(d).path("activities");
            if (!activities.isArray() || activities.isEmpty()) {
                problems.add("Day " + dayNo + " has no activities.");
                continue;
            }
            problems.addAll(timingViolations(dayNo, activities));
            for (JsonNode act : activities) {
                String location = act.path("location").asText("").trim();
                for (String field : REQUIRED_FIELDS) {
                    if (act.path(field).asText("").isBlank()) {
                        problems.add("Day " + dayNo + " activity '" + location + "' is missing '" + field + "'.");
                    }
                }
                String category = act.path("category").asText("");
                if (Arrays.stream(Category.values()).noneMatch(c -> c.name().equals(category))) {
                    problems.add("Day " + dayNo + " activity '" + location + "' has invalid category '" + category + "'.");
                } else if ((category.equals("SIGHTSEEING") || category.equals("DINING"))
                        && !location.isEmpty() && !seenVenues.add(location.toLowerCase())) {
                    problems.add("Day " + dayNo + " repeats venue '" + location + "', which already appears earlier in the trip.");
                }
            }
        }

        if (!"ARRIVAL".equals(firstCategory(days.get(0)))) {
            problems.add("Day 1 must begin with an ARRIVAL activity.");
        }
        if (!"DEPARTURE".equals(lastCategory(days.get(days.size() - 1)))) {
            problems.add("The last day must end with a DEPARTURE activity.");
        }
        return problems;
    }

    // shortcut: activities with an unparseable time are skipped, and past-midnight schedules are not handled; upgrade if the model starts emitting either.
    private static List<String> timingViolations(int dayNo, JsonNode activities) {
        List<String> problems = new ArrayList<>();
        JsonNode prev = null;
        Integer prevStart = null;
        for (JsonNode act : activities) {
            Integer start = minutesOfDay(act.path("time").asText(""));
            if (prev != null && prevStart != null && start != null
                    && prev.path("durationMinutes").isInt() && act.path("travelMinutesFromPrevious").isInt()) {
                int earliest = prevStart + prev.path("durationMinutes").asInt() + act.path("travelMinutesFromPrevious").asInt();
                if (start < earliest - TIMING_TOLERANCE_MINUTES) {
                    problems.add("Day " + dayNo + " activity '" + act.path("location").asText() + "' starts at " + act.path("time").asText()
                            + ", but '" + prev.path("location").asText() + "' ends at " + clock(prevStart + prev.path("durationMinutes").asInt())
                            + " and travel takes " + act.path("travelMinutesFromPrevious").asInt() + " min, so it cannot start before " + clock(earliest) + ".");
                }
            }
            prev = act;
            prevStart = start;
        }
        return problems;
    }

    static Integer minutesOfDay(String time) {
        Matcher m = CLOCK.matcher(time);
        if (!m.find()) {
            return null;
        }
        int hour = Integer.parseInt(m.group(1)) % 12;
        if (m.group(3) == null) {
            hour = Integer.parseInt(m.group(1));
        } else if (m.group(3).equalsIgnoreCase("PM")) {
            hour += 12;
        }
        return hour * 60 + Integer.parseInt(m.group(2));
    }

    private static String clock(int minutes) {
        int h = (minutes / 60) % 24;
        return String.format("%02d:%02d %s", h % 12 == 0 ? 12 : h % 12, minutes % 60, h < 12 ? "AM" : "PM");
    }

    private static String firstCategory(JsonNode day) {
        JsonNode acts = day.path("activities");
        return acts.isEmpty() ? null : acts.get(0).path("category").asText();
    }

    private static String lastCategory(JsonNode day) {
        JsonNode acts = day.path("activities");
        return acts.isEmpty() ? null : acts.get(acts.size() - 1).path("category").asText();
    }
}
