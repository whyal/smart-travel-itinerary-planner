package com.yonglun.itineraryassistant.dto;

import com.yonglun.itineraryassistant.model.Itinerary;
import java.time.Instant;

public record SavedItineraryResponse(
    Long id,
    String conversationId,
    String destination,
    Integer daysCount,
    ItineraryFormDataDto formData,
    Itinerary itinerary,
    String rawText,
    Instant createdAt
) {}
