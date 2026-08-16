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

    @org.springframework.beans.factory.annotation.Autowired
    public IngestionService(VectorStore vectorStore, ResourceLoader resourceLoader, ObjectMapper objectMapper) {
        this.vectorStore = vectorStore;
        this.resourceLoader = resourceLoader;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    /**
     * Ingests structured travel documents into the VectorStore.
     *
     * @param items              List of structured travel document items.
     * @param defaultDestination Default destination tag if item has none specified.
     * @return Number of documents ingested.
     */
    public synchronized int ingestDocuments(List<TravelDocumentDto> items, String defaultDestination) {
        if (items == null || items.isEmpty()) {
            return 0;
        }

        List<Document> documents = new ArrayList<>();
        Map<String, Integer> batchDestinationCounts = new HashMap<>();

        for (TravelDocumentDto item : items) {
            if (item == null) continue;

            String destination = (item.destination() != null && !item.destination().isBlank())
                    ? item.destination().trim()
                    : (defaultDestination != null && !defaultDestination.isBlank() ? defaultDestination.trim() : "General");

            String docId = (item.id() != null && !item.id().isBlank())
                    ? item.id().trim()
                    : UUID.randomUUID().toString();

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("destination", destination);
            metadata.put("doc_id", docId);
            metadata.put("type", "travel_guide");

            if (item.title() != null && !item.title().isBlank()) {
                metadata.put("title", item.title().trim());
            }
            if (item.category() != null && !item.category().isBlank()) {
                metadata.put("category", item.category().trim());
            }
            if (item.district() != null && !item.district().isBlank()) {
                metadata.put("district", item.district().trim());
            }
            if (item.bestTimeToVisit() != null && !item.bestTimeToVisit().isBlank()) {
                metadata.put("best_time_to_visit", item.bestTimeToVisit().trim());
            }
            if (item.suggestedDuration() != null && !item.suggestedDuration().isBlank()) {
                metadata.put("suggested_duration", item.suggestedDuration().trim());
            }
            if (item.tags() != null && !item.tags().isEmpty()) {
                metadata.put("tags", String.join(", ", item.tags()));
            }
            if (item.metadata() != null && !item.metadata().isEmpty()) {
                metadata.putAll(item.metadata());
            }

            StringBuilder contentBuilder = new StringBuilder();
            contentBuilder.append("Destination: ").append(destination).append("\n");
            if (item.title() != null && !item.title().isBlank()) {
                contentBuilder.append("Title: ").append(item.title().trim()).append("\n");
            }
            if (item.district() != null && !item.district().isBlank()) {
                contentBuilder.append("District: ").append(item.district().trim()).append("\n");
            }
            if (item.category() != null && !item.category().isBlank()) {
                contentBuilder.append("Category: ").append(item.category().trim()).append("\n");
            }
            if (item.bestTimeToVisit() != null && !item.bestTimeToVisit().isBlank()) {
                contentBuilder.append("Best Time to Visit: ").append(item.bestTimeToVisit().trim()).append("\n");
            }
            if (item.suggestedDuration() != null && !item.suggestedDuration().isBlank()) {
                contentBuilder.append("Suggested Duration: ").append(item.suggestedDuration().trim()).append("\n");
            }

            if (item.content() != null && !item.content().isBlank()) {
                contentBuilder.append("\n").append(item.content().trim());
            }

            Document doc = new Document(docId, contentBuilder.toString().trim(), metadata);
            documents.add(doc);
            batchDestinationCounts.merge(destination, 1, Integer::sum);
        }

        if (documents.isEmpty()) {
            return 0;
        }

        vectorStore.add(documents);
        ingestedDocumentCount.addAndGet(documents.size());
        batchDestinationCounts.forEach((dest, count) ->
                destinationDocumentCounts.merge(dest, count, Integer::sum)
        );

        log.info("Successfully ingested {} structured documents across {} destinations into VectorStore",
                documents.size(), batchDestinationCounts.keySet());
        return documents.size();
    }

    public synchronized int ingestDocuments(List<TravelDocumentDto> items) {
        return ingestDocuments(items, null);
    }

    /**
     * Ingest raw article strings for a specific destination into the VectorStore.
     */
    public synchronized int ingestDestinationKnowledge(List<String> rawArticles, String destination) {
        if (rawArticles == null || rawArticles.isEmpty()) {
            return 0;
        }

        String resolvedDestination = (destination != null && !destination.isBlank())
                ? destination.trim()
                : "General";

        List<Document> docs = rawArticles.stream()
                .filter(content -> content != null && !content.isBlank())
                .map(content -> new Document(
                        UUID.randomUUID().toString(),
                        content.trim(),
                        Map.of("destination", resolvedDestination, "type", "travel_guide")
                ))
                .toList();

        if (docs.isEmpty()) {
            return 0;
        }

        vectorStore.add(docs);
        ingestedDocumentCount.addAndGet(docs.size());
        destinationDocumentCounts.merge(resolvedDestination, docs.size(), Integer::sum);

        log.info("Successfully ingested {} raw article documents for destination [{}] into VectorStore",
                docs.size(), resolvedDestination);
        return docs.size();
    }

    /**
     * Ingest documents from an uploaded file (PDF, JSON, TXT, or Markdown).
     */
    public synchronized int ingestFile(MultipartFile file, String fallbackDestination) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty.");
        }

        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        String resolvedDestination = (fallbackDestination != null && !fallbackDestination.isBlank())
                ? fallbackDestination.trim()
                : "General";

        // 1. PDF File Ingestion
        if (filename.endsWith(".pdf") || (file.getContentType() != null && file.getContentType().equalsIgnoreCase("application/pdf"))) {
            ByteArrayResource resource = new ByteArrayResource(file.getBytes(), file.getOriginalFilename());
            PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(
                    resource,
                    PdfDocumentReaderConfig.builder()
                            .withPageTopMargin(0)
                            .withPageBottomMargin(0)
                            .build()
            );

            List<Document> pageDocs = pdfReader.get();
            if (pageDocs.isEmpty()) {
                log.warn("No text content could be extracted from PDF: {}", file.getOriginalFilename());
                return 0;
            }

            TokenTextSplitter splitter = TokenTextSplitter.builder().build();
            List<Document> splitDocs = splitter.apply(pageDocs);

            List<Document> enrichedDocs = splitDocs.stream()
                    .filter(doc -> doc.getText() != null && !doc.getText().isBlank())
                    .map(doc -> {
                        Map<String, Object> metadata = new HashMap<>(doc.getMetadata());
                        metadata.put("destination", resolvedDestination);
                        metadata.put("type", "travel_guide");
                        metadata.put("source_filename", file.getOriginalFilename());

                        StringBuilder textBuilder = new StringBuilder();
                        textBuilder.append("Destination: ").append(resolvedDestination).append("\n");
                        textBuilder.append("Source: ").append(file.getOriginalFilename()).append("\n\n");
                        textBuilder.append(doc.getText().trim());

                        return new Document(
                                UUID.randomUUID().toString(),
                                textBuilder.toString(),
                                metadata
                        );
                    })
                    .toList();

            if (enrichedDocs.isEmpty()) {
                return 0;
            }

            vectorStore.add(enrichedDocs);
            ingestedDocumentCount.addAndGet(enrichedDocs.size());
            destinationDocumentCounts.merge(resolvedDestination, enrichedDocs.size(), Integer::sum);

            log.info("Successfully parsed, split, and ingested {} chunks from PDF [{}] for destination [{}]",
                    enrichedDocs.size(), file.getOriginalFilename(), resolvedDestination);
            return enrichedDocs.size();
        }

        // 2. JSON File Ingestion
        if (filename.endsWith(".json") || (file.getContentType() != null && file.getContentType().contains("json"))) {
            JsonNode rootNode;
            try (InputStream is = file.getInputStream()) {
                rootNode = objectMapper.readTree(is);
            }

            if (rootNode.isArray()) {
                if (!rootNode.isEmpty() && rootNode.get(0).isTextual()) {
                    List<String> articles = new ArrayList<>();
                    for (JsonNode node : rootNode) {
                        articles.add(node.asText());
                    }
                    return ingestDestinationKnowledge(articles, resolvedDestination);
                } else {
                    List<TravelDocumentDto> items = objectMapper.convertValue(
                            rootNode,
                            new TypeReference<List<TravelDocumentDto>>() {}
                    );
                    return ingestDocuments(items, resolvedDestination);
                }
            } else if (rootNode.isObject()) {
                String targetDest = rootNode.hasNonNull("destination")
                        ? rootNode.get("destination").asText()
                        : resolvedDestination;

                if (rootNode.has("documents") && rootNode.get("documents").isArray()) {
                    List<TravelDocumentDto> items = objectMapper.convertValue(
                            rootNode.get("documents"),
                            new TypeReference<List<TravelDocumentDto>>() {}
                    );
                    return ingestDocuments(items, targetDest);
                } else if (rootNode.has("articles") && rootNode.get("articles").isArray()) {
                    List<String> articles = new ArrayList<>();
                    for (JsonNode node : rootNode.get("articles")) {
                        articles.add(node.asText());
                    }
                    return ingestDestinationKnowledge(articles, targetDest);
                } else {
                    TravelDocumentDto singleDoc = objectMapper.convertValue(rootNode, TravelDocumentDto.class);
                    return ingestDocuments(List.of(singleDoc), targetDest);
                }
            } else {
                throw new IllegalArgumentException("Unsupported JSON structure for ingestion.");
            }
        }

        // 3. TXT / Markdown File Ingestion
        String text = new String(file.getBytes(), StandardCharsets.UTF_8);
        String[] sections = text.split("(?m)^\\s*$\\n+");
        List<String> articles = Arrays.stream(sections)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();

        if (articles.isEmpty()) {
            articles = List.of(text.trim());
        }

        return ingestDestinationKnowledge(articles, resolvedDestination);
    }

    /**
     * Ingest documents from a Spring Resource.
     */
    public synchronized int ingestResource(Resource resource, String fallbackDestination) {
        try {
            if (resource == null || !resource.exists()) {
                log.warn("Knowledge data resource not found: {}", resource);
                return 0;
            }

            List<TravelDocumentDto> items;
            try (InputStream is = resource.getInputStream()) {
                items = objectMapper.readValue(is, new TypeReference<List<TravelDocumentDto>>() {});
            }

            return ingestDocuments(items, fallbackDestination);
        } catch (Exception e) {
            log.error("Failed to ingest knowledge documents from resource", e);
            throw new RuntimeException("Knowledge ingestion failed: " + e.getMessage(), e);
        }
    }

    /**
     * Preloads curated destination knowledge from a classpath resource or file path.
     */
    public synchronized int ingestPreloadedKnowledge(String destination, String resourcePath) {
        String resolvedDest = (destination != null && !destination.isBlank()) ? destination.trim() : "General";
        String path = (resourcePath != null && !resourcePath.isBlank())
                ? resourcePath
                : "classpath:data/" + resolvedDest.toLowerCase() + "/" + resolvedDest.toLowerCase() + "_travel_knowledge.json";

        Resource resource = resourceLoader.getResource(path);
        return ingestResource(resource, resolvedDest);
    }

    /**
     * Perform similarity search on the VectorStore to inspect retrieved documents.
     */
    public List<Document> similaritySearch(String query, int topK) {
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(Math.max(1, topK))
                .build();
        return vectorStore.similaritySearch(request);
    }

    public boolean isDestinationIngested(String destination) {
        if (destination == null || destination.isBlank()) {
            return false;
        }
        return destinationDocumentCounts.entrySet().stream()
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
}
