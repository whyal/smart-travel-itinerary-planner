package com.yonglun.itineraryassistant.dto;

import com.yonglun.itineraryassistant.model.Itinerary;

public record SaveItineraryRequest(
    String conversationId,
    String destination,
    Integer daysCount,
    ItineraryFormDataDto formData,
    Itinerary itinerary,
    String rawText,
    String createdAt
) {}
