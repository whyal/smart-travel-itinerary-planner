package com.yonglun.itineraryassistant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for modular embedding models.
 * Allows seamless switching between Google Gemini, Ollama (OSS models like Qwen3-Embedding-0.6B),
 * and OpenAI-compatible inference servers (TEI, vLLM, LM Studio) without breaking the application.
 */
@Component
@ConfigurationProperties(prefix = "app.embedding")
public class EmbeddingProperties {

    /**
     * Active embedding provider: "google", "ollama", "openai-compatible" (or "tei", "vllm", "openai")
     */
    private String provider = "google";

    /**
     * Model name identifier (e.g., "gemini-embedding-001", "qwen3-embedding:0.6b", "Qwen/Qwen3-Embedding-0.6B")
     */
    private String model = "gemini-embedding-001";

    /**
     * Vector embedding dimensions (e.g. 3072 for Gemini, 1024 for Qwen3-Embedding-0.6B, 1536 for text-embedding-3-small)
     */
    private int dimensions = 3072;

    /**
     * Explicit pgvector table name. If blank/null, dynamically resolves to:
     * "vector_store_" + sanitized(provider) + "_" + dimensions (e.g. "vector_store_ollama_1024")
     */
    private String tableName;

    /**
     * Ollama provider settings
     */
    private OllamaProperties ollama = new OllamaProperties();

    /**
     * OpenAI-compatible provider settings (TEI, vLLM, LM Studio, etc.)
     */
    private OpenAiCompatibleProperties openaiCompatible = new OpenAiCompatibleProperties();

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getDimensions() {
        return dimensions;
    }

    public void setDimensions(int dimensions) {
        this.dimensions = dimensions;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public OllamaProperties getOllama() {
        return ollama;
    }

    public void setOllama(OllamaProperties ollama) {
        this.ollama = ollama;
    }

    public OpenAiCompatibleProperties getOpenaiCompatible() {
        return openaiCompatible;
    }

    public void setOpenaiCompatible(OpenAiCompatibleProperties openaiCompatible) {
        this.openaiCompatible = openaiCompatible;
    }

    /**
     * Resolves the effective vector table name in PostgreSQL pgvector.
     * Prevents dimension collisions across different embedding models.
     */
    public String getEffectiveTableName() {
        if (tableName != null && !tableName.isBlank()) {
            return tableName.trim().toLowerCase();
        }
        String cleanProvider = (provider != null ? provider : "default")
                .trim()
                .toLowerCase()
                .replaceAll("[^a-z0-9_]", "_");
        return "vector_store_" + cleanProvider + "_" + dimensions;
    }

    public static class OllamaProperties {
        private String baseUrl = "http://localhost:11434";

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
    }

    public static class OpenAiCompatibleProperties {
        private String baseUrl = "http://localhost:8000";
        private String apiKey = "dummy-key";

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }
    }
}
