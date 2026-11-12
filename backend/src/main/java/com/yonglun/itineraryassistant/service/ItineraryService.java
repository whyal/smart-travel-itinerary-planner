package com.yonglun.itineraryassistant.service;

import com.yonglun.itineraryassistant.model.Itinerary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ItineraryService {

    private static final Logger log = LoggerFactory.getLogger(ItineraryService.class);

    private final ChatClient chatClient;
    private final BeanOutputConverter<Itinerary> outputConverter = new BeanOutputConverter<>(Itinerary.class);

    public ItineraryService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public Itinerary generateItinerary(String prompt, String conversationId) {
        Itinerary raw = chatClient.prompt()
                .user(prompt)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, resolveSessionId(conversationId)))
                .call()                .entity(Itinerary.class, spec -> spec
                        .useProviderStructuredOutput()
                        .validateSchema()
                );

        return sanitizeItinerary(raw);
    }

    public Itinerary sanitizeItinerary(Itinerary raw) {
        if (raw == null || raw.days() == null || raw.days().isEmpty()) {
            return raw;
        }

        String destinationName = cleanString(raw.destination());
        Set<String> seenLocations = new HashSet<>();
        List<Itinerary.DayPlan> sanitizedDays = new ArrayList<>();

        for (Itinerary.DayPlan day : raw.days()) {
            if (day == null || day.activities() == null) {
                sanitizedDays.add(day);
                continue;
            }

            int dayNum = day.dayNumber() > 0 ? day.dayNumber() : (sanitizedDays.size() + 1);
            String theme = cleanTheme(day.theme(), dayNum);

            List<Itinerary.Activity> uniqueActivities = new ArrayList<>();
            for (Itinerary.Activity activity : day.activities()) {
                if (activity == null) continue;

                String loc = cleanLocation(activity.location());
                if (loc.isBlank() && !destinationName.isBlank()) {
                    loc = destinationName;
                }
                String locationKey = loc.toLowerCase();
                boolean isGenericDestination = !destinationName.isEmpty() && locationKey.equals(destinationName.toLowerCase());
                if (!locationKey.isEmpty() && !isGenericDestination && seenLocations.contains(locationKey)) {
                    log.warn("Defensive filter: Removed duplicate activity location '{}' on Day {}", loc, dayNum);
                    continue;
                }
                if (!locationKey.isEmpty() && !isGenericDestination) {
                    seenLocations.add(locationKey);
                }

                String time = cleanTime(activity.time(), uniqueActivities.size());
                String desc = cleanDescription(activity.description(), loc);
                String gettingThere = cleanTransit(activity.gettingThere(), destinationName);
                String operatingHours = cleanHours(activity.operatingHours());

                uniqueActivities.add(new Itinerary.Activity(time, loc, desc, gettingThere, operatingHours));
            }

            sanitizedDays.add(new Itinerary.DayPlan(dayNum, theme, uniqueActivities));
        }

        return new Itinerary(destinationName, sanitizedDays);
    }

    public Flux<String> streamItinerary(String prompt, String conversationId) {
        String systemPrompt = "You are an expert travel data API. Output ONLY raw, RFC8259-compliant JSON without markdown code blocks (```json) or introductory text.\n\n" +
                "Core Rules:\n" +
                "- Day Count Invariant: The 'days' array MUST contain EXACTLY the number of days requested in the user prompt (numbered sequentially 1 to N). Never collapse or truncate days.\n" +
                "- Activities Requirement: Every day MUST include 2 to 4 activities. Never leave 'activities' empty.\n" +
                "- Supporting Knowledge Synthesis (NEVER Copy Verbatim): Retrieved context documents are supporting reference material ONLY. DO NOT lazily copy or echo identical sentences, paragraphs, or document metadata tags (e.g. 'Destination:', 'Title:', 'District:', 'Category:'). Synthesize facts into a fresh, original travel plan from a traveler's perspective.\n" +
                "- Consistent UI Activity Fields: For EVERY activity, you MUST provide all 5 fields with clean formatting for the UI:\n" +
                "  1. 'time': Specific scheduled time or window (e.g. '09:00 AM', '01:30 PM', '06:00 PM').\n" +
                "  2. 'location': Clean place/venue name only (e.g. 'Osaka Castle'), strictly without prefixes or metadata.\n" +
                "  3. 'description': Concise (15 to 25 words) synthesized action plan of what the traveler experiences. Never copy identical text from context documents.\n" +
                "  4. 'gettingThere': Specific, realistic transit directions (e.g. '5 min walk from Station Exit 2').\n" +
                "  5. 'operatingHours': Specific operating hours or window (e.g. '09:00 AM - 05:00 PM' or 'Open 24 hours').\n" +
                "- Location Uniqueness: Every attraction, museum, restaurant, landmark, or specific venue MUST appear at most once across the entire multi-day itinerary.\n" +
                "- Geographic Flow: Cluster activities by district or neighborhood per day to eliminate unnecessary commuting and backtracking.\n" +
                "- Realistic Pacing: Plan 2 to 4 major activities per day with practical 'gettingThere' transit details and accurate 'operatingHours'.\n" +
                "- JSON Whitespace & Formatting: Output clean, compact RFC8259 JSON without unnecessary spaces, excessive indentation, blank lines, or trailing whitespace. In all string values, use normal single-space separation between words with no raw newlines or tabs.\n\n" +
                outputConverter.getFormat();

        return chatClient.prompt()
                .system(systemPrompt)
                .user(prompt)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, resolveSessionId(conversationId)))
                .stream()
                .content();
    }

    private String cleanString(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\r", " ")
                .replace("\n", " ")
                .replace("\t", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String cleanTheme(String theme, int dayNumber) {
        String cleaned = cleanString(theme);
        cleaned = cleaned.replaceFirst("^(?i)(?:day\\s*\\d+[:\\s-]*|theme[:\\s-]*)", "").trim();
        return cleaned.isEmpty() ? ("Day " + dayNumber + " Highlights") : cleaned;
    }

    private String cleanLocation(String location) {
        String cleaned = cleanString(location);
        cleaned = cleaned.replaceFirst("^(?i)(?:title|location|venue|attraction|place|destination|district):\\s*", "").trim();
        cleaned = cleaned.replaceAll("(?i)\\s*\\(district:[^)]*\\)", "").trim();
        return cleaned;
    }

    private String cleanDescription(String description, String fallbackLocation) {
        if (description == null) {
            return "Explore and experience " + (fallbackLocation != null && !fallbackLocation.isBlank() ? fallbackLocation : "this destination") + ".";
        }
        String stripped = description.replaceAll("(?im)^(?:Destination|Title|District|Category|Best Time to Visit|Suggested Duration|Source):.*$", "");
        stripped = stripped.replaceAll("(?i)\\b(?:Destination|Title|District|Category|Best Time to Visit|Suggested Duration|Source):\\s*[^.:\\n]+(?:\\.|\\n|$|(?=\\s*\\b(?:Destination|Title|District|Category|Best Time to Visit|Suggested Duration|Source):))", "");
        stripped = stripped.replaceFirst("^(?i)(?:destination|title|district|category|best time to visit|suggested duration|source|description):\\s*", "");
        String cleaned = cleanString(stripped);
        if (cleaned.isEmpty()) {
            return "Explore and experience " + (fallbackLocation != null && !fallbackLocation.isBlank() ? fallbackLocation : "this destination") + ".";
        }
        return cleaned;
    }

    private String cleanTransit(String transit, String destination) {
        String cleaned = cleanString(transit);
        cleaned = cleaned.replaceFirst("^(?i)(?:getting there|transit|transport|how to get there):\\s*", "").trim();
        return cleaned.isEmpty() ? "Accessible via local transit or walking" : cleaned;
    }

    private String cleanHours(String hours) {
        String cleaned = cleanString(hours);
        cleaned = cleaned.replaceFirst("^(?i)(?:operating hours|opening hours|hours):\\s*", "").trim();
        return cleaned.isEmpty() ? "Flexible / General visiting hours" : cleaned;
    }

    private String cleanTime(String time, int index) {
        String cleaned = cleanString(time);
        cleaned = cleaned.replaceFirst("^(?i)(?:time|time of day):\\s*", "").trim();
        if (cleaned.isEmpty() || cleaned.equalsIgnoreCase("flexible")) {
            return switch (index) {
                case 0 -> "09:00 AM";
                case 1 -> "01:00 PM";
                case 2 -> "04:30 PM";
                case 3 -> "07:30 PM";
                default -> "Flexible";
            };
        }
        return cleaned;
    }

    private String resolveSessionId(String conversationId) {
        return (conversationId != null && !conversationId.isBlank())
                ? conversationId
                : "default-itinerary-session";
    }
}

