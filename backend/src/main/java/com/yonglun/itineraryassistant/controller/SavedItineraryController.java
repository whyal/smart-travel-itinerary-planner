package com.yonglun.itineraryassistant.controller;

import com.yonglun.itineraryassistant.dto.SaveItineraryRequest;
import com.yonglun.itineraryassistant.dto.SavedItineraryResponse;
import com.yonglun.itineraryassistant.service.SavedItineraryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/itineraries", "/api/itineraries"})
public class SavedItineraryController {

    private final SavedItineraryService service;

    public SavedItineraryController(SavedItineraryService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<SavedItineraryResponse> saveItinerary(@RequestBody SaveItineraryRequest request) {
        SavedItineraryResponse response = service.saveItinerary(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<SavedItineraryResponse>> getAllSavedItineraries(
            @RequestParam(required = false) String conversationId) {
        if (conversationId != null && !conversationId.isBlank()) {
            return ResponseEntity.ok(service.getSavedItinerariesByConversationId(conversationId));
        }
        return ResponseEntity.ok(service.getAllSavedItineraries());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SavedItineraryResponse> getSavedItineraryById(@PathVariable Long id) {
        return service.getSavedItineraryById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSavedItinerary(@PathVariable Long id) {
        if (service.deleteSavedItinerary(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
