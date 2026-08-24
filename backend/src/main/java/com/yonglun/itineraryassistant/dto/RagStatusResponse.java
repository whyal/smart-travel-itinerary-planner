package com.yonglun.itineraryassistant.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;
import java.util.Set;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RagStatusResponse(
        String status,
        int totalDocumentsIngested,
        Set<String> destinations,
        Map<String, Integer> documentCountByDestination,
        String embeddingProvider,
        String embeddingModel,
        Integer embeddingDimensions,
        String vectorTable
) {
    public RagStatusResponse(String status, int totalDocumentsIngested, Set<String> destinations, Map<String, Integer> documentCountByDestination) {
        this(status, totalDocumentsIngested, destinations, documentCountByDestination, null, null, null, null);
    }

    public RagStatusResponse(String status, int totalDocumentsIngested) {
        this(status, totalDocumentsIngested, Set.of(), Map.of(), null, null, null, null);
    }
}
