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
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
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

    @Bean
    public VectorStore vectorStore(
            @Value("${app.vectorstore.type:pgvector}") String vectorStoreType,
            ObjectProvider<JdbcTemplate> jdbcTemplateProvider,
            EmbeddingModel embeddingModel) {

        if ("simple".equalsIgnoreCase(vectorStoreType)) {
            log.info("Using in-memory SimpleVectorStore (app.vectorstore.type=simple).");
            return SimpleVectorStore.builder(embeddingModel).build();
        }

        try {
            JdbcTemplate jdbcTemplate = jdbcTemplateProvider.getIfAvailable();
            if (jdbcTemplate != null) {
                log.info("Initializing persistent PgVectorStore (PostgreSQL + pgvector)...");
                return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                        .dimensions(768)
                        .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
                        .schemaName("public")
                        .vectorTableName("vector_store")
                        .initializeSchema(true)
                        .build();
            } else {
                log.warn("JdbcTemplate unavailable. Falling back to in-memory SimpleVectorStore.");
                return SimpleVectorStore.builder(embeddingModel).build();
            }
        } catch (Exception e) {
            log.warn("Failed to initialize PgVectorStore ({}: {}). Falling back to in-memory SimpleVectorStore.",
                    e.getClass().getSimpleName(), e.getMessage());
            return SimpleVectorStore.builder(embeddingModel).build();
        }
    }

    @Bean
    public ChatClient chatClient(
            ChatClient.Builder builder,
            VectorStore vectorStore,
            ChatMemory chatMemory) {

        return builder
                .defaultSystem("You are a concise travel assistant. Generate compact itineraries strictly adhering to the requested schema. \n" +
                        "Rules:\n" +
                        "- No greetings, pleasantries, or closing remarks.\n" +
                        "- Keep activity descriptions under 15 words.\n" +
                        "- Focus strictly on location, duration, and core activity.")
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        QuestionAnswerAdvisor.builder(vectorStore).build()
                )
                .build();
    }
}