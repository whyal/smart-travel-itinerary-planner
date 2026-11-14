package com.yonglun.itineraryassistant.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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
    void testStreamItinerary() {
        when(chatClient.prompt()
                .system(anyString())
                .user(anyString())
                .stream()
                .content())
                .thenReturn(Flux.just("{\"destination\":", "\"Kyoto\"}"));

        Flux<String> flux = itineraryService.streamItinerary("3 days in Kyoto");

        List<String> chunks = flux.collectList().block();
        assertThat(chunks).containsExactly("{\"destination\":", "\"Kyoto\"}");
    }
}
