package com.yonglun.itineraryassistant.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yonglun.itineraryassistant.dto.CustomIngestionRequest;
import com.yonglun.itineraryassistant.dto.DocumentBatchIngestionRequest;
import com.yonglun.itineraryassistant.dto.TravelDocumentDto;
import com.yonglun.itineraryassistant.service.IngestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class KnowledgeControllerTest {

    @Mock
    private IngestionService ingestionService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(new KnowledgeController(ingestionService)).build();
    }

    @Test
    void testIngestDocumentsEndpoint() throws Exception {
        TravelDocumentDto doc = new TravelDocumentDto(
                "doc-1",
                "London",
                "Tower of London",
                "history",
                "Central London",
                "Historic castle on the north bank of the River Thames.",
                List.of("castle", "crown-jewels"),
                "3 hours",
                "Morning",
                Map.of()
        );

        when(ingestionService.ingestDocuments(anyList(), any())).thenReturn(1);

        mockMvc.perform(post("/api/knowledge/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(doc))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.destination", is("London")))
                .andExpect(jsonPath("$.documentsIngested", is(1)));
    }

    @Test
    void testIngestDocumentsEmptyValidation() throws Exception {
        mockMvc.perform(post("/api/knowledge/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("error")));
    }

    @Test
    void testIngestBatchEndpoint() throws Exception {
        TravelDocumentDto doc = new TravelDocumentDto(
                "doc-2",
                "Paris",
                "Louvre",
                "museum",
                "1st Arr.",
                "Art museum",
                List.of("art"),
                "3 hours",
                "Evening",
                Map.of()
        );
        DocumentBatchIngestionRequest batch = new DocumentBatchIngestionRequest("Paris", List.of(doc));

        when(ingestionService.ingestDocuments(anyList(), eq("Paris"))).thenReturn(1);

        mockMvc.perform(post("/api/knowledge/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batch)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.destination", is("Paris")))
                .andExpect(jsonPath("$.documentsIngested", is(1)));
    }

    @Test
    void testIngestArticlesEndpoint() throws Exception {
        CustomIngestionRequest request = new CustomIngestionRequest(
                "Seoul",
                List.of("Gyeongbokgung Palace guide", "Myeongdong street food guide")
        );

        when(ingestionService.ingestDestinationKnowledge(anyList(), eq("Seoul"))).thenReturn(2);

        mockMvc.perform(post("/api/knowledge/articles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.destination", is("Seoul")))
                .andExpect(jsonPath("$.documentsIngested", is(2)));
    }

    @Test
    void testUploadFileEndpoint() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "paris_knowledge.json",
                "application/json",
                "[{}]".getBytes(StandardCharsets.UTF_8)
        );

        when(ingestionService.ingestFile(any(), eq("Paris"))).thenReturn(1);

        mockMvc.perform(multipart("/api/knowledge/upload")
                        .file(file)
                        .param("destination", "Paris"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.documentsIngested", is(1)));
    }

    @Test
    void testUploadEmptyFileReturnsBadRequest() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.txt",
                "text/plain",
                new byte[0]
        );

        mockMvc.perform(multipart("/api/knowledge/upload")
                        .file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("error")));
    }

    @Test
    void testPreloadEndpoint() throws Exception {
        when(ingestionService.ingestPreloadedKnowledge(eq("Kyoto"), any())).thenReturn(10);

        mockMvc.perform(post("/api/knowledge/preload")
                        .param("destination", "Kyoto"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.destination", is("Kyoto")))
                .andExpect(jsonPath("$.documentsIngested", is(10)));
    }

    @Test
    void testStatusEndpoint() throws Exception {
        when(ingestionService.getIngestedDocumentCount()).thenReturn(15);
        when(ingestionService.getIngestedDestinations()).thenReturn(Set.of("Kyoto", "Paris"));
        when(ingestionService.getDestinationDocumentCounts()).thenReturn(Map.of("Kyoto", 10, "Paris", 5));

        mockMvc.perform(get("/api/knowledge/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ready")))
                .andExpect(jsonPath("$.totalDocumentsIngested", is(15)))
                .andExpect(jsonPath("$.destinations", hasItems("Kyoto", "Paris")))
                .andExpect(jsonPath("$.documentCountByDestination.Kyoto", is(10)))
                .andExpect(jsonPath("$.documentCountByDestination.Paris", is(5)));
    }

    @Test
    void testSimilaritySearchEndpoint() throws Exception {
        Document doc = new Document("london-eye", "London Eye Observation Wheel", Map.of("destination", "London"));
        when(ingestionService.similaritySearch(anyString(), anyInt())).thenReturn(List.of(doc));

        mockMvc.perform(get("/api/knowledge/similarity-search")
                        .param("query", "London eye wheel")
                        .param("topK", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is("london-eye")))
                .andExpect(jsonPath("$[0].metadata.destination", is("London")));
    }
}
