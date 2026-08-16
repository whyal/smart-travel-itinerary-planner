package com.yonglun.itineraryassistant.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yonglun.itineraryassistant.dto.CustomIngestionRequest;
import com.yonglun.itineraryassistant.service.IngestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.ai.document.Document;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class RagControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockitoBean
    private IngestionService ingestionService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.webApplicationContext).build();
    }

    @Test
    void testIngestKyotoKnowledgeEndpoint() throws Exception {
        when(ingestionService.ingestKyotoKnowledge()).thenReturn(10);

        mockMvc.perform(post("/api/rag/ingest/kyoto"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.destination", is("Kyoto")))
                .andExpect(jsonPath("$.documentsIngested", is(10)))
                .andExpect(jsonPath("$.message", containsString("10 Kyoto travel knowledge documents")));
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
        Document doc = new Document("kyoto-fushimi-inari", "Fushimi Inari Guide Content", Map.of("destination", "Kyoto", "category", "attraction"));
        when(ingestionService.similaritySearch(anyString(), anyInt())).thenReturn(List.of(doc));

        mockMvc.perform(get("/api/rag/similarity-search")
                        .param("query", "Inari shrine torii")
                        .param("topK", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is("kyoto-fushimi-inari")))
                .andExpect(jsonPath("$[0].text", containsString("Fushimi Inari Guide Content")))
                .andExpect(jsonPath("$[0].metadata.destination", is("Kyoto")));
    }

    @Test
    void testStatusEndpoint() throws Exception {
        when(ingestionService.isKyotoIngested()).thenReturn(true);
        when(ingestionService.getIngestedDocumentCount()).thenReturn(10);

        mockMvc.perform(get("/api/rag/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ready")))
                .andExpect(jsonPath("$.kyotoIngested", is(true)))
                .andExpect(jsonPath("$.totalDocumentsIngested", is(10)));
    }
}
