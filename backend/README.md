#  AI Travel Itinerary Generator — Backend API

The core Spring Boot service for the AI Travel Itinerary Generator. This backend leverages **Spring Boot 4.1.0** and **Spring AI 2.0.0** to generate type-safe, token-efficient travel plans using Google Gemini, Retrieval-Augmented Generation (RAG), and per-user conversational memory.

It exposes both a synchronous structured JSON endpoint and a reactive Server-Sent Events (SSE) streaming endpoint for progressive frontend rendering.

---

##  Key Features

- **Dual-Mode Execution:**
  - **Synchronous (`/api/itinerary/generate`):** Uses Spring AI's `.entity()` mapping to return strongly-typed Java Records.
  - **Reactive Streaming (`/api/itinerary/stream`):** Uses Spring WebFlux and `Flux<String>` to stream raw JSON tokens over SSE as the LLM generates them.
- **Provider-Level Structured Output:** Enforces strict native JSON mode on Google GenAI (`.useProviderStructuredOutput()`), eliminating markdown wrapper artifacts, conversational filler, and Jackson parsing exceptions.
- **Context-Aware RAG (`QuestionAnswerAdvisor` & `SimpleVectorStore`):** Grounds recommendations in curated vector store data for accurate, hyper-local activity suggestions.
- **Curated Kyoto Travel Knowledge Base:** Embeds rich, structured documents covering Kyoto landmarks (Fushimi Inari, Kiyomizu-dera, Arashiyama, Kinkaku-ji), food culture (Kaiseki, Nishiki Market, Shojin Ryori), transit & district clustering rules, and etiquette.
- **RAG & Ingestion Endpoints:**
  - `POST /api/v1/rag/ingest/kyoto`: Embeds and ingests curated Kyoto documents into the vector store.
  - `POST /api/v1/rag/ingest/custom`: Ingests custom text documents dynamically for any destination.
  - `GET /api/v1/rag/similarity-search?query=...&topK=...`: Directly inspects vector store retrieval results.
  - `GET /api/v1/rag/status`: Checks vector store and ingestion readiness.
- **Session-Isolated Chat Memory (`MessageChatMemoryAdvisor`):** Implements dynamic `conversationId` binding (`ChatMemory.CONVERSATION_ID`) to isolate multi-turn chat histories across concurrent users.
- **Type-Safe Serialization:** Built on Java 21+ Records and Jackson 3 (`tools.jackson`) for robust, error-free JSON processing.
