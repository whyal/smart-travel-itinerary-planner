package com.yonglun.itineraryassistant.config;

import com.yonglun.itineraryassistant.service.IngestionService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

import java.sql.ResultSet;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class DocumentIndexLoaderTest {

    private final EmbeddingProperties properties = new EmbeddingProperties();
    private final IngestionService ingestionService = mock(IngestionService.class);
    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);

    @Test
    void restoresDocumentsFromActiveVectorTable() throws Exception {
        properties.setProvider("ollama");
        properties.setDimensions(1024);
        ResultSet row = mock(ResultSet.class);
        when(row.getString(1)).thenReturn("id-1");
        when(row.getString(2)).thenReturn("Kyoto");
        doAnswer(inv -> {
            inv.<RowCallbackHandler>getArgument(1).processRow(row);
            return null;
        }).when(jdbcTemplate).query(contains("public.vector_store_ollama_1024"), any(RowCallbackHandler.class));

        new DocumentIndexLoader(jdbcTemplate, properties, ingestionService).loadExistingDocuments();

        verify(ingestionService).restoreDocumentIndex(Map.of("id-1", "Kyoto"));
    }

    @Test
    void startsNormallyWhenTableDoesNotExistYet() {
        doThrow(new RuntimeException("relation does not exist")).when(jdbcTemplate).query(anyString(), any(RowCallbackHandler.class));

        new DocumentIndexLoader(jdbcTemplate, properties, ingestionService).loadExistingDocuments();

        verifyNoInteractions(ingestionService);
    }
}
