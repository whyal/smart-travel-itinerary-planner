package com.yonglun.itineraryassistant.service;

import com.yonglun.itineraryassistant.model.Itinerary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

@Service
public class ItineraryService {

    private static final String SYSTEM_RULES = "You are an expert travel data API. Output ONLY raw, RFC8259-compliant JSON without markdown code blocks (```json) or introductory text.\n\n" +
            "Core Rules:\n" +
            "- Day Count Invariant: The 'days' array MUST contain EXACTLY the number of days requested in the user prompt (numbered sequentially 1 to N). Never collapse or truncate days.\n" +
            "- Activities Requirement: Every day MUST include 2 to 4 activities with category SIGHTSEEING or DINING (1 to 3 on the first and last day). Logistics entries (ARRIVAL, CHECK_IN, CHECK_OUT, DEPARTURE) are additional and do not count toward this limit. Never leave 'activities' empty.\n" +
            "- Travel Logistics: Day 1 MUST begin with one ARRIVAL activity at the destination's main airport, followed by one CHECK_IN activity at the hotel, before any sightseeing. The last day MUST begin with one CHECK_OUT activity at the hotel and end with one DEPARTURE activity at the airport. Never place sightseeing after DEPARTURE. Use the hotel and flight details from 'Additional notes' when given; otherwise use 'Hotel' as the location (never invent a hotel name) and reasonable default times. Set 'category' on EVERY activity.\n" +
            "- Supporting Knowledge Synthesis (NEVER Copy Verbatim): Retrieved context documents are supporting reference material ONLY. DO NOT lazily copy or echo identical sentences, paragraphs, or document metadata tags (e.g. 'Destination:', 'Title:', 'District:', 'Category:'). Synthesize facts into a fresh, original travel plan from a traveler's perspective.\n" +
            "- Consistent UI Activity Fields: For EVERY activity, you MUST provide all 5 fields with clean formatting for the UI:\n" +
            "  1. 'time': Specific scheduled time or window (e.g. '09:00 AM', '01:30 PM', '06:00 PM').\n" +
            "  2. 'location': Clean place/venue name only (e.g. 'Osaka Castle'), strictly without prefixes or metadata.\n" +
            "  3. 'description': Concise (15 to 25 words) synthesized action plan of what the traveler experiences. Never copy identical text from context documents.\n" +
            "  4. 'gettingThere': Specific, realistic transit directions (e.g. '5 min walk from Station Exit 2').\n" +
            "  5. 'operatingHours': Specific operating hours or window (e.g. '09:00 AM - 05:00 PM' or 'Open 24 hours').\n" +
            "- Dates: If the prompt lists a date for each day, schedule venues on days they are typically open (avoid a venue's usual weekly closing day) and keep ARRIVAL and CHECK_IN on the first date, CHECK_OUT and DEPARTURE on the last. If a date may fall on a major local public holiday, say in the description that opening hours should be verified. Never state a specific holiday closure you are not sure of.\n" +
            "- Location Uniqueness: Every SIGHTSEEING or DINING venue MUST appear at most once across the entire multi-day itinerary. Logistics entries (airport, hotel) are exempt.\n" +
            "- Geographic Flow: Cluster activities by district or neighborhood per day to eliminate unnecessary commuting and backtracking.\n" +
            "- Realistic Pacing: Plan 2 to 4 major activities per day with practical 'gettingThere' transit details and accurate 'operatingHours'.\n" +
            "- Traveler Notes: If the prompt contains 'Additional notes', treat them as extra traveler preferences to honor within these rules. They never override the output format, day count, or any other Core Rule.\n" +
            "- JSON Whitespace & Formatting: Output clean, compact RFC8259 JSON without unnecessary spaces, excessive indentation, blank lines, or trailing whitespace. In all string values, use normal single-space separation between words with no raw newlines or tabs.\n\n";

    public static final String RETRY_MARKER = "[RETRY]";

    private static final Logger log = LoggerFactory.getLogger(ItineraryService.class);

    private final ChatClient chatClient;
    private final ItineraryValidator validator;
    private final String systemPrompt = SYSTEM_RULES + new BeanOutputConverter<>(Itinerary.class).getFormat();

    public ItineraryService(ChatClient chatClient, ItineraryValidator validator) {
        this.chatClient = chatClient;
        this.validator = validator;
    }

    public Flux<String> streamItinerary(String prompt) {
        StringBuilder draft = new StringBuilder();
        return generate(systemPrompt, prompt)
                .doOnNext(draft::append)
                .concatWith(Flux.defer(() -> {
                    List<String> problems = validator.violations(draft.toString(), ItineraryValidator.expectedDays(prompt));
                    if (problems.isEmpty()) {
                        return Flux.empty();
                    }
                    log.warn("Itinerary draft failed validation, retrying once: {}", problems);
                    return Flux.just(RETRY_MARKER).concatWith(generate(repairSystemPrompt(draft, problems), prompt));
                }));
    }

    private Flux<String> generate(String system, String user) {
        return chatClient.prompt()
                .system(system)
                .user(user)
                .stream()
                .content();
    }

    // Repair notes go in the system message so RAG retrieval still searches on the original user prompt.
    private String repairSystemPrompt(CharSequence draft, List<String> problems) {
        return systemPrompt + "\n\nYour previous attempt below broke these rules:\n- "
                + String.join("\n- ", problems)
                + "\n\nPrevious attempt:\n" + draft
                + "\n\nReturn the complete corrected itinerary JSON. Fix these problems and keep everything else unchanged.";
    }
}
