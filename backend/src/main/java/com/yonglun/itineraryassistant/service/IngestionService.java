package com.yonglun.itineraryassistant.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yonglun.itineraryassistant.dto.TravelDocumentDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    private final VectorStore vectorStore;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;

    private final AtomicInteger ingestedDocumentCount = new AtomicInteger(0);
    private final Map<String, Integer> destinationDocumentCounts = new ConcurrentHashMap<>();

    public IngestionService(VectorStore vectorStore, ResourceLoader resourceLoader, ObjectMapper objectMapper) {
        this.vectorStore = vectorStore;
        this.resourceLoader = resourceLoader;
        this.objectMapper = objectMapper;
    }

    public synchronized int ingestDocuments(List<TravelDocumentDto> items, String defaultDestination) {
        if (items == null || items.isEmpty()) {
            return 0;
        }

        List<Document> documents = new ArrayList<>();
        Map<String, Integer> batchCounts = new HashMap<>();

        for (TravelDocumentDto item : items) {
            if (item == null) continue;
            String dest = hasText(item.destination()) ? item.destination().trim()
                    : (hasText(defaultDestination) ? defaultDestination.trim() : "General");
            documents.add(toDocument(item, dest));
            batchCounts.merge(dest, 1, Integer::sum);
        }

        return recordAndStore(documents, batchCounts);
    }

    public synchronized int ingestDocuments(List<TravelDocumentDto> items) {
        return ingestDocuments(items, null);
    }

    public synchronized int ingestDestinationKnowledge(List<String> rawArticles, String destination) {
        if (rawArticles == null || rawArticles.isEmpty()) {
            return 0;
        }

        String dest = hasText(destination) ? destination.trim() : "General";
        List<Document> docs = rawArticles.stream()
                .filter(this::hasText)
                .map(content -> new Document(
                        UUID.randomUUID().toString(),
                        content.trim(),
                        Map.of("destination", dest, "type", "travel_guide")
                ))
                .toList();

        return recordAndStore(docs, Map.of(dest, docs.size()));
    }

    public synchronized int ingestFile(MultipartFile file, String fallbackDestination) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty.");
        }

        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        String dest = hasText(fallbackDestination) ? fallbackDestination.trim() : "General";

        // 1. PDF File Ingestion
        if (filename.endsWith(".pdf") || "application/pdf".equalsIgnoreCase(file.getContentType())) {
            ByteArrayResource resource = new ByteArrayResource(file.getBytes(), file.getOriginalFilename());
            PagePdfDocumentReader reader = new PagePdfDocumentReader(resource, PdfDocumentReaderConfig.builder().build());
            List<Document> splitDocs = TokenTextSplitter.builder().build().apply(reader.get());

            List<Document> enrichedDocs = splitDocs.stream()
                    .filter(doc -> hasText(doc.getText()))
                    .map(doc -> {
                        Map<String, Object> meta = new HashMap<>(doc.getMetadata());
                        meta.put("destination", dest);
                        meta.put("type", "travel_guide");
                        meta.put("source_filename", file.getOriginalFilename());
                        String content = "Destination: " + dest + "\nSource: " + file.getOriginalFilename() + "\n\n" + doc.getText().trim();
                        return new Document(UUID.randomUUID().toString(), content, meta);
                    })
                    .toList();

            return recordAndStore(enrichedDocs, Map.of(dest, enrichedDocs.size()));
        }

        // 2. JSON File Ingestion
        if (filename.endsWith(".json") || (file.getContentType() != null && file.getContentType().contains("json"))) {
            JsonNode root;
            try (InputStream is = file.getInputStream()) {
                root = objectMapper.readTree(is);
            }

            if (root.isArray()) {
                if (!root.isEmpty() && root.get(0).isTextual()) {
                    List<String> articles = objectMapper.convertValue(root, new TypeReference<List<String>>() {});
                    return ingestDestinationKnowledge(articles, dest);
                }
                List<TravelDocumentDto> items = objectMapper.convertValue(root, new TypeReference<List<TravelDocumentDto>>() {});
                return ingestDocuments(items, dest);
            }

            if (root.isObject()) {
                String targetDest = root.hasNonNull("destination") ? root.get("destination").asText() : dest;
                if (root.has("documents")) {
                    List<TravelDocumentDto> items = objectMapper.convertValue(root.get("documents"), new TypeReference<List<TravelDocumentDto>>() {});
                    return ingestDocuments(items, targetDest);
                }
                if (root.has("articles")) {
                    List<String> articles = objectMapper.convertValue(root.get("articles"), new TypeReference<List<String>>() {});
                    return ingestDestinationKnowledge(articles, targetDest);
                }
                return ingestDocuments(List.of(objectMapper.convertValue(root, TravelDocumentDto.class)), targetDest);
            }

            throw new IllegalArgumentException("Unsupported JSON structure for ingestion.");
        }

        // 3. Plain Text / Markdown File Ingestion
        String text = new String(file.getBytes(), StandardCharsets.UTF_8);
        List<String> articles = Arrays.stream(text.split("(?m)^\\s*$\\n+"))
                .map(String::trim)
                .filter(this::hasText)
                .toList();

        return ingestDestinationKnowledge(articles.isEmpty() ? List.of(text.trim()) : articles, dest);
    }

    public synchronized int ingestResource(Resource resource, String fallbackDestination) {
        try {
            if (resource == null || !resource.exists()) {
                log.warn("Knowledge data resource not found: {}", resource);
                return 0;
            }

            try (InputStream is = resource.getInputStream()) {
                List<TravelDocumentDto> items = objectMapper.readValue(is, new TypeReference<List<TravelDocumentDto>>() {});
                return ingestDocuments(items, fallbackDestination);
            }
        } catch (Exception e) {
            log.error("Failed to ingest knowledge documents from resource", e);
            throw new RuntimeException("Knowledge ingestion failed: " + e.getMessage(), e);
        }
    }

    public synchronized int ingestPreloadedKnowledge(String destination, String resourcePath) {
        String dest = hasText(destination) ? destination.trim() : "General";
        String path = hasText(resourcePath)
                ? resourcePath
                : "classpath:data/" + dest.toLowerCase() + "/" + dest.toLowerCase() + "_travel_knowledge.json";

        return ingestResource(resourceLoader.getResource(path), dest);
    }

    public List<Document> similaritySearch(String query, int topK) {
        return vectorStore.similaritySearch(
                SearchRequest.builder().query(query).topK(Math.max(1, topK)).build()
        );
    }

    public boolean isDestinationIngested(String destination) {
        return hasText(destination) && destinationDocumentCounts.entrySet().stream()
                .anyMatch(e -> e.getKey().equalsIgnoreCase(destination.trim()) && e.getValue() > 0);
    }

    public int getIngestedDocumentCount() {
        return ingestedDocumentCount.get();
    }

    public Set<String> getIngestedDestinations() {
        return Collections.unmodifiableSet(destinationDocumentCounts.keySet());
    }

    public Map<String, Integer> getDestinationDocumentCounts() {
        return Collections.unmodifiableMap(destinationDocumentCounts);
    }

    private Document toDocument(TravelDocumentDto item, String destination) {
        String docId = hasText(item.id()) ? item.id().trim() : UUID.randomUUID().toString();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("destination", destination);
        metadata.put("doc_id", docId);
        metadata.put("type", "travel_guide");

        putIfText(metadata, "title", item.title());
        putIfText(metadata, "category", item.category());
        putIfText(metadata, "district", item.district());
        putIfText(metadata, "best_time_to_visit", item.bestTimeToVisit());
        putIfText(metadata, "suggested_duration", item.suggestedDuration());

        if (item.tags() != null && !item.tags().isEmpty()) {
            metadata.put("tags", String.join(", ", item.tags()));
        }
        if (item.metadata() != null && !item.metadata().isEmpty()) {
            metadata.putAll(item.metadata());
        }

        StringBuilder sb = new StringBuilder("Destination: ").append(destination).append("\n");
        appendField(sb, "Title: ", item.title());
        appendField(sb, "District: ", item.district());
        appendField(sb, "Category: ", item.category());
        appendField(sb, "Best Time to Visit: ", item.bestTimeToVisit());
        appendField(sb, "Suggested Duration: ", item.suggestedDuration());

        if (hasText(item.content())) {
            sb.append("\n").append(item.content().trim());
        }

        return new Document(docId, sb.toString().trim(), metadata);
    }

    private int recordAndStore(List<Document> docs, Map<String, Integer> counts) {
        if (docs.isEmpty()) return 0;
        vectorStore.add(docs);
        ingestedDocumentCount.addAndGet(docs.size());
        counts.forEach((dest, count) -> destinationDocumentCounts.merge(dest, count, Integer::sum));
        log.info("Successfully ingested {} documents into VectorStore for destinations: {}", docs.size(), counts.keySet());
        return docs.size();
    }

    private boolean hasText(String str) {
        return str != null && !str.isBlank();
    }

    private void putIfText(Map<String, Object> map, String key, String val) {
        if (hasText(val)) map.put(key, val.trim());
    }

    private void appendField(StringBuilder sb, String label, String val) {
        if (hasText(val)) sb.append(label).append(val.trim()).append("\n");
    }
}

