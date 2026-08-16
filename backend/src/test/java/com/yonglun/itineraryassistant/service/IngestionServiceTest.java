package com.yonglun.itineraryassistant.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yonglun.itineraryassistant.dto.TravelDocumentDto;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
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
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

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
    void testIngestStructuredDocuments() {
        TravelDocumentDto doc1 = new TravelDocumentDto(
                "paris-eiffel",
                "Paris",
                "Eiffel Tower Guide",
                "attraction",
                "7th Arrondissement",
                "Iconic iron lattice tower on the Champ de Mars.",
                List.of("landmark", "view"),
                "2-3 hours",
                "Sunset",
                Map.of("architect", "Gustave Eiffel")
        );
        TravelDocumentDto doc2 = new TravelDocumentDto(
                "paris-louvre",
                "Paris",
                "Louvre Museum",
                "museum",
                "1st Arrondissement",
                "World's largest art museum and historic monument.",
                List.of("art", "mona-lisa"),
                "3-4 hours",
                "Wednesday or Friday evening",
                Map.of()
        );

        int count = ingestionService.ingestDocuments(List.of(doc1, doc2));

        assertThat(count).isEqualTo(2);
        assertThat(ingestionService.isDestinationIngested("Paris")).isTrue();
        assertThat(ingestionService.getIngestedDestinations()).contains("Paris");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore, times(1)).add(captor.capture());

        List<Document> captured = captor.getValue();
        assertThat(captured).hasSize(2);
        assertThat(captured.get(0).getId()).isEqualTo("paris-eiffel");
        assertThat(captured.get(0).getText()).contains("Destination: Paris", "Eiffel Tower Guide", "Champ de Mars");
        assertThat(captured.get(0).getMetadata())
                .containsEntry("destination", "Paris")
                .containsEntry("title", "Eiffel Tower Guide")
                .containsEntry("category", "attraction")
                .containsEntry("architect", "Gustave Eiffel");
    }

    @Test
    void testIngestDestinationKnowledge() {
        List<String> rawArticles = List.of(
                "Article 1 about Osaka street food in Dotonbori",
                "Article 2 about Osaka Castle and gardens"
        );

        int count = ingestionService.ingestDestinationKnowledge(rawArticles, "Osaka");

        assertThat(count).isEqualTo(2);
        assertThat(ingestionService.isDestinationIngested("Osaka")).isTrue();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore, times(1)).add(captor.capture());

        List<Document> capturedDocs = captor.getValue();
        assertThat(capturedDocs).hasSize(2);
        assertThat(capturedDocs.get(0).getText()).contains("Osaka street food");
        assertThat(capturedDocs.get(0).getMetadata()).containsEntry("destination", "Osaka");
    }

    @Test
    void testIngestJsonFileUpload() throws Exception {
        String jsonContent = """
                [
                  {
                    "id": "tokyo-shibuya",
                    "destination": "Tokyo",
                    "title": "Shibuya Crossing",
                    "category": "attraction",
                    "district": "Shibuya",
                    "content": "Famous scramble crossing in front of Shibuya Station.",
                    "tags": ["crossing", "shopping"]
                  },
                  {
                    "id": "tokyo-sensoji",
                    "destination": "Tokyo",
                    "title": "Senso-ji Temple",
                    "category": "attraction",
                    "district": "Asakusa",
                    "content": "Tokyo's oldest and most significant ancient Buddhist temple.",
                    "tags": ["temple", "asakusa"]
                  }
                ]
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "tokyo_knowledge.json",
                "application/json",
                jsonContent.getBytes(StandardCharsets.UTF_8)
        );

        int count = ingestionService.ingestFile(file, "Tokyo");

        assertThat(count).isEqualTo(2);
        assertThat(ingestionService.isDestinationIngested("Tokyo")).isTrue();
    }

    @Test
    void testIngestTextFileUpload() throws Exception {
        String textContent = """
                Rome Colosseum Guide:
                The Colosseum is an oval amphitheatre in the centre of the city of Rome, Italy.

                Vatican Museums:
                The Vatican Museums are the public museums of the Vatican City.
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "rome_notes.txt",
                "text/plain",
                textContent.getBytes(StandardCharsets.UTF_8)
        );

        int count = ingestionService.ingestFile(file, "Rome");

        assertThat(count).isEqualTo(2);
        assertThat(ingestionService.isDestinationIngested("Rome")).isTrue();
    }

    @Test
    void testIngestPdfFileUpload() throws Exception {
        byte[] pdfBytes;
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(doc, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12);
                content.newLineAtOffset(50, 700);
                content.showText("Barcelona Sagrada Familia travel overview and visitor tips.");
                content.endText();
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            pdfBytes = baos.toByteArray();
        }

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "barcelona_guide.pdf",
                "application/pdf",
                pdfBytes
        );

        int count = ingestionService.ingestFile(file, "Barcelona");

        assertThat(count).isGreaterThan(0);
        assertThat(ingestionService.isDestinationIngested("Barcelona")).isTrue();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore, times(1)).add(captor.capture());

        List<Document> capturedDocs = captor.getValue();
        assertThat(capturedDocs).isNotEmpty();
        assertThat(capturedDocs.get(0).getText()).contains("Barcelona", "Sagrada", "Familia");
        assertThat(capturedDocs.get(0).getMetadata()).containsEntry("destination", "Barcelona");
    }

    @Test
    void testSimilaritySearch() {
        Document mockDoc = new Document("doc-1", "Fushimi Inari Guide", Map.of("destination", "Kyoto"));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(mockDoc));

        List<Document> results = ingestionService.similaritySearch("temple", 2);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getText()).contains("Fushimi Inari");
    }
}
