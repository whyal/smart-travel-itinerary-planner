package com.yonglun.itineraryassistant.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.ObjectProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiConfigTest {

    private final AiConfig aiConfig = new AiConfig();

    @Test
    @DisplayName("Should select OllamaChatModel when active provider is ollama")
    void testOllamaChatModelSelection() {
        EmbeddingProperties props = new EmbeddingProperties();
        props.setProvider("ollama");

        @SuppressWarnings("unchecked")
        ObjectProvider<GoogleGenAiChatModel> googleProvider = mock(ObjectProvider.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<OllamaChatModel> ollamaProvider = mock(ObjectProvider.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<OpenAiChatModel> openAiProvider = mock(ObjectProvider.class);

        OllamaChatModel mockOllama = mock(OllamaChatModel.class);
        when(ollamaProvider.getIfAvailable()).thenReturn(mockOllama);

        ChatModel model = aiConfig.primaryChatModel(props, null, null, googleProvider, ollamaProvider, openAiProvider);
        assertThat(model).isSameAs(mockOllama);
    }

    @Test
    @DisplayName("Should select OpenAiChatModel when active provider is openai-compatible")
    void testOpenAiChatModelSelection() {
        EmbeddingProperties props = new EmbeddingProperties();
        props.setProvider("openai-compatible");

        @SuppressWarnings("unchecked")
        ObjectProvider<GoogleGenAiChatModel> googleProvider = mock(ObjectProvider.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<OllamaChatModel> ollamaProvider = mock(ObjectProvider.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<OpenAiChatModel> openAiProvider = mock(ObjectProvider.class);

        OpenAiChatModel mockOpenAi = mock(OpenAiChatModel.class);
        when(openAiProvider.getIfAvailable()).thenReturn(mockOpenAi);

        ChatModel model = aiConfig.primaryChatModel(props, null, null, googleProvider, ollamaProvider, openAiProvider);
        assertThat(model).isSameAs(mockOpenAi);
    }

    @Test
    @DisplayName("Should select GoogleGenAiChatModel when provider is google and valid key is present")
    void testGoogleChatModelSelectionWithKey() {
        EmbeddingProperties props = new EmbeddingProperties();
        props.setProvider("google");

        @SuppressWarnings("unchecked")
        ObjectProvider<GoogleGenAiChatModel> googleProvider = mock(ObjectProvider.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<OllamaChatModel> ollamaProvider = mock(ObjectProvider.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<OpenAiChatModel> openAiProvider = mock(ObjectProvider.class);

        GoogleGenAiChatModel mockGoogle = mock(GoogleGenAiChatModel.class);
        when(googleProvider.getIfAvailable()).thenReturn(mockGoogle);

        ChatModel model = aiConfig.primaryChatModel(props, null, "valid-gemini-key", googleProvider, ollamaProvider, openAiProvider);
        assertThat(model).isSameAs(mockGoogle);
    }

    @Test
    @DisplayName("Should fall back to OllamaChatModel when provider is google but GEMINI_API_KEY is not configured")
    void testGoogleFallbackToOllamaWhenKeyMissing() {
        EmbeddingProperties props = new EmbeddingProperties();
        props.setProvider("google");

        @SuppressWarnings("unchecked")
        ObjectProvider<GoogleGenAiChatModel> googleProvider = mock(ObjectProvider.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<OllamaChatModel> ollamaProvider = mock(ObjectProvider.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<OpenAiChatModel> openAiProvider = mock(ObjectProvider.class);

        OllamaChatModel mockOllama = mock(OllamaChatModel.class);
        when(ollamaProvider.getIfAvailable()).thenReturn(mockOllama);

        ChatModel model = aiConfig.primaryChatModel(props, null, "", googleProvider, ollamaProvider, openAiProvider);
        assertThat(model).isSameAs(mockOllama);
    }

    @Test
    @DisplayName("Should throw clear exception when selected provider is unavailable")
    void testThrowsWhenNoChatModelAvailable() {
        EmbeddingProperties props = new EmbeddingProperties();
        props.setProvider("ollama");

        @SuppressWarnings("unchecked")
        ObjectProvider<GoogleGenAiChatModel> googleProvider = mock(ObjectProvider.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<OllamaChatModel> ollamaProvider = mock(ObjectProvider.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<OpenAiChatModel> openAiProvider = mock(ObjectProvider.class);

        when(ollamaProvider.getIfAvailable()).thenReturn(null);

        assertThatThrownBy(() -> aiConfig.primaryChatModel(props, null, null, googleProvider, ollamaProvider, openAiProvider))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("OllamaChatModel is not available");
    }
}
