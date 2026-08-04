#  AI Travel Itinerary Generator — Backend API

The core Spring Boot service for the AI Travel Itinerary Generator. This backend leverages **Spring Boot 4.1.0** and **Spring AI 2.0.0** to generate type-safe, token-efficient travel plans using Google Gemini, Retrieval-Augmented Generation (RAG), and per-user conversational memory.

It exposes both a synchronous structured JSON endpoint and a reactive Server-Sent Events (SSE) streaming endpoint for progressive frontend rendering.

---

##  Key Features

- **Dual-Mode Execution:**
  - **Synchronous (`/api/itinerary/generate`):** Uses Spring AI's `.entity()` mapping to return strongly-typed Java Records.
  - **Reactive Streaming (`/api/itinerary/stream`):** Uses Spring WebFlux and `Flux<String>` to stream raw JSON tokens over SSE as the LLM generates them.
- **Provider-Level Structured Output:** Enforces strict native JSON mode on Google GenAI (`.useProviderStructuredOutput()`), eliminating markdown wrapper artifacts, conversational filler, and Jackson parsing exceptions.
- **Context-Aware RAG (`QuestionAnswerAdvisor`):** Grounds recommendations in local vector store data for accurate, hyper-local activity suggestions.
- **Session-Isolated Chat Memory (`MessageChatMemoryAdvisor`):** Implements dynamic `conversationId` binding (`ChatMemory.CONVERSATION_ID`) to isolate multi-turn chat histories across concurrent users.
- **Type-Safe Serialization:** Built on Java 21+ Records and Jackson 3 (`tools.jackson`) for robust, error-free JSON processing.

---

##  Tech Stack

- **Framework:** Spring Boot 4.1.0
- **AI Engine:** Spring AI 2.0.0 (Google GenAI / Gemini)
- **Reactive Programming:** Spring WebFlux (Project Reactor)
- **JSON Parser:** Jackson 3 (`tools.jackson`)
- **Data Modeling:** Java 25+ Records
- **Memory & Vector Store:** Spring AI `ChatMemory`, `VectorStore`
