package com.yonglun.itineraryassistant.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yonglun.itineraryassistant.dto.ItineraryFormDataDto;
import com.yonglun.itineraryassistant.dto.SaveItineraryRequest;
import com.yonglun.itineraryassistant.model.Itinerary;
import com.yonglun.itineraryassistant.repository.SavedItineraryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class SavedItineraryControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private SavedItineraryRepository repository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.webApplicationContext).build();
        repository.deleteAll();
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

        mockMvc.perform(post("/api/itineraries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
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

        SaveItineraryRequest req1 = new SaveItineraryRequest("session-1", "Tokyo", 5, formData, itinerary, "text1", null);
        SaveItineraryRequest req2 = new SaveItineraryRequest("session-2", "Kyoto", 3, formData, itinerary, "text2", null);

        mockMvc.perform(post("/api/itineraries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req1))).andExpect(status().isCreated());

        mockMvc.perform(post("/api/itineraries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req2))).andExpect(status().isCreated());

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
