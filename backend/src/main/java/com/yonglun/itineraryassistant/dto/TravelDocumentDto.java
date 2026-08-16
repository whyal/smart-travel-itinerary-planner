package com.yonglun.itineraryassistant.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TravelDocumentDto(
        String id,
        String destination,
        String title,
        String category,
        String district,
        String content,
        List<String> tags,
        String suggestedDuration,
        String bestTimeToVisit,
        Map<String, Object> metadata
) {
    public TravelDocumentDto {
        if (tags == null) {
            tags = List.of();
        }
        if (metadata == null) {
            metadata = Map.of();
        }
    }
}
