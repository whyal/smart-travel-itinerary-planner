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

        PromptTemplate travelRagPromptTemplate = new PromptTemplate("""
                {query}

                Context information from ingested travel knowledge documents is below, surrounded by ---------------------

                ---------------------
                {question_answer_context}
                ---------------------

                Instructions for utilizing context documents:
                - Supporting Reference Only (NEVER Copy Verbatim): Retrieved context documents are supporting reference material ONLY (providing factual venue names, locations, transit routes, and operating hours). DO NOT copy or paste raw document sentences, paragraphs, or metadata tags (such as 'Destination:', 'Title:', 'District:', 'Category:', 'Suggested Duration:'). Every activity must be an original, synthesized travel plan written for the user's itinerary.
                - Day Count Invariant: You MUST generate exactly the number of days requested in {query}. If context documents do not cover all requested days, supplement with your knowledge of the destination to complete every day with 2 to 4 activities.
                - Consistent UI Fields: For EVERY activity, you MUST provide all 5 fields with clean formatting: 'time' (chronological time, e.g., '09:00 AM'), 'location' (clean venue/place name only), 'description' (fresh, concise 15-25 words action plan), 'gettingThere' (practical transit directions), and 'operatingHours' (visiting hours).
                - JSON Whitespace & Clean Formatting: Output clean, compact RFC8259 JSON without unnecessary whitespace, excessive indentation, or empty lines. All string properties must be single-line strings with normal single-space separation between words. Never include unescaped newlines, tabs, or multiple consecutive spaces in string values.
                """);

        return builder
                .defaultSystem("You are an expert travel assistant. Generate compact, realistic itineraries adhering strictly to the requested schema.\n\n" +
                        "Core Rules:\n" +
                        "- Day Count Invariant: The 'days' array MUST contain EXACTLY the number of days requested in the user prompt (numbered sequentially 1 to N). Never collapse or truncate days.\n" +
                        "- Activities Requirement: Every day MUST include 2 to 4 activities. Never leave 'activities' empty.\n" +
                        "- Supporting Knowledge Synthesis (NEVER Copy Verbatim): Retrieved context documents are supporting reference material ONLY. DO NOT lazily copy or echo identical sentences, paragraphs, or document metadata tags (e.g. 'Destination:', 'Title:', 'District:', 'Category:'). Synthesize facts into a fresh, original travel plan from a traveler's perspective.\n" +
                        "- Consistent UI Activity Fields: For EVERY activity, you MUST provide all 5 fields with clean formatting for the UI:\n" +
                        "  1. 'time': Specific scheduled time or window (e.g. '09:00 AM', '01:30 PM', '06:00 PM').\n" +
                        "  2. 'location': Clean place/venue name only (e.g. 'Osaka Castle'), strictly without prefixes or metadata.\n" +
                        "  3. 'description': Concise (15 to 25 words) synthesized action plan of what the traveler experiences. Never copy identical text from context documents.\n" +
                        "  4. 'gettingThere': Specific, realistic transit directions (e.g. '5 min walk from Station Exit 2').\n" +
                        "  5. 'operatingHours': Specific operating hours or window (e.g. '09:00 AM - 05:00 PM' or 'Open 24 hours').\n" +
                        "- Location Uniqueness: Every attraction, museum, restaurant, landmark, or specific venue MUST appear at most once across the entire multi-day itinerary.\n" +
                        "- Geographic Flow: Cluster activities by district or neighborhood per day to eliminate unnecessary commuting and backtracking.\n" +
                        "- Realistic Pacing: Plan 2 to 4 major activities per day with practical 'gettingThere' transit details and accurate 'operatingHours'.\n" +
                        "- JSON Whitespace & Formatting: Output clean, compact RFC8259 JSON without unnecessary spaces, excessive indentation, blank lines, or trailing whitespace. In all string values, use normal single-space separation between words with no raw newlines or tabs.")
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
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