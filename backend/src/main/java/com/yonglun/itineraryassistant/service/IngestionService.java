package com.yonglun.itineraryassistant.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class IngestionService {
    private final VectorStore vectorStore;

    public IngestionService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public void ingestDestinationKnowledge(List<String> rawArticles, String destination) {
        List<Document> docs = rawArticles.stream()
                .map(content -> new Document(
                        content,
                        Map.of("destination", destination, "type", "travel_guide")
                ))
                .toList();

        // VectorStore automatically handles embedding generation via the default EmbeddingModel
        vectorStore.add(docs);
    }
}
