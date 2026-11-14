package com.yonglun.itineraryassistant.config;

import com.yonglun.itineraryassistant.service.IngestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "app.vectorstore.type", havingValue = "pgvector", matchIfMissing = true)
public class DocumentIndexLoader {

    private static final Logger log = LoggerFactory.getLogger(DocumentIndexLoader.class);

    private final JdbcTemplate jdbcTemplate;
    private final EmbeddingProperties embeddingProperties;
    private final IngestionService ingestionService;

    public DocumentIndexLoader(JdbcTemplate jdbcTemplate, EmbeddingProperties embeddingProperties, IngestionService ingestionService) {
        this.jdbcTemplate = jdbcTemplate;
        this.embeddingProperties = embeddingProperties;
        this.ingestionService = ingestionService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void loadExistingDocuments() {
        String table = embeddingProperties.getEffectiveTableName();
        try {
            Map<String, String> destinationById = new HashMap<>();
            jdbcTemplate.query(
                    "SELECT CAST(id AS varchar), metadata->>'destination' FROM public." + table,
                    rs -> {
                        destinationById.put(rs.getString(1), rs.getString(2));
                    });
            ingestionService.restoreDocumentIndex(destinationById);
            log.info("Restored {} existing documents from vector table '{}'", destinationById.size(), table);
        } catch (Exception e) {
            log.warn("Could not read existing documents from vector table '{}': {}", table, e.getMessage());
        }
    }
}
