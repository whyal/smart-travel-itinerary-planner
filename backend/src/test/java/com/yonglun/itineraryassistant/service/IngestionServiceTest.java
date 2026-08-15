package com.yonglun.itineraryassistant.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IngestionServiceTest {

    @Mock
    private VectorStore vectorStore;

    private ResourceLoader resourceLoader;
    private ObjectMapper objectMapper;
    private IngestionService ingestionService;

    @BeforeEach
    void setUp() {
        resourceLoader = new DefaultResourceLoader();
        objectMapper = new ObjectMapper();
        ingestionService = new IngestionService(vectorStore, resourceLoader, objectMapper);
    }

    @Test
    void testIngestKyotoKnowledge() {
        int count = ingestionService.ingestKyotoKnowledge();

        assertThat(count).isGreaterThan(0);
        assertThat(ingestionService.isKyotoIngested()).isTrue();
        assertThat(ingestionService.getIngestedDocumentCount()).isEqualTo(count);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore, times(1)).add(captor.capture());

        List<Document> capturedDocs = captor.getValue();
        assertThat(capturedDocs).hasSize(count);

        // Verify structure of first document
        Document firstDoc = capturedDocs.getFirst();
        assertThat(firstDoc.getId()).isNotBlank();
        assertThat(firstDoc.getText()).contains("Kyoto");
        assertThat(firstDoc.getMetadata())
                .containsEntry("destination", "Kyoto")
                .containsKey("category")
                .containsKey("district")
                .containsKey("title");
    }

    @Test
    void testIngestDestinationKnowledge() {
        List<String> rawArticles = List.of(
                "Article 1 about Osaka street food in Dotonbori",
                "Article 2 about Osaka Castle and gardens"
        );

        ingestionService.ingestDestinationKnowledge(rawArticles, "Osaka");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore, times(1)).add(captor.capture());

        List<Document> capturedDocs = captor.getValue();
        assertThat(capturedDocs).hasSize(2);
        assertThat(capturedDocs.get(0).getText()).contains("Osaka street food");
        assertThat(capturedDocs.get(0).getMetadata()).containsEntry("destination", "Osaka");
    }

    @Test
    void testSimilaritySearch() {
        Document mockDoc = new Document("doc-1", "Fushimi Inari Guide", java.util.Map.of("destination", "Kyoto"));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(mockDoc));

        List<Document> results = ingestionService.similaritySearch("temple", 2);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getText()).contains("Fushimi Inari");
    }
}
