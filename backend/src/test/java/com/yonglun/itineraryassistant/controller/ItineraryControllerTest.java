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
        this.mockMvc = MockMvcBuilders.standaloneSetup(new ItineraryController(itineraryService)).build();
    }

    @Test
    void testGenerateItinerary() throws Exception {
        PromptRequest request = new PromptRequest("Trip to Kyoto", "conv-1");
        Itinerary itinerary = new Itinerary("Kyoto", List.of());

        when(itineraryService.generateItinerary(eq("Trip to Kyoto"), eq("conv-1"))).thenReturn(itinerary);

        mockMvc.perform(post("/api/itinerary/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.destination", is("Kyoto")));
    }

    @Test
    void testStreamItinerary() throws Exception {
        PromptRequest request = new PromptRequest("Trip to Kyoto", "conv-1");

        when(itineraryService.streamItinerary(eq("Trip to Kyoto"), eq("conv-1")))
                .thenReturn(Flux.just("chunk1", "chunk2"));

        mockMvc.perform(post("/api/itinerary/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}
