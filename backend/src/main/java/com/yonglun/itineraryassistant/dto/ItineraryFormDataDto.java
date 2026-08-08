package com.yonglun.itineraryassistant.dto;

public record ItineraryFormDataDto(
    String destination,
    Integer days,
    String pace,
    String interests,
    String budget
) {}
