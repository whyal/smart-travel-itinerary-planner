package com.yonglun.itineraryassistant.dto;

public record PromptRequest(
    String prompt,
    String conversationId
) {}
