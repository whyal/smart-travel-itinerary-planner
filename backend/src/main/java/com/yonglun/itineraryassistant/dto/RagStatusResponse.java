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
) {}
