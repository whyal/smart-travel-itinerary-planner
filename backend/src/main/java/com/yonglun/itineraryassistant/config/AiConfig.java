package com.yonglun.itineraryassistant.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class AiConfig {

    private static final Logger log = LoggerFactory.getLogger(AiConfig.class);

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20)
                .build();
    }

    @Configuration
    @ConditionalOnClass(name = "org.springframework.ai.vectorstore.pgvector.PgVectorStore")
    @ConditionalOnProperty(name = "app.vectorstore.type", havingValue = "pgvector", matchIfMissing = true)
    static class PgVectorStoreConfiguration {

        @Bean
        public VectorStore vectorStore(
                JdbcTemplate jdbcTemplate,
                EmbeddingModel embeddingModel,
                @Value("${spring.ai.vectorstore.pgvector.dimensions:3072}") int dimensions,
                @Value("${spring.ai.vectorstore.pgvector.index-type:}") String configuredIndexType) {

            org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgIndexType indexType;
            if (configuredIndexType != null && !configuredIndexType.isBlank()) {
                indexType = org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgIndexType.valueOf(configuredIndexType.trim().toUpperCase());
            } else {
                // pgvector HNSW & IVFFLAT indexes have a hard limit of 2,000 dimensions in PostgreSQL.
                // For high-dimensional embeddings (>2000, e.g. 3072-dim Gemini), use NONE to allow exact vector operations without index limits.
                indexType = (dimensions > 2000)
                        ? org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgIndexType.NONE
                        : org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgIndexType.HNSW;
            }

            log.info("Initializing persistent PgVectorStore (PostgreSQL + pgvector with {} dimensions, indexType: {})...",
                    dimensions, indexType);

            return org.springframework.ai.vectorstore.pgvector.PgVectorStore.builder(jdbcTemplate, embeddingModel)
                    .dimensions(dimensions)
                    .distanceType(org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgDistanceType.COSINE_DISTANCE)
                    .indexType(indexType)
                    .schemaName("public")
                    .vectorTableName("vector_store")
                    .initializeSchema(true)
                    .build();
        }
    }

    @Configuration
    static class FallbackVectorStoreConfiguration {

        @Bean
        @ConditionalOnMissingBean(VectorStore.class)
        public VectorStore fallbackVectorStore(EmbeddingModel embeddingModel) {
            log.info("Initializing in-memory SimpleVectorStore.");
            return SimpleVectorStore.builder(embeddingModel).build();
        }
    }

    @Bean
    public ChatClient chatClient(
            ChatClient.Builder builder,
            VectorStore vectorStore,
            ChatMemory chatMemory) {

        return builder
                .defaultSystem("You are a knowledgeable travel assistant. Generate compact itineraries strictly adhering to the requested schema. \n" +
                        "Rules:\n" +
                        "- No greetings, pleasantries, or closing remarks.\n" +
                        "- Keep activity descriptions concise (under 20 words).\n" +
                        "- Provide practical 'gettingThere' transit details and accurate 'operatingHours' for each activity.")
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        QuestionAnswerAdvisor.builder(vectorStore).build()
                )
                .build();
    }
}