package com.yonglun.itineraryassistant.dto;

public record RagStatusResponse(
        String status,
        boolean kyotoIngested,
        int totalDocumentsIngested
) {}
