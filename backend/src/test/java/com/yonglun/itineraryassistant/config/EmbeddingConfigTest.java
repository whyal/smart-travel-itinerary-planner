package com.yonglun.itineraryassistant.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.google.genai.text.GoogleGenAiTextEmbeddingModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EmbeddingConfigTest {

    private final EmbeddingConfig embeddingConfig = new EmbeddingConfig();

    @Test
    @DisplayName("Should derive correct default and custom vector table names")
    void testEffectiveTableName() {
        EmbeddingProperties props = new EmbeddingProperties();
        props.setProvider("google");
        props.setDimensions(3072);
        assertThat(props.getEffectiveTableName()).isEqualTo("vector_store_google_3072");

        props.setProvider("ollama");
        props.setModel("qwen3-embedding:0.6b");
        props.setDimensions(1024);
        assertThat(props.getEffectiveTableName()).isEqualTo("vector_store_ollama_1024");

        props.setProvider("openai-compatible");
        props.setDimensions(1024);
        assertThat(props.getEffectiveTableName()).isEqualTo("vector_store_openai_compatible_1024");

        // Custom explicit table name
        props.setTableName("custom_travel_vectors");
        assertThat(props.getEffectiveTableName()).isEqualTo("custom_travel_vectors");
    }

    @Test
    @DisplayName("Should select Ollama provider when configured")
    void testOllamaProviderSelection() {
        EmbeddingProperties props = new EmbeddingProperties();
        props.setProvider("ollama");
        props.setModel("qwen3-embedding:0.6b");
        props.setDimensions(1024);
        props.getOllama().setBaseUrl("http://localhost:11434");

        @SuppressWarnings("unchecked")
        ObjectProvider<GoogleGenAiTextEmbeddingModel> googleProvider = mock(ObjectProvider.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<OllamaEmbeddingModel> ollamaProvider = mock(ObjectProvider.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<OpenAiEmbeddingModel> openAiProvider = mock(ObjectProvider.class);

        OllamaEmbeddingModel mockOllama = mock(OllamaEmbeddingModel.class);
        when(ollamaProvider.getIfAvailable()).thenReturn(mockOllama);

        EmbeddingModel model = embeddingConfig.embeddingModel(props, googleProvider, ollamaProvider, openAiProvider);
        assertThat(model).isSameAs(mockOllama);
    }

    @Test
    @DisplayName("Should select Google GenAI provider when configured")
    void testGoogleProviderSelection() {
        EmbeddingProperties props = new EmbeddingProperties();
        props.setProvider("google");
        props.setModel("gemini-embedding-001");
        props.setDimensions(3072);

        @SuppressWarnings("unchecked")
        ObjectProvider<GoogleGenAiTextEmbeddingModel> googleProvider = mock(ObjectProvider.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<OllamaEmbeddingModel> ollamaProvider = mock(ObjectProvider.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<OpenAiEmbeddingModel> openAiProvider = mock(ObjectProvider.class);

        GoogleGenAiTextEmbeddingModel mockGoogle = mock(GoogleGenAiTextEmbeddingModel.class);
        when(googleProvider.getIfAvailable()).thenReturn(mockGoogle);

        EmbeddingModel model = embeddingConfig.embeddingModel(props, googleProvider, ollamaProvider, openAiProvider);
        assertThat(model).isSameAs(mockGoogle);
    }

    @Test
    @DisplayName("Should throw clear error when Google provider is missing API key/bean")
    void testMissingGoogleBeanThrowsHelpfulException() {
        EmbeddingProperties props = new EmbeddingProperties();
        props.setProvider("google");

        @SuppressWarnings("unchecked")
        ObjectProvider<GoogleGenAiTextEmbeddingModel> googleProvider = mock(ObjectProvider.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<OllamaEmbeddingModel> ollamaProvider = mock(ObjectProvider.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<OpenAiEmbeddingModel> openAiProvider = mock(ObjectProvider.class);

        when(googleProvider.getIfAvailable()).thenReturn(null);

        assertThatThrownBy(() -> embeddingConfig.embeddingModel(props, googleProvider, ollamaProvider, openAiProvider))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Google GenAI EmbeddingModel is not available. Please verify GEMINI_API_KEY");
    }
}
