# Backend Agent Guidelines & System Boundaries

This document defines the architectural rules, coding standards, and system invariants for any AI agent or developer working on the **Spring Boot 4 + Spring AI 2.0 Backend**.

---

## 1. System Scope & Identity

The backend is a reactive and RESTful travel itinerary service integrating Spring AI, PostgreSQL `pgvector`, and multi-turn chat memory advisors.

### Key Class & Configuration Map

| Role | Class / File | Responsibility |
| :--- | :--- | :--- |
| **Modular Embedding Factory** | [`EmbeddingConfig`](src/main/java/com/yonglun/itineraryassistant/config/EmbeddingConfig.java) | Selects & instantiates the active `EmbeddingModel` based on configuration. |
| **Embedding Properties** | [`EmbeddingProperties`](src/main/java/com/yonglun/itineraryassistant/config/EmbeddingProperties.java) | Config properties & dynamic vector table name generator. |
| **AI & VectorStore Setup** | [`AiConfig`](src/main/java/com/yonglun/itineraryassistant/config/AiConfig.java) | Primary `ChatModel`, `ChatClient`, `PgVectorStore`, & `SimpleVectorStore` beans. |
| **Itinerary Generation & SSE** | [`ItineraryService`](src/main/java/com/yonglun/itineraryassistant/service/ItineraryService.java) | RAG retrieval, chat memory integration, and SSE flux streaming. |
| **Knowledge & Ingestion** | [`IngestionService`](src/main/java/com/yonglun/itineraryassistant/service/IngestionService.java) | Ingests PDF/JSON/Text docs into isolated destination partitions. |
| **Persistence CRUD** | [`SavedItineraryService`](src/main/java/com/yonglun/itineraryassistant/service/SavedItineraryService.java) | Saves, updates, and deletes generated itineraries. |
| **Global Error Handling** | [`GlobalExceptionHandler`](src/main/java/com/yonglun/itineraryassistant/controller/GlobalExceptionHandler.java) | Uniform REST API error responses (`ApiErrorResponse`). |
| **Application Config** | [`application.yaml`](src/main/resources/application.yaml) | Base datasource, model, and provider configuration. |

---

## 2. Hard Boundaries & Invariants (MUST FOLLOW)

### ✅ Architectural Requirements (DO)
- **Use Immutable Records for DTOs**: All request/response contracts in `com.yonglun.itineraryassistant.dto` must be Java `record` types with Jakarta Validation annotations (`@NotBlank`, `@Min`, etc.).
- **Dynamic Table Isolation**: Always route vector operations through `embeddingProperties.getEffectiveTableName()` to guarantee different embedding models reside in separate vector tables (`vector_store_{provider}_{dimensions}`).
- **Index Threshold Awareness**: Maintain HNSW indexing for `dimensions <= 2000` and `NONE` (flat scan) for `dimensions > 2000` (e.g. Gemini 3072 dims).
- **Spring AI `@Primary` Resolution**: Whenever defining starter beans with potential classpath ambiguities, ensure `@Primary` is explicitly declared.
- **Use Spring-Managed Beans**: Always inject the Spring-managed `ObjectMapper` rather than instantiating new instances.

### 🚫 Forbidden Patterns (DO NOT)
- **DO NOT hardcode database tables**: Never write raw table names like `"vector_store"` in queries; always use `EmbeddingProperties`.
- **DO NOT break SSE Streaming Protocol**: SSE endpoints must produce `MediaType.TEXT_EVENT_STREAM_VALUE` and return raw string chunks without Markdown code block wrappers (` ```json `).
- **DO NOT create circular dependencies**: Controllers $\rightarrow$ Services $\rightarrow$ Repositories/VectorStore. Never inject controllers into services.

---

## 3. Embedding Model Switching Recipes (Zero Code Changes)

Embedding models can be switched entirely through environment variables or `application.yaml`:

### Option A: Local OSS Model (Ollama `qwen3-embedding:0.6b`)
```bash
export EMBEDDING_PROVIDER=ollama
export EMBEDDING_MODEL=qwen3-embedding:0.6b
export EMBEDDING_DIMENSIONS=1024
export OLLAMA_BASE_URL=http://localhost:11434
```

### Option B: High-Performance Server (HuggingFace TEI / vLLM)
```bash
export EMBEDDING_PROVIDER=openai-compatible
export EMBEDDING_MODEL=Qwen/Qwen3-Embedding-0.6B
export EMBEDDING_DIMENSIONS=1024
export OPENAI_COMPAT_BASE_URL=http://localhost:8000
```

### Option C: Cloud Provider (Google Gemini)
```bash
export EMBEDDING_PROVIDER=google
export EMBEDDING_MODEL=gemini-embedding-001
export EMBEDDING_DIMENSIONS=3072
export GEMINI_API_KEY=your_gemini_api_key
```

---

## 4. Verification & Testing

Always verify all backend changes before completing a task:

```bash
./gradlew test
```

> **Note**: All unit and controller tests use in-memory vector stores and mocks. They do not require a running PostgreSQL or Ollama instance to pass.
