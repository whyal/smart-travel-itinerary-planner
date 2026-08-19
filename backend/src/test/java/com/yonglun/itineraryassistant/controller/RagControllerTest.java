package com.yonglun.itineraryassistant.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yonglun.itineraryassistant.dto.CustomIngestionRequest;
import com.yonglun.itineraryassistant.service.IngestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RagControllerTest {

    @Mock
    private IngestionService ingestionService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(new RagController(ingestionService)).build();
    }

    @Test
    void testIngestCustomKnowledgeEndpoint() throws Exception {
        CustomIngestionRequest request = new CustomIngestionRequest(
                "Tokyo",
                List.of("Tokyo Tower guide", "Shinjuku nightlife guide")
        );

        when(ingestionService.ingestDestinationKnowledge(anyList(), eq("Tokyo"))).thenReturn(2);

        mockMvc.perform(post("/api/rag/ingest/custom")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.destination", is("Tokyo")))
                .andExpect(jsonPath("$.documentsIngested", is(2)));
    }

    @Test
    void testIngestCustomKnowledgeEmptyValidation() throws Exception {
        CustomIngestionRequest request = new CustomIngestionRequest("Tokyo", List.of());

        mockMvc.perform(post("/api/rag/ingest/custom")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("error")));
    }

    @Test
    void testSimilaritySearchEndpoint() throws Exception {
        Document doc = new Document("tokyo-skytree", "Tokyo Skytree Guide Content", Map.of("destination", "Tokyo", "category", "attraction"));
        when(ingestionService.similaritySearch(anyString(), anyInt())).thenReturn(List.of(doc));

        mockMvc.perform(get("/api/rag/similarity-search")
                        .param("query", "tower view")
                        .param("topK", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is("tokyo-skytree")))
                .andExpect(jsonPath("$[0].text", containsString("Tokyo Skytree Guide Content")))
                .andExpect(jsonPath("$[0].metadata.destination", is("Tokyo")));
    }

    @Test
    void testStatusEndpoint() throws Exception {
        when(ingestionService.getIngestedDocumentCount()).thenReturn(10);
        when(ingestionService.getIngestedDestinations()).thenReturn(Set.of("Tokyo"));
        when(ingestionService.getDestinationDocumentCounts()).thenReturn(Map.of("Tokyo", 10));

        mockMvc.perform(get("/api/rag/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ready")))
                .andExpect(jsonPath("$.totalDocumentsIngested", is(10)))
                .andExpect(jsonPath("$.destinations", hasItem("Tokyo")))
                .andExpect(jsonPath("$.documentCountByDestination.Tokyo", is(10)));
    }
}
