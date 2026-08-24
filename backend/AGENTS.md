# AGENTS.md — AI Agent Guide for Backend Architecture & Modular Embeddings

This document serves as the zero-token-waste guide for AI agents and developers working on the **AI Travel Itinerary Generator Backend**.
Read this document before scanning the codebase.

---

## 1. Quick File Map & Architecture

| Role | File Path |
| :--- | :--- |
| **Embedding Config Properties** | [`com.yonglun.itineraryassistant.config.EmbeddingProperties`](file:///home/whyal/Projects/smart-travel-itinerary-planner/backend/src/main/java/com/yonglun/itineraryassistant/config/EmbeddingProperties.java) |
| **Modular Embedding Factory** | [`com.yonglun.itineraryassistant.config.EmbeddingConfig`](file:///home/whyal/Projects/smart-travel-itinerary-planner/backend/src/main/java/com/yonglun/itineraryassistant/config/EmbeddingConfig.java) |
| **VectorStore & ChatClient Config**| [`com.yonglun.itineraryassistant.config.AiConfig`](file:///home/whyal/Projects/smart-travel-itinerary-planner/backend/src/main/java/com/yonglun/itineraryassistant/config/AiConfig.java) |
| **Knowledge & Ingestion Service** | [`com.yonglun.itineraryassistant.service.IngestionService`](file:///home/whyal/Projects/smart-travel-itinerary-planner/backend/src/main/java/com/yonglun/itineraryassistant/service/IngestionService.java) |
| **Knowledge Controller & Status** | [`com.yonglun.itineraryassistant.controller.KnowledgeController`](file:///home/whyal/Projects/smart-travel-itinerary-planner/backend/src/main/java/com/yonglun/itineraryassistant/controller/KnowledgeController.java) |
| **Configuration File** | [`src/main/resources/application.yaml`](file:///home/whyal/Projects/smart-travel-itinerary-planner/backend/src/main/resources/application.yaml) |
| **Detailed Architecture & Presets** | [`docs/EMBEDDINGS.md`](file:///home/whyal/Projects/smart-travel-itinerary-planner/docs/EMBEDDINGS.md) |

---

## 2. Core Invariants & Rules

1. **Modular Embedding Providers**:
   - Supported values for `app.embedding.provider`:
     - `google` (default): Uses `gemini-embedding-001` or `text-embedding-004` (3072 or 768 dims).
     - `ollama`: Uses local OSS models such as `qwen3-embedding:0.6b`, `bge-m3`, `nomic-embed-text` (default base URL: `http://localhost:11434`).
     - `openai-compatible` / `tei` / `vllm`: Uses OpenAI-compatible endpoints such as HuggingFace TEI (Text Embeddings Inference) or vLLM (default base URL: `http://localhost:8000`).
2. **Dimension & Table Isolation**:
   - Embeddings from different models (or dimensions) reside in different vector spaces.
   - Vector table name is **dynamically routed**: `vector_store_{provider}_{dimensions}` (e.g., `vector_store_google_3072`, `vector_store_ollama_1024`).
   - If `app.embedding.table-name` is explicitly specified, it overrides the auto-generated table name.
3. **pgvector Index Optimization**:
   - For `dimensions <= 2000` (e.g. Qwen3's 1024 dims), `PgVectorStore` automatically configures **HNSW** indexing for fast ANN search.
   - For `dimensions > 2000` (e.g. Gemini's 3072 dims), `PgVectorStore` automatically falls back to `NONE` (exact flat search).
4. **Primary ChatModel Resolution**:
   - [`AiConfig.java`](file:///home/whyal/Projects/smart-travel-itinerary-planner/backend/src/main/java/com/yonglun/itineraryassistant/config/AiConfig.java) defines a `@Primary` `ChatModel` bean to prevent Spring autoconfiguration ambiguity when multiple model starters reside on the classpath.

---

## 3. How to Switch Embedding Models (Zero Code Change)

### Option A: Switch to Local OSS Model (Qwen3-Embedding-0.6B via Ollama)
Set environment variables:
```bash
export EMBEDDING_PROVIDER=ollama
export EMBEDDING_MODEL=qwen3-embedding:0.6b
export EMBEDDING_DIMENSIONS=1024
export OLLAMA_BASE_URL=http://localhost:11434
```

### Option B: Switch to HuggingFace TEI / vLLM (OpenAI-compatible)
Set environment variables:
```bash
export EMBEDDING_PROVIDER=openai-compatible
export EMBEDDING_MODEL=Qwen/Qwen3-Embedding-0.6B
export EMBEDDING_DIMENSIONS=1024
export OPENAI_COMPAT_BASE_URL=http://localhost:8000
```

### Option C: Default Google Gemini
```bash
export EMBEDDING_PROVIDER=google
export EMBEDDING_MODEL=gemini-embedding-001
export EMBEDDING_DIMENSIONS=3072
export GEMINI_API_KEY=your_key_here
```

---

## 4. Verification & Testing

Always verify your changes with:
```bash
./gradlew test
```
All unit and integration tests run in memory without requiring a live PostgreSQL or Ollama instance.
