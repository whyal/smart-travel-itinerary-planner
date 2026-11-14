package com.yonglun.itineraryassistant.service;

import com.yonglun.itineraryassistant.model.Itinerary;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class ItineraryService {

    private static final String SYSTEM_RULES = "You are an expert travel data API. Output ONLY raw, RFC8259-compliant JSON without markdown code blocks (```json) or introductory text.\n\n" +
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
            "- JSON Whitespace & Formatting: Output clean, compact RFC8259 JSON without unnecessary spaces, excessive indentation, blank lines, or trailing whitespace. In all string values, use normal single-space separation between words with no raw newlines or tabs.\n\n";

    private final ChatClient chatClient;
    private final String systemPrompt = SYSTEM_RULES + new BeanOutputConverter<>(Itinerary.class).getFormat();

    public ItineraryService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public Flux<String> streamItinerary(String prompt, String conversationId) {
        return chatClient.prompt()
                .system(systemPrompt)
                .user(prompt)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, conversationId))
                .stream()
                .content();
    }
}
