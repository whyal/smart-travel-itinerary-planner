package com.yonglun.itineraryassistant.controller;

import com.yonglun.itineraryassistant.config.EmbeddingProperties;
import com.yonglun.itineraryassistant.dto.*;
import com.yonglun.itineraryassistant.service.IngestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeController.class);

    private final IngestionService ingestionService;
    private final EmbeddingProperties embeddingProperties;

    public KnowledgeController(
            IngestionService ingestionService,
            EmbeddingProperties embeddingProperties) {
        this.ingestionService = ingestionService;
        this.embeddingProperties = embeddingProperties;
    }

    /**
     * Ingest a flat list of structured travel documents.
     * For batched ingestion with an explicit destination wrapper, use {@link #ingestBatch(DocumentBatchIngestionRequest)}.
     */
    @PostMapping(value = "/documents", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<IngestionResponse> ingestDocuments(
            @RequestBody List<TravelDocumentDto> documents,
            @RequestParam(required = false) String destination) {

        if (documents == null || documents.isEmpty()) {
            return ResponseEntity.badRequest().body(new IngestionResponse(
                    "error", "Documents list cannot be empty.", destination, 0
            ));
        }

        int count = ingestionService.ingestDocuments(documents, destination);
        String targetDest = (destination != null && !destination.isBlank())
                ? destination
                : (!documents.isEmpty() && documents.getFirst() != null && documents.getFirst().destination() != null
                ? documents.getFirst().destination()
                : "Multiple Destinations");

        return ResponseEntity.ok(new IngestionResponse(
                "success", "Successfully embedded and ingested " + count + " structured travel documents.", targetDest, count
        ));
    }

    /**
     * Ingest a batch of structured travel documents with an explicit destination wrapper.
     * Prefer {@link #ingestDocuments(List, String)} with a {@code ?destination} query param for new integrations.
     *
     * @deprecated Use POST /api/knowledge/documents?destination=... instead.
     */
    @Deprecated
    @PostMapping(value = "/batch", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<IngestionResponse> ingestBatch(@RequestBody DocumentBatchIngestionRequest request) {
        if (request == null || request.documents() == null || request.documents().isEmpty()) {
            return ResponseEntity.badRequest().body(new IngestionResponse(
                    "error", "Request documents list cannot be empty.", request != null ? request.destination() : null, 0
            ));
        }

        int count = ingestionService.ingestDocuments(request.documents(), request.destination());
        return ResponseEntity.ok(new IngestionResponse(
                "success", "Successfully embedded and ingested " + count + " structured documents for destination [" + request.destination() + "].",
                request.destination(), count
        ));
    }

    /**
     * Ingest raw article text for a destination.
     * The canonical path is POST /api/knowledge/articles; /custom and /ingest are deprecated aliases.
     */
    @PostMapping(value = "/articles", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<IngestionResponse> ingestArticles(@RequestBody CustomIngestionRequest request) {
        if (request == null || request.articles() == null || request.articles().isEmpty()) {
            return ResponseEntity.badRequest().body(new IngestionResponse(
                    "error", "Articles list cannot be empty.", request != null ? request.destination() : null, 0
            ));
        }

        int count = ingestionService.ingestDestinationKnowledge(request.articles(), request.destination());
        return ResponseEntity.ok(new IngestionResponse(
                "success", "Successfully embedded and ingested " + count + " raw article documents for destination [" + request.destination() + "].",
                request.destination(), count
        ));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<IngestionResponse> uploadDocumentFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "destination", required = false) String destination) throws Exception {

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(new IngestionResponse(
                    "error", "Uploaded file is empty or missing.", destination, 0
            ));
        }

        try {
            int count = ingestionService.ingestFile(file, destination);
            return ResponseEntity.ok(new IngestionResponse(
                    "success", "Successfully parsed and ingested " + count + " documents from file [" + file.getOriginalFilename() + "].",
                    destination, count
            ));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid file content uploaded: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new IngestionResponse(
                    "error", "Invalid file format: " + e.getMessage(), destination, 0
            ));
        }
    }

    @PostMapping("/preload")
    public ResponseEntity<IngestionResponse> preloadKnowledge(
            @RequestParam(defaultValue = "Kyoto") String destination,
            @RequestParam(required = false) String resourcePath) {

        int count = ingestionService.ingestPreloadedKnowledge(destination, resourcePath);
        if (count == 0) {
            return ResponseEntity.ok(new IngestionResponse(
                    "not_found", "No preloaded knowledge files found for destination [" + destination + "]. The assistant will rely on model knowledge or user-ingested documents.",
                    destination, 0
            ));
        }
        return ResponseEntity.ok(new IngestionResponse(
                "success", "Successfully preloaded and ingested " + count + " documents for destination [" + destination + "].",
                destination, count
        ));
    }

    @GetMapping({"/status", "/stats"})
    public ResponseEntity<RagStatusResponse> getStatus() {
        return ResponseEntity.ok(new RagStatusResponse(
                "ready",
                ingestionService.getIngestedDocumentCount(),
                ingestionService.getIngestedDestinations(),
                ingestionService.getDestinationDocumentCounts(),
                embeddingProperties != null ? embeddingProperties.getProvider() : null,
                embeddingProperties != null ? embeddingProperties.getModel() : null,
                embeddingProperties != null ? embeddingProperties.getDimensions() : null,
                embeddingProperties != null ? embeddingProperties.getEffectiveTableName() : null
        ));
    }

    @GetMapping({"/similarity-search", "/search"})
    public ResponseEntity<List<RagDocumentResponse>> searchKnowledge(
            @RequestParam String query,
            @RequestParam(defaultValue = "4") int topK) {

        List<RagDocumentResponse> responses = ingestionService.similaritySearch(query, topK).stream()
                .map(doc -> new RagDocumentResponse(doc.getId(), doc.getText(), doc.getMetadata(), doc.getScore()))
                .toList();

        return ResponseEntity.ok(responses);
    }
}
