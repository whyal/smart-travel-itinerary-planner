package com.yonglun.itineraryassistant.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.google.genai.text.GoogleGenAiTextEmbeddingModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Modular Embedding Model Factory.
 * Selects and instantiates the appropriate EmbeddingModel based on app.embedding.provider.
 */
@Configuration
public class EmbeddingConfig {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingConfig.class);

    @Bean
    @Primary
    public EmbeddingModel embeddingModel(
            EmbeddingProperties properties,
            ObjectProvider<GoogleGenAiTextEmbeddingModel> googleEmbeddingProvider,
            ObjectProvider<OllamaEmbeddingModel> ollamaEmbeddingProvider,
            ObjectProvider<OpenAiEmbeddingModel> openAiEmbeddingProvider) {

        String rawProvider = properties.getProvider() != null ? properties.getProvider().trim().toLowerCase() : "google";
        String modelName = properties.getModel();
        int dimensions = properties.getDimensions();

        log.info("Configuring modular EmbeddingModel [Provider: '{}', Model: '{}', Dimensions: {}]",
                rawProvider, modelName, dimensions);

        switch (rawProvider) {
            case "ollama": {
                OllamaEmbeddingModel existing = ollamaEmbeddingProvider.getIfAvailable();
                if (existing != null) {
                    log.info("Using autoconfigured OllamaEmbeddingModel for '{}'", modelName);
                    return existing;
                }
                String baseUrl = properties.getOllama().getBaseUrl();
                log.info("Creating custom OllamaEmbeddingModel for model '{}' at base URL '{}'", modelName, baseUrl);
                OllamaApi ollamaApi = OllamaApi.builder().baseUrl(baseUrl).build();
                return OllamaEmbeddingModel.builder()
                        .ollamaApi(ollamaApi)
                        .options(OllamaEmbeddingOptions.builder().model(modelName).build())
                        .build();
            }

            case "openai":
            case "openai-compatible":
            case "tei":
            case "vllm": {
                OpenAiEmbeddingModel existing = openAiEmbeddingProvider.getIfAvailable();
                if (existing != null) {
                    log.info("Using autoconfigured OpenAiEmbeddingModel for '{}'", modelName);
                    return existing;
                }
                String baseUrl = properties.getOpenaiCompatible().getBaseUrl();
                log.info("Configuring OpenAI-compatible embedding provider for model '{}' at base URL '{}'", modelName, baseUrl);
                throw new IllegalStateException("OpenAiEmbeddingModel not autoconfigured. Please configure spring.ai.openai.base-url="
                        + baseUrl + " and spring.ai.openai.api-key=" + properties.getOpenaiCompatible().getApiKey());
            }

            case "google":
            default: {
                GoogleGenAiTextEmbeddingModel existing = googleEmbeddingProvider.getIfAvailable();
                if (existing != null) {
                    log.info("Using Google GenAI Text Embedding Model ('{}')", modelName);
                    return existing;
                }
                throw new IllegalStateException("Google GenAI EmbeddingModel is not available. Please verify GEMINI_API_KEY is configured.");
            }
        }
    }
}
