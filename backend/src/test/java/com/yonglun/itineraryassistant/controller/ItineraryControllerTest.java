package com.yonglun.itineraryassistant.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yonglun.itineraryassistant.dto.PromptRequest;
import com.yonglun.itineraryassistant.model.Itinerary;
import com.yonglun.itineraryassistant.service.ItineraryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ItineraryControllerTest {

    @Mock
    private ItineraryService itineraryService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(new ItineraryController(itineraryService))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void testStreamItinerary() throws Exception {
        PromptRequest request = new PromptRequest("Trip to Kyoto", "conv-1");

        when(itineraryService.streamItinerary(eq("Trip to Kyoto")))
                .thenReturn(Flux.just("chunk1", "chunk2"));

        mockMvc.perform(post("/api/itinerary/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void testBlankConversationIdIsRejected() throws Exception {
        for (String path : List.of("/api/itinerary/stream")) {
            for (String body : List.of("{\"prompt\":\"Trip\"}", "{\"prompt\":\"Trip\",\"conversationId\":\"  \"}")) {
                mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.message", is("conversationId is required")));
            }
        }
        verifyNoInteractions(itineraryService);
    }

    @Test
    void testBlankPromptIsRejected() throws Exception {
        for (String path : List.of("/api/itinerary/stream")) {
            mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"prompt\":\"  \",\"conversationId\":\"conv-1\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", is("prompt is required")));
        }
        verifyNoInteractions(itineraryService);
    }
}
