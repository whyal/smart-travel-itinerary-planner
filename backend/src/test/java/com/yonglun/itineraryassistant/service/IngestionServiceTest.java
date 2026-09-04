package com.yonglun.itineraryassistant.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yonglun.itineraryassistant.dto.TravelDocumentDto;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IngestionServiceTest {

    @Mock
    private VectorStore vectorStore;

    @Mock
    private ResourceLoader mockResourceLoader;

    @Captor
    private ArgumentCaptor<List<Document>> documentsCaptor;

    @Captor
    private ArgumentCaptor<SearchRequest> searchRequestCaptor;

    private ResourceLoader resourceLoader;
    private ObjectMapper objectMapper;
    private IngestionService ingestionService;

    @BeforeEach
    void setUp() {
        resourceLoader = new DefaultResourceLoader();
        objectMapper = new ObjectMapper();
        ingestionService = new IngestionService(vectorStore, resourceLoader, objectMapper);
    }

    @Nested
    @DisplayName("ingestDocuments (Structured TravelDocumentDto)")
    class IngestDocumentsTests {

        @Test
        @DisplayName("Happy path: Ingest multiple full-featured documents and assert deep document fields & metadata")
        void testIngestStructuredDocuments_Success() {
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
            assertThat(ingestionService.getIngestedDocumentCount()).isEqualTo(2);
            assertThat(ingestionService.isDestinationIngested("Paris")).isTrue();
            assertThat(ingestionService.getIngestedDestinations()).containsExactly("Paris");
            assertThat(ingestionService.getDestinationDocumentCounts()).containsEntry("Paris", 2);

            verify(vectorStore, times(1)).add(documentsCaptor.capture());

            List<Document> captured = documentsCaptor.getValue();
            assertThat(captured).hasSize(2);

            Document capturedDoc1 = captured.get(0);
            assertThat(capturedDoc1.getId()).isEqualTo("paris-eiffel");
            assertThat(capturedDoc1.getText())
                    .contains("Destination: Paris")
                    .contains("Title: Eiffel Tower Guide")
                    .contains("District: 7th Arrondissement")
                    .contains("Category: attraction")
                    .contains("Best Time to Visit: Sunset")
                    .contains("Suggested Duration: 2-3 hours")
                    .contains("Iconic iron lattice tower on the Champ de Mars.");
            assertThat(capturedDoc1.getMetadata())
                    .containsEntry("destination", "Paris")
                    .containsEntry("doc_id", "paris-eiffel")
                    .containsEntry("type", "travel_guide")
                    .containsEntry("title", "Eiffel Tower Guide")
                    .containsEntry("category", "attraction")
                    .containsEntry("district", "7th Arrondissement")
                    .containsEntry("best_time_to_visit", "Sunset")
                    .containsEntry("suggested_duration", "2-3 hours")
                    .containsEntry("tags", "landmark, view")
                    .containsEntry("architect", "Gustave Eiffel");

            Document capturedDoc2 = captured.get(1);
            assertThat(capturedDoc2.getId()).isEqualTo("paris-louvre");
            assertThat(capturedDoc2.getText())
                    .contains("Destination: Paris")
                    .contains("Title: Louvre Museum")
                    .contains("District: 1st Arrondissement")
                    .contains("Category: museum")
                    .contains("Best Time to Visit: Wednesday or Friday evening")
                    .contains("Suggested Duration: 3-4 hours")
                    .contains("World's largest art museum and historic monument.");
            assertThat(capturedDoc2.getMetadata())
                    .containsEntry("destination", "Paris")
                    .containsEntry("doc_id", "paris-louvre")
                    .containsEntry("type", "travel_guide")
                    .containsEntry("tags", "art, mona-lisa");
        }

        @Test
        @DisplayName("Edge case: Uses default destination when item destination is missing and generates UUID for missing ID")
        void testIngestStructuredDocuments_DefaultDestinationAndGeneratedId() {
            TravelDocumentDto doc = new TravelDocumentDto(
                    null,
                    null,
                    "Big Ben",
                    "landmark",
                    null,
                    "Iconic clock tower in London.",
                    null,
                    null,
                    null,
                    null
            );

            int count = ingestionService.ingestDocuments(List.of(doc), "London");

            assertThat(count).isEqualTo(1);
            assertThat(ingestionService.isDestinationIngested("London")).isTrue();

            verify(vectorStore, times(1)).add(documentsCaptor.capture());
            List<Document> captured = documentsCaptor.getValue();
            assertThat(captured).hasSize(1);
            Document capturedDoc = captured.get(0);

            assertThat(capturedDoc.getId()).isNotBlank();
            assertThat(capturedDoc.getMetadata())
                    .containsEntry("destination", "London")
                    .containsEntry("doc_id", capturedDoc.getId())
                    .containsEntry("type", "travel_guide")
                    .containsEntry("title", "Big Ben")
                    .containsEntry("category", "landmark")
                    .doesNotContainKey("district")
                    .doesNotContainKey("tags");
            assertThat(capturedDoc.getText())
                    .contains("Destination: London")
                    .contains("Title: Big Ben")
                    .contains("Iconic clock tower in London.");
        }

        @Test
        @DisplayName("Edge case: Fallback destination to 'General' when item destination and default destination are blank")
        void testIngestStructuredDocuments_FallbackToGeneral() {
            TravelDocumentDto doc = new TravelDocumentDto(
                    "id-123",
                    "   ",
                    "Unknown Attraction",
                    null,
                    null,
                    "General travel tip",
                    null,
                    null,
                    null,
                    null
            );

            int count = ingestionService.ingestDocuments(List.of(doc), "  ");

            assertThat(count).isEqualTo(1);
            assertThat(ingestionService.isDestinationIngested("General")).isTrue();

            verify(vectorStore, times(1)).add(documentsCaptor.capture());
            Document capturedDoc = documentsCaptor.getValue().get(0);
            assertThat(capturedDoc.getMetadata()).containsEntry("destination", "General");
            assertThat(capturedDoc.getText()).startsWith("Destination: General");
        }

        @Test
        @DisplayName("Unhappy/Edge case: Null or empty items list returns 0 without calling vectorStore")
        void testIngestStructuredDocuments_NullOrEmptyList() {
            int countNull = ingestionService.ingestDocuments(null, "Tokyo");
            int countEmpty = ingestionService.ingestDocuments(Collections.emptyList(), "Tokyo");

            assertThat(countNull).isZero();
            assertThat(countEmpty).isZero();
            assertThat(ingestionService.getIngestedDocumentCount()).isZero();
            verifyNoInteractions(vectorStore);
        }

        @Test
        @DisplayName("Edge case: List containing null elements filters them out gracefully")
        void testIngestStructuredDocuments_ListWithNullElements() {
            List<TravelDocumentDto> list = Collections.singletonList(null);

            int count = ingestionService.ingestDocuments(list, "Rome");

            assertThat(count).isZero();
            assertThat(ingestionService.getIngestedDocumentCount()).isZero();
            verifyNoInteractions(vectorStore);
        }
    }

    @Nested
    @DisplayName("ingestDestinationKnowledge (Raw Articles)")
    class IngestDestinationKnowledgeTests {

        @Test
        @DisplayName("Happy path: Ingest raw article strings and assert captured documents")
        void testIngestDestinationKnowledge_Success() {
            List<String> rawArticles = List.of(
                    "Article 1 about Osaka street food in Dotonbori",
                    "Article 2 about Osaka Castle and gardens"
            );

            int count = ingestionService.ingestDestinationKnowledge(rawArticles, "Osaka");

            assertThat(count).isEqualTo(2);
            assertThat(ingestionService.isDestinationIngested("Osaka")).isTrue();
            assertThat(ingestionService.getIngestedDocumentCount()).isEqualTo(2);

            verify(vectorStore, times(1)).add(documentsCaptor.capture());
            List<Document> capturedDocs = documentsCaptor.getValue();
            assertThat(capturedDocs).hasSize(2);

            assertThat(capturedDocs.get(0).getId()).isNotBlank();
            assertThat(capturedDocs.get(0).getText()).isEqualTo("Article 1 about Osaka street food in Dotonbori");
            assertThat(capturedDocs.get(0).getMetadata())
                    .containsEntry("destination", "Osaka")
                    .containsEntry("type", "travel_guide");

            assertThat(capturedDocs.get(1).getId()).isNotBlank();
            assertThat(capturedDocs.get(1).getText()).isEqualTo("Article 2 about Osaka Castle and gardens");
            assertThat(capturedDocs.get(1).getMetadata())
                    .containsEntry("destination", "Osaka")
                    .containsEntry("type", "travel_guide");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("Edge case: Blank destination defaults to 'General'")
        void testIngestDestinationKnowledge_BlankDestinationDefaultsToGeneral(String destination) {
            int count = ingestionService.ingestDestinationKnowledge(List.of("Exploring the world"), destination);

            assertThat(count).isEqualTo(1);
            assertThat(ingestionService.isDestinationIngested("General")).isTrue();

            verify(vectorStore, times(1)).add(documentsCaptor.capture());
            Document doc = documentsCaptor.getValue().get(0);
            assertThat(doc.getMetadata()).containsEntry("destination", "General");
        }

        @Test
        @DisplayName("Edge case: Filter out empty or blank strings from raw articles")
        void testIngestDestinationKnowledge_FiltersBlankArticles() {
            List<String> rawArticles = List.of("Valid article", "   ", "", "Another valid article");

            int count = ingestionService.ingestDestinationKnowledge(rawArticles, "Kyoto");

            assertThat(count).isEqualTo(2);
            verify(vectorStore, times(1)).add(documentsCaptor.capture());
            List<Document> captured = documentsCaptor.getValue();
            assertThat(captured).hasSize(2);
            assertThat(captured.get(0).getText()).isEqualTo("Valid article");
            assertThat(captured.get(1).getText()).isEqualTo("Another valid article");
        }

        @Test
        @DisplayName("Unhappy/Edge case: Null or empty articles list returns 0 without calling vectorStore")
        void testIngestDestinationKnowledge_NullOrEmptyList() {
            int countNull = ingestionService.ingestDestinationKnowledge(null, "Kyoto");
            int countEmpty = ingestionService.ingestDestinationKnowledge(List.of(), "Kyoto");

            assertThat(countNull).isZero();
            assertThat(countEmpty).isZero();
            verifyNoInteractions(vectorStore);
        }

        @Test
        @DisplayName("Unhappy/Edge case: Articles list containing only blank strings stores nothing")
        void testIngestDestinationKnowledge_OnlyBlankStrings() {
            int count = ingestionService.ingestDestinationKnowledge(List.of("  ", "   ", " "), "Kyoto");

            assertThat(count).isZero();
            verifyNoInteractions(vectorStore);
        }
    }

    @Nested
    @DisplayName("ingestFile (Multipart File Uploads)")
    class IngestFileTests {

        @Test
        @DisplayName("Happy path: JSON file with array of TravelDocumentDto objects")
        void testIngestJsonFile_ArrayOfObjects() throws Exception {
            String jsonContent = """
                    [
                      {
                        "id": "tokyo-shibuya",
                        "destination": "Tokyo",
                        "title": "Shibuya Crossing",
                        "category": "attraction",
                        "district": "Shibuya",
                        "content": "Famous scramble crossing in front of Shibuya Station.",
                        "tags": ["crossing", "shopping"],
                        "suggestedDuration": "1 hour",
                        "bestTimeToVisit": "Evening",
                        "metadata": {"crowd_level": "high"}
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

            assertThat(count).isEqualTo(1);
            assertThat(ingestionService.isDestinationIngested("Tokyo")).isTrue();

            verify(vectorStore, times(1)).add(documentsCaptor.capture());
            List<Document> docs = documentsCaptor.getValue();
            assertThat(docs).hasSize(1);
            Document doc = docs.get(0);
            assertThat(doc.getId()).isEqualTo("tokyo-shibuya");
            assertThat(doc.getText())
                    .contains("Destination: Tokyo")
                    .contains("Title: Shibuya Crossing")
                    .contains("Famous scramble crossing");
            assertThat(doc.getMetadata())
                    .containsEntry("destination", "Tokyo")
                    .containsEntry("crowd_level", "high")
                    .containsEntry("tags", "crossing, shopping");
        }

        @Test
        @DisplayName("Happy path: JSON file with array of raw strings")
        void testIngestJsonFile_ArrayOfStrings() throws Exception {
            String jsonContent = """
                    [
                      "Kyoto is famous for historic temples and gardens.",
                      "Arashiyama bamboo grove is best visited early morning."
                    ]
                    """;

            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "kyoto_articles.json",
                    "application/json",
                    jsonContent.getBytes(StandardCharsets.UTF_8)
            );

            int count = ingestionService.ingestFile(file, "Kyoto");

            assertThat(count).isEqualTo(2);
            verify(vectorStore, times(1)).add(documentsCaptor.capture());
            List<Document> docs = documentsCaptor.getValue();
            assertThat(docs).hasSize(2);
            assertThat(docs.get(0).getText()).isEqualTo("Kyoto is famous for historic temples and gardens.");
            assertThat(docs.get(0).getMetadata()).containsEntry("destination", "Kyoto");
            assertThat(docs.get(1).getText()).isEqualTo("Arashiyama bamboo grove is best visited early morning.");
        }

        @Test
        @DisplayName("Happy path: JSON object with 'documents' wrapper")
        void testIngestJsonFile_ObjectWithDocumentsWrapper() throws Exception {
            String jsonContent = """
                    {
                      "destination": "Seoul",
                      "documents": [
                        {
                          "id": "seoul-tower",
                          "title": "N Seoul Tower",
                          "content": "Panoramic views of Seoul from Namsan Mountain."
                        }
                      ]
                    }
                    """;

            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "seoul.json",
                    "application/json",
                    jsonContent.getBytes(StandardCharsets.UTF_8)
            );

            int count = ingestionService.ingestFile(file, "Fallback");

            assertThat(count).isEqualTo(1);
            assertThat(ingestionService.isDestinationIngested("Seoul")).isTrue();

            verify(vectorStore, times(1)).add(documentsCaptor.capture());
            Document doc = documentsCaptor.getValue().get(0);
            assertThat(doc.getId()).isEqualTo("seoul-tower");
            assertThat(doc.getMetadata()).containsEntry("destination", "Seoul");
        }

        @Test
        @DisplayName("Happy path: JSON object with 'articles' wrapper")
        void testIngestJsonFile_ObjectWithArticlesWrapper() throws Exception {
            String jsonContent = """
                    {
                      "destination": "Busan",
                      "articles": [
                        "Haeundae Beach is popular in summer.",
                        "Gamcheon Culture Village features colorful houses."
                      ]
                    }
                    """;

            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "busan.json",
                    "application/json",
                    jsonContent.getBytes(StandardCharsets.UTF_8)
            );

            int count = ingestionService.ingestFile(file, "Fallback");

            assertThat(count).isEqualTo(2);
            assertThat(ingestionService.isDestinationIngested("Busan")).isTrue();

            verify(vectorStore, times(1)).add(documentsCaptor.capture());
            List<Document> docs = documentsCaptor.getValue();
            assertThat(docs).hasSize(2);
            assertThat(docs.get(0).getMetadata()).containsEntry("destination", "Busan");
            assertThat(docs.get(1).getMetadata()).containsEntry("destination", "Busan");
        }

        @Test
        @DisplayName("Happy path: JSON object representing single TravelDocumentDto")
        void testIngestJsonFile_SingleObject() throws Exception {
            String jsonContent = """
                    {
                      "id": "london-eye",
                      "title": "London Eye",
                      "content": "Observation wheel on the South Bank of the River Thames."
                    }
                    """;

            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "london_eye.json",
                    "application/json",
                    jsonContent.getBytes(StandardCharsets.UTF_8)
            );

            int count = ingestionService.ingestFile(file, "London");

            assertThat(count).isEqualTo(1);
            verify(vectorStore, times(1)).add(documentsCaptor.capture());
            Document doc = documentsCaptor.getValue().get(0);
            assertThat(doc.getId()).isEqualTo("london-eye");
            assertThat(doc.getMetadata()).containsEntry("destination", "London");
        }

        @Test
        @DisplayName("Happy path: Text file split across multiple paragraphs")
        void testIngestTextFile_MultiParagraphs() throws Exception {
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

            verify(vectorStore, times(1)).add(documentsCaptor.capture());
            List<Document> docs = documentsCaptor.getValue();
            assertThat(docs).hasSize(2);
            assertThat(docs.get(0).getText()).contains("Rome Colosseum Guide");
            assertThat(docs.get(0).getMetadata()).containsEntry("destination", "Rome");
            assertThat(docs.get(1).getText()).contains("Vatican Museums");
            assertThat(docs.get(1).getMetadata()).containsEntry("destination", "Rome");
        }

        @Test
        @DisplayName("Happy path: PDF file upload and content extraction")
        void testIngestPdfFile_Success() throws Exception {
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

            verify(vectorStore, times(1)).add(documentsCaptor.capture());
            List<Document> capturedDocs = documentsCaptor.getValue();
            assertThat(capturedDocs).isNotEmpty();
            Document capturedDoc = capturedDocs.get(0);
            assertThat(capturedDoc.getText())
                    .contains("Destination: Barcelona")
                    .contains("Source: barcelona_guide.pdf")
                    .contains("Barcelona", "Sagrada", "Familia");
            assertThat(capturedDoc.getMetadata())
                    .containsEntry("destination", "Barcelona")
                    .containsEntry("type", "travel_guide")
                    .containsEntry("source_filename", "barcelona_guide.pdf");
        }

        @Test
        @DisplayName("Unhappy path: Null file throws IllegalArgumentException")
        void testIngestFile_NullFile_ThrowsException() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> ingestionService.ingestFile(null, "Tokyo"));
            assertThat(ex.getMessage()).contains("File cannot be empty");
            verifyNoInteractions(vectorStore);
        }

        @Test
        @DisplayName("Unhappy path: Empty file throws IllegalArgumentException")
        void testIngestFile_EmptyFile_ThrowsException() {
            MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> ingestionService.ingestFile(emptyFile, "Tokyo"));
            assertThat(ex.getMessage()).contains("File cannot be empty");
            verifyNoInteractions(vectorStore);
        }

        @Test
        @DisplayName("Unhappy path: Malformed JSON throws Jackson/IO Exception")
        void testIngestFile_MalformedJson_ThrowsException() {
            MockMultipartFile invalidJsonFile = new MockMultipartFile(
                    "file",
                    "invalid.json",
                    "application/json",
                    "{ invalid json content ]".getBytes(StandardCharsets.UTF_8)
            );

            assertThrows(Exception.class, () -> ingestionService.ingestFile(invalidJsonFile, "Tokyo"));
            verifyNoInteractions(vectorStore);
        }

        @Test
        @DisplayName("Unhappy path: Unsupported JSON structure (e.g. primitive number/boolean) throws IllegalArgumentException")
        void testIngestFile_UnsupportedJsonStructure_ThrowsException() {
            MockMultipartFile invalidStructureFile = new MockMultipartFile(
                    "file",
                    "primitive.json",
                    "application/json",
                    "12345".getBytes(StandardCharsets.UTF_8)
            );

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> ingestionService.ingestFile(invalidStructureFile, "Tokyo"));
            assertThat(ex.getMessage()).contains("Unsupported JSON structure");
            verifyNoInteractions(vectorStore);
        }
    }

    @Nested
    @DisplayName("ingestResource & ingestPreloadedKnowledge")
    class IngestResourceTests {

        @Test
        @DisplayName("Happy path: ingestResource reads valid JSON resource and stores documents")
        void testIngestResource_ValidResource() {
            String jsonContent = """
                    [
                      {
                        "id": "kyoto-kinkakuji",
                        "destination": "Kyoto",
                        "title": "Kinkaku-ji",
                        "category": "temple",
                        "content": "Zen Buddhist temple covered in gold leaf."
                      }
                    ]
                    """;
            Resource validResource = new ByteArrayResource(jsonContent.getBytes(StandardCharsets.UTF_8)) {
                @Override
                public boolean exists() {
                    return true;
                }
            };

            int count = ingestionService.ingestResource(validResource, "Kyoto");

            assertThat(count).isEqualTo(1);
            assertThat(ingestionService.isDestinationIngested("Kyoto")).isTrue();

            verify(vectorStore, times(1)).add(documentsCaptor.capture());
            Document doc = documentsCaptor.getValue().get(0);
            assertThat(doc.getId()).isEqualTo("kyoto-kinkakuji");
            assertThat(doc.getText()).contains("Kinkaku-ji");
            assertThat(doc.getMetadata()).containsEntry("destination", "Kyoto");
        }

        @Test
        @DisplayName("Unhappy/Edge path: Null or non-existent resource returns 0 without calling vectorStore")
        void testIngestResource_NonExistentResource() {
            Resource nonExistentResource = mock(Resource.class);
            when(nonExistentResource.exists()).thenReturn(false);

            int countNull = ingestionService.ingestResource(null, "Kyoto");
            int countNonExistent = ingestionService.ingestResource(nonExistentResource, "Kyoto");

            assertThat(countNull).isZero();
            assertThat(countNonExistent).isZero();
            verifyNoInteractions(vectorStore);
        }

        @Test
        @DisplayName("Unhappy path: Ingest resource throwing IOException wrapped in RuntimeException")
        void testIngestResource_IOException_ThrowsRuntimeException() throws Exception {
            Resource failingResource = mock(Resource.class);
            when(failingResource.exists()).thenReturn(true);
            when(failingResource.getInputStream()).thenThrow(new IOException("Simulated disk error"));

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> ingestionService.ingestResource(failingResource, "Kyoto"));

            assertThat(ex.getMessage()).contains("Knowledge ingestion failed");
            verifyNoInteractions(vectorStore);
        }

        @Test
        @DisplayName("Happy path: ingestPreloadedKnowledge resolves resource path and ingests correctly")
        void testIngestPreloadedKnowledge_CustomAndDefaultPath() {
            IngestionService customService = new IngestionService(vectorStore, mockResourceLoader, objectMapper);

            String jsonContent = """
                    [
                      {
                        "id": "tokyo-skytree",
                        "title": "Tokyo Skytree",
                        "content": "Broadcasting and observation tower."
                      }
                    ]
                    """;
            Resource mockRes = new ByteArrayResource(jsonContent.getBytes(StandardCharsets.UTF_8)) {
                @Override
                public boolean exists() {
                    return true;
                }
            };

            when(mockResourceLoader.getResource("classpath:data/tokyo/tokyo_travel_knowledge.json"))
                    .thenReturn(mockRes);

            int count = customService.ingestPreloadedKnowledge("Tokyo", null);

            assertThat(count).isEqualTo(1);
            assertThat(customService.isDestinationIngested("Tokyo")).isTrue();

            verify(mockResourceLoader).getResource("classpath:data/tokyo/tokyo_travel_knowledge.json");
            verify(vectorStore, times(1)).add(documentsCaptor.capture());
            Document doc = documentsCaptor.getValue().get(0);
            assertThat(doc.getId()).isEqualTo("tokyo-skytree");
            assertThat(doc.getMetadata()).containsEntry("destination", "Tokyo");
        }
    }

    @Nested
    @DisplayName("similaritySearch")
    class SimilaritySearchTests {

        @Test
        @DisplayName("Happy path: Captures SearchRequest and asserts query and topK parameters")
        void testSimilaritySearch_VerifySearchRequestParameters() {
            Document mockDoc = new Document("doc-1", "Fushimi Inari Guide", Map.of("destination", "Kyoto"));
            when(vectorStore.similaritySearch(searchRequestCaptor.capture())).thenReturn(List.of(mockDoc));

            List<Document> results = ingestionService.similaritySearch("shrine in Kyoto", 5);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getId()).isEqualTo("doc-1");
            assertThat(results.get(0).getText()).contains("Fushimi Inari Guide");

            SearchRequest capturedRequest = searchRequestCaptor.getValue();
            assertThat(capturedRequest.getQuery()).isEqualTo("shrine in Kyoto");
            assertThat(capturedRequest.getTopK()).isEqualTo(5);
        }

        @Test
        @DisplayName("Edge case: topK <= 0 is coerced to minimum of 1 in SearchRequest")
        void testSimilaritySearch_NegativeOrZeroTopK_CoercedToOne() {
            when(vectorStore.similaritySearch(searchRequestCaptor.capture())).thenReturn(List.of());

            ingestionService.similaritySearch("ramen", 0);
            assertThat(searchRequestCaptor.getValue().getTopK()).isEqualTo(1);

            ingestionService.similaritySearch("ramen", -10);
            assertThat(searchRequestCaptor.getValue().getTopK()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("State and destination inquiry methods")
    class StateQueryTests {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "UnknownCity"})
        @DisplayName("Edge case: isDestinationIngested returns false for null, blank, or unknown destinations")
        void testIsDestinationIngested_ReturnsFalseForUnregistered(String destination) {
            assertThat(ingestionService.isDestinationIngested(destination)).isFalse();
        }

        @Test
        @DisplayName("Happy path: Destination lookup is case-insensitive")
        void testIsDestinationIngested_CaseInsensitive() {
            TravelDocumentDto doc = new TravelDocumentDto(
                    "rome-1", "Rome", "Colosseum", "attraction", null, "Ancient arena", null, null, null, null
            );
            ingestionService.ingestDocuments(List.of(doc));

            assertThat(ingestionService.isDestinationIngested("rome")).isTrue();
            assertThat(ingestionService.isDestinationIngested("ROME")).isTrue();
            assertThat(ingestionService.isDestinationIngested(" RoMe ")).isTrue();
        }
    }
}
