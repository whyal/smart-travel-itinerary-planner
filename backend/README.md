# AI Travel Itinerary Generator — Backend API

Core Spring Boot service for the AI Travel Itinerary Generator. This backend uses **Spring Boot 4.1.0** and **Spring AI 2.0.0** with Java 21 to generate travel plans using Google Gemini, Retrieval-Augmented Generation (RAG), and per-user conversational memory.

It exposes both a synchronous structured JSON endpoint and a reactive Server-Sent Events (SSE) streaming endpoint for progressive frontend rendering.

---

## Key Features

- **Dual-Mode Execution:**
  - **Synchronous (`/api/itinerary/generate`):** Uses Spring AI's `.entity()` mapping to return strongly-typed Java Records.
  - **Reactive Streaming (`/api/itinerary/stream`):** Uses Spring WebFlux and `Flux<String>` to stream raw JSON tokens over SSE as the model generates them.
- **Provider-Level Structured Output:** Configured for native JSON mode on Google GenAI (`.useProviderStructuredOutput()`) to ensure schema-compliant output without markdown wrapper artifacts.
- **Modular Embedding Architecture (Cloud & OSS):** Pluggable embedding providers supporting Google Gemini (`gemini-embedding-001`), local OSS models like `Qwen3-Embedding-0.6B` via Ollama or HuggingFace TEI, and OpenAI-compatible inference servers. Dynamic table isolation (`vector_store_{provider}_{dimensions}`) prevents vector dimension mismatch issues. See [docs/EMBEDDINGS.md](../docs/EMBEDDINGS.md) and [backend/AGENTS.md](AGENTS.md).
- **Context-Aware RAG with Persistent `PgVectorStore`:** Persists document embeddings directly in PostgreSQL via the `pgvector` extension with automatic HNSW indexing for <= 2000 dimension models.
- **Destination-Agnostic Knowledge Base:** Embeds structured and raw knowledge documents for any destination (e.g. Kyoto, Tokyo, Paris, Rome, Seoul, etc.).
- **Admin Ingestion REST API (`/api/admin/ingest`):**
  - `POST /api/admin/ingest/documents`: Ingests an array of structured travel documents with metadata (title, category, district, duration, best time, tags).
  - `POST /api/admin/ingest/batch`: Ingests a batch of travel documents with a destination tag.
  - `POST /api/admin/ingest/articles`: Ingests raw article text strings dynamically for any destination.
  - `POST /api/admin/ingest/upload`: Multi-part manual file upload supporting `.pdf`, `.json`, `.txt`, and `.md` formats.
  - `POST /api/admin/ingest/preload`: Preloads datasets from classpath or custom resource paths.
  - `GET /api/admin/ingest/status`: Checks total documents and breakdown per destination.
  - `GET /api/admin/ingest/similarity-search?query=...&topK=...`: Inspects vector store similarity retrieval.
- **Session-Isolated Chat Memory (`MessageChatMemoryAdvisor`):** Implements dynamic `conversationId` binding (`ChatMemory.CONVERSATION_ID`) to isolate multi-turn chat histories across concurrent users.
- **Type-Safe Serialization:** Built on Java 21 Records and Jackson 3 for robust JSON processing.
