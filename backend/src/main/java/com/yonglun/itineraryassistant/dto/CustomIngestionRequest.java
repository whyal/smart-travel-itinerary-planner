package com.yonglun.itineraryassistant.dto;

import java.util.List;

public record CustomIngestionRequest(
        String destination,
        List<String> articles
) {}
