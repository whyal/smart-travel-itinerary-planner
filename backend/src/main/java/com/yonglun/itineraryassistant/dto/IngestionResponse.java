package com.yonglun.itineraryassistant.dto;

public record IngestionResponse(
        String status,
        String message,
        String destination,
        int documentsIngested
) {}
