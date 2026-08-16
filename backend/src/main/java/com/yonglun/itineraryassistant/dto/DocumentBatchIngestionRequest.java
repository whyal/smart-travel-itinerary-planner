package com.yonglun.itineraryassistant.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DocumentBatchIngestionRequest(
        String destination,
        List<TravelDocumentDto> documents
) {}
