package com.yonglun.itineraryassistant.config;

import com.yonglun.itineraryassistant.service.IngestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class KyotoDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(KyotoDataInitializer.class);

    private final IngestionService ingestionService;
    private final boolean autoIngest;

    public KyotoDataInitializer(
            IngestionService ingestionService,
            @Value("${app.rag.auto-ingest-kyoto:true}") boolean autoIngest) {
        this.ingestionService = ingestionService;
        this.autoIngest = autoIngest;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (autoIngest) {
            log.info("Auto-ingesting Kyoto travel knowledge base into VectorStore on startup...");
            try {
                int count = ingestionService.ingestKyotoKnowledge();
                log.info("Successfully initialized VectorStore with {} Kyoto documents.", count);
            } catch (Exception e) {
                log.warn("Auto-ingestion of Kyoto documents was skipped or encountered an issue: {}", e.getMessage());
            }
        } else {
            log.info("Auto-ingestion of Kyoto documents is disabled via configuration (app.rag.auto-ingest-kyoto=false).");
        }
    }
}
