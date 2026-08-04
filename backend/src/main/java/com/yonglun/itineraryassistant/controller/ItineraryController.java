package com.yonglun.itineraryassistant.controller;

import com.yonglun.itineraryassistant.dto.PromptRequest;
import com.yonglun.itineraryassistant.model.Itinerary;
import com.yonglun.itineraryassistant.service.ItineraryService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/itinerary")
public class ItineraryController {
    private final ItineraryService itineraryService;

    public ItineraryController(ItineraryService itineraryService) {
        this.itineraryService = itineraryService;
    }

    /**
     * Standard synchronous JSON endpoint
     */
    @PostMapping("/generate")
    public ResponseEntity<Itinerary> generateItinerary(@RequestBody PromptRequest request) {
        Itinerary itinerary = itineraryService.generateItinerary(
                request.prompt(),
                request.conversationId()
        );
        return ResponseEntity.ok(itinerary);
    }

    /**
     * Server-Sent Events (SSE) streaming endpoint
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamItinerary(@RequestBody PromptRequest request) {
        return itineraryService.streamItinerary(
                request.prompt(),
                request.conversationId()
        );
    }
}
