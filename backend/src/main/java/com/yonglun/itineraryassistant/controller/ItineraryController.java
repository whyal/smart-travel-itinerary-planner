package com.yonglun.itineraryassistant.controller;

import com.yonglun.itineraryassistant.dto.PromptRequest;
import com.yonglun.itineraryassistant.service.ItineraryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/itinerary")
public class ItineraryController {
    private final ItineraryService itineraryService;

    public ItineraryController(ItineraryService itineraryService) {
        this.itineraryService = itineraryService;
    }

        @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamItinerary(@RequestBody PromptRequest request) {
        PromptRequest valid = validated(request);
        return itineraryService.streamItinerary(valid.prompt());
    }

    private static PromptRequest validated(PromptRequest request) {
        if (request.prompt() == null || request.prompt().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "prompt is required");
        }
        if (request.conversationId() == null || request.conversationId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "conversationId is required");
        }
        return request;
    }
}
