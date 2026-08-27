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
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class AiConfig {

    private static final Logger log = LoggerFactory.getLogger(AiConfig.class);

    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    @Primary
    public ChatModel primaryChatModel(
            ObjectProvider<GoogleGenAiChatModel> googleChatModelProvider,
            ObjectProvider<OllamaChatModel> ollamaChatModelProvider,
            ObjectProvider<OpenAiChatModel> openAiChatModelProvider) {
        GoogleGenAiChatModel google = googleChatModelProvider.getIfAvailable();
        if (google != null) {
            return google;
        }
        OllamaChatModel ollama = ollamaChatModelProvider.getIfAvailable();
        if (ollama != null) {
            return ollama;
        }
        OpenAiChatModel openAi = openAiChatModelProvider.getIfAvailable();
        if (openAi != null) {
            return openAi;
        }
        throw new IllegalStateException("No ChatModel bean is currently available in application context.");
    }

    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20)
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "app.vectorstore.type", havingValue = "simple")
    public VectorStore simpleVectorStore(EmbeddingModel embeddingModel) {
        log.info("Initializing in-memory SimpleVectorStore.");
        return SimpleVectorStore.builder(embeddingModel).build();
    }

    @Bean
    @ConditionalOnProperty(name = "app.vectorstore.type", havingValue = "pgvector", matchIfMissing = true)
    public VectorStore pgVectorStore(
            JdbcTemplate jdbcTemplate,
            EmbeddingModel embeddingModel,
            EmbeddingProperties embeddingProperties,
            @Value("${spring.ai.vectorstore.pgvector.dimensions:0}") int configuredDimensions,
            @Value("${spring.ai.vectorstore.pgvector.index-type:}") String configuredIndexType,
            @Value("${spring.ai.vectorstore.pgvector.table-name:}") String configuredTableName) {

        int dimensions = configuredDimensions > 0 ? configuredDimensions : embeddingProperties.getDimensions();
        String tableName = (configuredTableName != null && !configuredTableName.isBlank())
                ? configuredTableName.trim().toLowerCase()
                : embeddingProperties.getEffectiveTableName();

        PgVectorStore.PgIndexType indexType;
        if (configuredIndexType != null && !configuredIndexType.isBlank()) {
            indexType = PgVectorStore.PgIndexType.valueOf(configuredIndexType.trim().toUpperCase());
        } else {
            indexType = (dimensions > 2000) ? PgVectorStore.PgIndexType.NONE : PgVectorStore.PgIndexType.HNSW;
        }

        log.info("Initializing persistent PgVectorStore (Table: '{}', Dimensions: {}, IndexType: {})...",
                tableName, dimensions, indexType);

        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .dimensions(dimensions)
                .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
                .indexType(indexType)
                .schemaName("public")
                .vectorTableName(tableName)
                .initializeSchema(true)
                .build();
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