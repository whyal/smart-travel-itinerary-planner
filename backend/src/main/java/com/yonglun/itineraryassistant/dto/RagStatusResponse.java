package com.yonglun.itineraryassistant.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;
import java.util.Set;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RagStatusResponse(
        String status,
        boolean kyotoIngested,
        int totalDocumentsIngested,
        Set<String> destinations,
        Map<String, Integer> documentCountByDestination
) {
    public RagStatusResponse(String status, boolean kyotoIngested, int totalDocumentsIngested) {
        this(status, kyotoIngested, totalDocumentsIngested, Set.of(), Map.of());
    }
}
