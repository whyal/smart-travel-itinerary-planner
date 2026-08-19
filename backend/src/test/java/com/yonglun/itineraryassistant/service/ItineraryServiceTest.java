package com.yonglun.itineraryassistant.service;

import com.yonglun.itineraryassistant.model.Itinerary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItineraryServiceTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatClient chatClient;

    private ItineraryService itineraryService;

    @BeforeEach
    void setUp() {
        itineraryService = new ItineraryService(chatClient);
    }

    @Test
    void testGenerateItinerary() {
        Itinerary expected = new Itinerary("Kyoto", List.of());

        when(chatClient.prompt()
                .user(anyString())
                .advisors(any(Consumer.class))
                .call()
                .entity(any(Class.class), any(Consumer.class)))
                .thenReturn(expected);

        Itinerary result = itineraryService.generateItinerary("3 days in Kyoto", "conv-1");

        assertThat(result).isNotNull();
        assertThat(result.destination()).isEqualTo("Kyoto");
    }

    @Test
    void testStreamItinerary() {
        when(chatClient.prompt()
                .system(anyString())
                .user(any(Consumer.class))
                .advisors(any(Consumer.class))
                .stream()
                .content())
                .thenReturn(Flux.just("{\"destination\":", "\"Kyoto\"}"));

        Flux<String> flux = itineraryService.streamItinerary("3 days in Kyoto", "conv-1");

        List<String> chunks = flux.collectList().block();
        assertThat(chunks).containsExactly("{\"destination\":", "\"Kyoto\"}");
    }
}
