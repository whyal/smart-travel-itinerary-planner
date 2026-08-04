package com.yonglun.itineraryassistant.dto;


// Lightweight DTO matching the frontend payload
public record PromptRequest(
    String prompt,
    String conversationId // Optional UUID/session ID from frontend
) {}
