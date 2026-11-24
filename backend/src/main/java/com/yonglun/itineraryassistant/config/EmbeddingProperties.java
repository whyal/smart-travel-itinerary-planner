package com.yonglun.itineraryassistant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.embedding")
public class EmbeddingProperties {

    private String provider = "google";
    private String model = "gemini-embedding-001";
    private int dimensions = 3072;
    private String tableName;
    private OllamaProperties ollama = new OllamaProperties();

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
}
