package com.yonglun.itineraryassistant.dto;

import java.util.Map;

public record RagDocumentResponse(
        String id,
        String text,
        Map<String, Object> metadata,
        Double score
) {}
