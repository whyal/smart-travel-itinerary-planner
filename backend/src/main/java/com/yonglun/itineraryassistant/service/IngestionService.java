package com.yonglun.itineraryassistant.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);
    public static final String KYOTO_DATA_PATH = "classpath:data/kyoto/kyoto_travel_knowledge.json";

    private final VectorStore vectorStore;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;

    private final AtomicBoolean kyotoIngested = new AtomicBoolean(false);
    private final AtomicInteger ingestedDocumentCount = new AtomicInteger(0);

    @org.springframework.beans.factory.annotation.Autowired
    public IngestionService(VectorStore vectorStore, ResourceLoader resourceLoader, ObjectMapper objectMapper) {
        this.vectorStore = vectorStore;
        this.resourceLoader = resourceLoader;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    /**
     * Loads curated Kyoto travel knowledge documents from classpath JSON and embeds them into the VectorStore.
     *
     * @return Number of documents successfully ingested.
     */
    public synchronized int ingestKyotoKnowledge() {
        try {
            Resource resource = resourceLoader.getResource(KYOTO_DATA_PATH);
            if (!resource.exists()) {
                log.warn("Kyoto knowledge data file not found at: {}", KYOTO_DATA_PATH);
                return 0;
            }

            List<KyotoDocumentItem> items;
            try (InputStream is = resource.getInputStream()) {
                items = objectMapper.readValue(is, new TypeReference<List<KyotoDocumentItem>>() {});
            }

            if (items == null || items.isEmpty()) {
                log.warn("No Kyoto document items found in dataset");
                return 0;
            }

            List<Document> documents = new ArrayList<>();
            for (KyotoDocumentItem item : items) {
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("destination", "Kyoto");
                metadata.put("doc_id", item.id());
                metadata.put("title", item.title());
                metadata.put("category", item.category());
                metadata.put("district", item.district());
                if (item.bestTimeToVisit() != null) {
                    metadata.put("best_time_to_visit", item.bestTimeToVisit());
                }
                if (item.suggestedDuration() != null) {
                    metadata.put("suggested_duration", item.suggestedDuration());
                }
                if (item.tags() != null && !item.tags().isEmpty()) {
                    metadata.put("tags", String.join(", ", item.tags()));
                }
                metadata.put("type", "travel_guide");

                StringBuilder contentBuilder = new StringBuilder();
                contentBuilder.append("Destination: Kyoto\n");
                contentBuilder.append("Title: ").append(item.title()).append("\n");
                contentBuilder.append("District: ").append(item.district()).append("\n");
                contentBuilder.append("Category: ").append(item.category()).append("\n");
                if (item.bestTimeToVisit() != null) {
                    contentBuilder.append("Best Time to Visit: ").append(item.bestTimeToVisit()).append("\n");
                }
                if (item.suggestedDuration() != null) {
                    contentBuilder.append("Suggested Duration: ").append(item.suggestedDuration()).append("\n");
                }
                contentBuilder.append("\n").append(item.content());

                Document doc = new Document(item.id(), contentBuilder.toString(), metadata);
                documents.add(doc);
            }

            vectorStore.add(documents);
            kyotoIngested.set(true);
            ingestedDocumentCount.addAndGet(documents.size());
            log.info("Successfully ingested {} Kyoto knowledge documents into VectorStore", documents.size());
            return documents.size();
        } catch (Exception e) {
            log.error("Failed to ingest Kyoto knowledge documents", e);
            throw new RuntimeException("Kyoto knowledge ingestion failed: " + e.getMessage(), e);
        }
    }

    /**
     * Ingest raw article strings for a specific destination into the VectorStore.
     */
    public void ingestDestinationKnowledge(List<String> rawArticles, String destination) {
        if (rawArticles == null || rawArticles.isEmpty()) {
            return;
        }

        List<Document> docs = rawArticles.stream()
                .map(content -> new Document(
                        content,
                        Map.of("destination", destination, "type", "travel_guide")
                ))
                .toList();

        vectorStore.add(docs);
        ingestedDocumentCount.addAndGet(docs.size());
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

    public boolean isKyotoIngested() {
        return kyotoIngested.get();
    }

    public int getIngestedDocumentCount() {
        return ingestedDocumentCount.get();
    }

    public record KyotoDocumentItem(
            String id,
            String title,
            String category,
            String district,
            String content,
            List<String> tags,
            String suggestedDuration,
            String bestTimeToVisit
    ) {}
}
