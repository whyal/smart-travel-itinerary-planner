package com.yonglun.itineraryassistant.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.vectorstore.SearchRequest;
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
            EmbeddingProperties properties,
            @Value("${app.chat.provider:${EMBEDDING_PROVIDER:}}") String configuredChatProvider,
            @Value("${spring.ai.google.genai.api-key:}") String geminiApiKey,
            ObjectProvider<GoogleGenAiChatModel> googleChatModelProvider,
            ObjectProvider<OllamaChatModel> ollamaChatModelProvider,
            ObjectProvider<OpenAiChatModel> openAiChatModelProvider) {

        String provider = (configuredChatProvider != null && !configuredChatProvider.isBlank())
                ? configuredChatProvider.trim().toLowerCase()
                : (properties.getProvider() != null && !properties.getProvider().isBlank()
                ? properties.getProvider().trim().toLowerCase()
                : "google");

        log.info("Configuring primary ChatModel [Active Provider: '{}']", provider);

        switch (provider) {
            case "ollama": {
                OllamaChatModel ollama = ollamaChatModelProvider.getIfAvailable();
                if (ollama != null) {
                    log.info("Using autoconfigured OllamaChatModel as primary ChatModel.");
                    return ollama;
                }
                throw new IllegalStateException("OllamaChatModel is not available. Please verify Ollama is running and configured.");
            }

            case "openai":
            case "openai-compatible":
            case "tei":
            case "vllm": {
                OpenAiChatModel openAi = openAiChatModelProvider.getIfAvailable();
                if (openAi != null) {
                    log.info("Using autoconfigured OpenAiChatModel as primary ChatModel.");
                    return openAi;
                }
                throw new IllegalStateException("OpenAiChatModel is not available. Please verify spring.ai.openai configuration.");
            }

            case "google":
            default: {
                boolean hasGeminiKey = geminiApiKey != null
                        && !geminiApiKey.isBlank()
                        && !"dummy-key".equalsIgnoreCase(geminiApiKey.trim())
                        && !geminiApiKey.startsWith("${");
                if (hasGeminiKey) {
                    GoogleGenAiChatModel google = googleChatModelProvider.getIfAvailable();
                    if (google != null) {
                        log.info("Using autoconfigured GoogleGenAiChatModel as primary ChatModel.");
                        return google;
                    }
                } else {
                    log.warn("GEMINI_API_KEY is not configured. Falling back to local OllamaChatModel if available...");
                    OllamaChatModel ollama = ollamaChatModelProvider.getIfAvailable();
                    if (ollama != null) {
                        log.info("Falling back to OllamaChatModel as primary ChatModel.");
                        return ollama;
                    }
                    OpenAiChatModel openAi = openAiChatModelProvider.getIfAvailable();
                    if (openAi != null) {
                        log.info("Falling back to OpenAiChatModel as primary ChatModel.");
                        return openAi;
                    }
                }
                GoogleGenAiChatModel google = googleChatModelProvider.getIfAvailable();
                if (google != null) {
                    return google;
                }
                throw new IllegalStateException("No ChatModel bean is currently available in application context.");
            }
        }
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
            VectorStore vectorStore) {

        PromptTemplate travelRagPromptTemplate = new PromptTemplate("""
                {query}

                Context information from ingested travel knowledge documents is below, surrounded by ---------------------

                ---------------------
                {question_answer_context}
                ---------------------

                Use the context only as supporting reference for venue names, locations, transit routes and operating hours. Never copy its sentences or metadata tags; write an original plan.
                """);

        return builder
                .defaultAdvisors(
                        QuestionAnswerAdvisor.builder(vectorStore)
                                .searchRequest(SearchRequest.builder()
                                        .topK(6)
                                        .similarityThreshold(0.40)
                                        .build())
                                .promptTemplate(travelRagPromptTemplate)
                                .build()
                )
                .build();
    }
}