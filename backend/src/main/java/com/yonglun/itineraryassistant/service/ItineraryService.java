package com.yonglun.itineraryassistant.service;

import com.yonglun.itineraryassistant.model.Itinerary;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class ItineraryService {

    private final ChatClient chatClient;
    private final BeanOutputConverter<Itinerary> outputConverter = new BeanOutputConverter<>(Itinerary.class);

    public ItineraryService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public Itinerary generateItinerary(String prompt, String conversationId) {
        return chatClient.prompt()
                .user(prompt)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, resolveSessionId(conversationId)))
                .call()
                // Force Gemini's native API JSON mode so it never outputs conversational text
                .entity(Itinerary.class, spec -> spec
                        .useProviderStructuredOutput()
                        .validateSchema()
                );
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

