package com.yonglun.itineraryassistant.controller;

import com.yonglun.itineraryassistant.dto.PromptRequest;
import com.yonglun.itineraryassistant.service.ItineraryService;
import com.yonglun.itineraryassistant.service.ItineraryValidator;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/itinerary")
public class ItineraryController {
    static final int MAX_PROMPT_CHARS = 4000;
    static final int MAX_DAYS = 30;

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
        if (request.prompt().length() > MAX_PROMPT_CHARS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "prompt must be at most " + MAX_PROMPT_CHARS + " characters");
        }
        Integer days = ItineraryValidator.expectedDays(request.prompt());
        if (days != null && (days < 1 || days > MAX_DAYS)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Days must be between 1 and " + MAX_DAYS);
        }
        return request;
    }
}
