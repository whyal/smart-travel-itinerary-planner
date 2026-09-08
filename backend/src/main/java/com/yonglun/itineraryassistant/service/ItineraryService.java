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
                .call()
                // Force Gemini's native API JSON mode so it never outputs conversational text
                .entity(Itinerary.class, spec -> spec
                        .useProviderStructuredOutput()
                        .validateSchema()
                );

        return sanitizeItinerary(raw);
    }

    public Itinerary sanitizeItinerary(Itinerary raw) {
        if (raw == null || raw.days() == null || raw.days().isEmpty()) {
            return raw;
        }

        Set<String> seenLocations = new HashSet<>();
        List<Itinerary.DayPlan> sanitizedDays = new ArrayList<>();

        for (Itinerary.DayPlan day : raw.days()) {
            if (day == null || day.activities() == null) {
                sanitizedDays.add(day);
                continue;
            }

            List<Itinerary.Activity> uniqueActivities = new ArrayList<>();
            for (Itinerary.Activity activity : day.activities()) {
                if (activity == null) continue;
                String locationKey = (activity.location() != null) ? activity.location().trim().toLowerCase() : "";
                if (!locationKey.isEmpty() && seenLocations.contains(locationKey)) {
                    log.warn("Defensive filter: Removed duplicate activity location '{}' on Day {}", activity.location(), day.dayNumber());
                    continue;
                }
                if (!locationKey.isEmpty()) {
                    seenLocations.add(locationKey);
                }
                uniqueActivities.add(activity);
            }

            sanitizedDays.add(new Itinerary.DayPlan(day.dayNumber(), day.theme(), uniqueActivities));
        }

        return new Itinerary(raw.destination(), sanitizedDays);
    }

    public Flux<String> streamItinerary(String prompt, String conversationId) {
        return chatClient.prompt()
                // Add a strict system rule to prevent markdown fences and reinforce anti-duplication during SSE streaming
                .system("You are a data API. Output ONLY raw, RFC8259-compliant JSON without markdown code blocks (```json) or introductory text. " +
                        "Ensure every visited location/attraction is unique across the entire multi-day itinerary (zero duplicate venues across days), " +
                        "cluster activities by district each day, and limit activities to 2 to 4 per day.")
                .user(userSpec -> userSpec
                        .text(prompt + "\n\n{format}")
                        .param("format", outputConverter.getFormat())
                )
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, resolveSessionId(conversationId)))
                .stream()
                .content();
    }

    private String resolveSessionId(String conversationId) {
        return (conversationId != null && !conversationId.isBlank())
                ? conversationId
                : "default-itinerary-session";
    }
}

