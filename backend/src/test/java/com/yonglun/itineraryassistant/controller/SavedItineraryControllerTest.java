package com.yonglun.itineraryassistant.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yonglun.itineraryassistant.dto.ItineraryFormDataDto;
import com.yonglun.itineraryassistant.dto.SaveItineraryRequest;
import com.yonglun.itineraryassistant.dto.SavedItineraryResponse;
import com.yonglun.itineraryassistant.model.Itinerary;
import com.yonglun.itineraryassistant.service.SavedItineraryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class SavedItineraryControllerTest {

    @Mock
    private SavedItineraryService service;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(new SavedItineraryController(service)).build();
    }

    @Test
    void testSaveItinerarySuccess() throws Exception {
        ItineraryFormDataDto formData = new ItineraryFormDataDto(
                "Paris", 3, "Moderate", "Museums, Food", "Medium"
        );

        Itinerary.Activity activity = new Itinerary.Activity(
                "10:00 AM", "Louvre Museum", "Explore classical art masterpieces"
        );
        Itinerary.DayPlan dayPlan = new Itinerary.DayPlan(1, "Art & History", List.of(activity));
        Itinerary itinerary = new Itinerary("Paris", List.of(dayPlan));

        SaveItineraryRequest request = new SaveItineraryRequest(
                "conv-12345",
                "Paris",
                3,
                formData,
                itinerary,
                "Day 1: Art & History...",
                "2026-08-08T09:24:29.000Z"
        );

        SavedItineraryResponse mockResponse = new SavedItineraryResponse(
                1L,
                "conv-12345",
                "Paris",
                3,
                formData,
                itinerary,
                "Day 1: Art & History...",
                Instant.parse("2026-08-08T09:24:29.000Z")
        );

        when(service.saveItinerary(any(SaveItineraryRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/itineraries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.conversationId", is("conv-12345")))
                .andExpect(jsonPath("$.destination", is("Paris")))
                .andExpect(jsonPath("$.daysCount", is(3)))
                .andExpect(jsonPath("$.formData.pace", is("Moderate")))
                .andExpect(jsonPath("$.itinerary.destination", is("Paris")))
                .andExpect(jsonPath("$.rawText", is("Day 1: Art & History...")));
    }

    @Test
    void testGetAllAndFilterByConversationId() throws Exception {
        ItineraryFormDataDto formData = new ItineraryFormDataDto("Tokyo", 5, "Fast", "Anime", "High");
        Itinerary itinerary = new Itinerary("Tokyo", List.of());

        SavedItineraryResponse res1 = new SavedItineraryResponse(1L, "session-1", "Tokyo", 5, formData, itinerary, "text1", Instant.now());
        SavedItineraryResponse res2 = new SavedItineraryResponse(2L, "session-2", "Kyoto", 3, formData, itinerary, "text2", Instant.now());

        when(service.getAllSavedItineraries()).thenReturn(List.of(res1, res2));
        when(service.getSavedItinerariesByConversationId(eq("session-1"))).thenReturn(List.of(res1));

        // Get all
        mockMvc.perform(get("/api/itineraries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        // Filter by session-1
        mockMvc.perform(get("/api/itineraries").param("conversationId", "session-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].conversationId", is("session-1")));
    }
}
