package com.yonglun.itineraryassistant.controller;

import com.yonglun.itineraryassistant.dto.SaveItineraryRequest;
import com.yonglun.itineraryassistant.dto.SavedItineraryResponse;
import com.yonglun.itineraryassistant.service.SavedItineraryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/itineraries")
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
}
