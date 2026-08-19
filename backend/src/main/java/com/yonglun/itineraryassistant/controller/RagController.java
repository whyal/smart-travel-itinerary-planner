package com.yonglun.itineraryassistant.controller;

import com.yonglun.itineraryassistant.dto.CustomIngestionRequest;
import com.yonglun.itineraryassistant.dto.IngestionResponse;
import com.yonglun.itineraryassistant.dto.RagDocumentResponse;
import com.yonglun.itineraryassistant.dto.RagStatusResponse;
import com.yonglun.itineraryassistant.service.IngestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rag")
public class RagController {

    private final IngestionService ingestionService;

    public RagController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping(value = {"/ingest", "/ingest/custom"})
    public ResponseEntity<IngestionResponse> ingestCustomKnowledge(@RequestBody CustomIngestionRequest request) {
        if (request == null || request.articles() == null || request.articles().isEmpty()) {
            return ResponseEntity.badRequest().body(new IngestionResponse(
                    "error",
                    "Articles list cannot be empty.",
                    request != null ? request.destination() : null,
                    0
            ));
        }

        int count = ingestionService.ingestDestinationKnowledge(request.articles(), request.destination());
        return ResponseEntity.ok(new IngestionResponse(
                "success",
                "Successfully embedded and ingested " + count + " documents for " + request.destination() + ".",
                request.destination(),
                count
        ));
    }

    @GetMapping("/similarity-search")
    public ResponseEntity<List<RagDocumentResponse>> searchKnowledge(
            @RequestParam String query,
            @RequestParam(defaultValue = "4") int topK) {

        List<RagDocumentResponse> responses = ingestionService.similaritySearch(query, topK).stream()
                .map(doc -> new RagDocumentResponse(doc.getId(), doc.getText(), doc.getMetadata(), doc.getScore()))
                .toList();

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/status")
    public ResponseEntity<RagStatusResponse> getStatus() {
        return ResponseEntity.ok(new RagStatusResponse(
                "ready",
                ingestionService.getIngestedDocumentCount(),
                ingestionService.getIngestedDestinations(),
                ingestionService.getDestinationDocumentCounts()
        ));
    }
}

