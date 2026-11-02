# AI Travel Itinerary Generator — Backend Service

Core Spring Boot microservice for the AI Travel Itinerary Generator. Built with **Spring Boot 4.1.0**, **Spring AI 2.0.0**, and **Java 21**, providing AI-driven travel planning with Google Gemini, context-aware RAG, modular vector embeddings, and reactive SSE streaming.

## Prerequisites

- **Java Development Kit (JDK) 21+** (`java -version`)
- **Docker Desktop** (recommended for running PostgreSQL + pgvector) OR a local **PostgreSQL 15+** with the **`pgvector`** extension
- **Google Gemini API Key** (from [Google AI Studio](https://aistudio.google.com/)) OR local Ollama / OpenAI-compatible endpoint

## Quick Start & Setup

### 1. Start Database with Docker (Recommended)

From the `backend/` directory:

```bash
docker compose up -d
```

This starts a PostgreSQL instance with the `pgvector` extension configured on port `5432`.

*(Manual PostgreSQL setup alternative: create database `tripplanner` and run `CREATE EXTENSION IF NOT EXISTS vector;`)*

### 2. Configure Environment Variables

Create a `.env` file in the `backend/` directory (or copy from `.env.example`):

```bash
cp .env.example .env
```

Configure your credentials:

```env
GEMINI_API_KEY=your_gemini_api_key_here
DB_USERNAME=postgres
DB_PASSWORD=your_postgres_password_here
# Optional overrides:
# SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/tripplanner
# EMBEDDING_PROVIDER=google # Options: google, ollama, openai-compatible
```

### 3. Build & Run

```bash
# Run application in development mode (starts on http://localhost:8080)
./gradlew bootRun

# Run unit and integration tests (uses in-memory SimpleVectorStore; no live DB required)
./gradlew test

# Build executable production JAR
./gradlew build
```

## Architecture & Package Structure

```
backend/src/main/java/com/yonglun/itineraryassistant/
├── config/
│   ├── AiConfig.java               # ChatModel, ChatClient, PgVectorStore, SimpleVectorStore beans
│   ├── EmbeddingConfig.java         # Modular EmbeddingModel factory (Google / Ollama / OpenAI-compatible)
│   └── EmbeddingProperties.java     # Embedding properties & dynamic vector table name generator
├── controller/
│   ├── ItineraryController.java     # Synchronous JSON and SSE streaming itinerary endpoints
│   ├── KnowledgeController.java     # RAG ingestion (PDF/JSON/Text/Articles) & similarity search
│   ├── SavedItineraryController.java# CRUD endpoints for saved itinerary persistence
│   └── GlobalExceptionHandler.java  # Centralized REST error handling (ApiErrorResponse)
├── dto/
│   ├── PromptRequest.java           # User prompt and conversationId session payload
│   ├── TravelDocumentDto.java       # Structured travel item record with metadata
│   ├── Itinerary.java               # Structured day-by-day itinerary domain model
│   └── ApiErrorResponse.java        # Standardized error payload
├── model/
│   └── SavedItinerary.java          # JPA entity for PostgreSQL persistence
├── repository/
│   └── SavedItineraryRepository.java# Spring Data JPA repository for saved itineraries
└── service/
    ├── ItineraryService.java        # RAG retrieval, chat memory integration, and SSE flux streaming
    ├── IngestionService.java        # Document parsing, token chunking, and idempotent vector ingestion
    └── SavedItineraryService.java   # CRUD business logic for saved itineraries
```

## Environment Variables Reference

| Variable | Description | Default | Required? |
| :--- | :--- | :--- | :--- |
| `GEMINI_API_KEY` | API key for Google Gemini GenAI chat & cloud embeddings | — | Yes (if using Google) |
| `DB_USERNAME` | PostgreSQL database user | — | Yes |
| `DB_PASSWORD` | PostgreSQL database password | — | Yes |
| `SPRING_DATASOURCE_URL` | JDBC connection URL | `jdbc:postgresql://localhost:5432/tripplanner` | No |
| `VECTORSTORE_TYPE` | Vector store type (`pgvector` or `simple`) | `pgvector` | No |
| `EMBEDDING_PROVIDER` | Active embedding provider (`google`, `ollama`, `openai-compatible`) | `google` | No |
| `EMBEDDING_MODEL` | Model identifier for embeddings | `gemini-embedding-001` | No |
| `EMBEDDING_DIMENSIONS` | Embedding vector dimensions | `3072` | No |
| `EMBEDDING_TABLE_NAME` | Explicit table name override (blank = auto) | `vector_store_{provider}_{dims}` | No |
| `OLLAMA_BASE_URL` | Base URL for local Ollama service | `http://localhost:11434` | No |
| `OPENAI_COMPAT_BASE_URL` | Base URL for TEI / vLLM inference server | `http://localhost:8000` | No |
| `OPENAI_COMPAT_API_KEY` | API key for OpenAI-compatible endpoint | `dummy-key` | No |

## Embedding Provider Switching

The backend supports hot-swapping embedding models via environment variables with zero code changes:

| Provider | `EMBEDDING_PROVIDER` | `EMBEDDING_MODEL` | `EMBEDDING_DIMENSIONS` | Notes |
| :--- | :--- | :--- | :--- | :--- |
| **Google Gemini (Default)** | `google` | `gemini-embedding-001` | `3072` | Cloud-hosted, requires `GEMINI_API_KEY` |
| **Ollama (Local OSS)** | `ollama` | `qwen3-embedding:0.6b` | `1024` | Free, offline, runs on local GPU |
| **TEI / vLLM (OSS)** | `openai-compatible` | `Qwen/Qwen3-Embedding-0.6B` | `1024` | High-throughput Docker container |

Dynamic table routing guarantees that each model uses its own vector table (`vector_store_{provider}_{dimensions}`), preventing dimension collisions and data corruption.

> For in-depth recipes, WSL2 configurations, and schema partitioning details, see the [Modular Embedding Architecture Guide](../docs/EMBEDDINGS.md).

## REST & Streaming API Overview

All endpoints are hosted under `/api`. For full request/response schemas, JSON payloads, and curl examples, see the [REST & Streaming API Reference](../docs/API.md).

### Itinerary Endpoints
- `POST /api/itinerary/generate`: Synchronous structured JSON itinerary generation.
- `POST /api/itinerary/stream`: Reactive Server-Sent Events (SSE) token streaming for real-time UI rendering.
- `GET /api/itineraries`: Retrieve all saved itineraries.
- `POST /api/itineraries`: Save an itinerary to PostgreSQL.
- `GET /api/itineraries/{id}`: Retrieve a saved itinerary by ID.
- `DELETE /api/itineraries/{id}`: Delete a saved itinerary by ID.

### Knowledge Base & Ingestion Endpoints (`/api/knowledge`)
- `POST /api/knowledge/documents`: Ingest structured travel documents with metadata.
- `POST /api/knowledge/batch`: Ingest multiple destination document sets.
- `POST /api/knowledge/articles`: Ingest free-form articles or blog posts.
- `POST /api/knowledge/upload`: Multipart file upload (`.pdf`, `.json`, `.txt`, `.md`).
- `POST /api/knowledge/preload`: Preload bundled classpath datasets (Kyoto).
- `GET /api/knowledge/status`: Check vector store ingestion statistics per destination.
- `GET /api/knowledge/similarity-search`: Test vector store similarity retrieval.

## Testing & Quality Assurance

Unit and integration tests run entirely against an in-memory `SimpleVectorStore` bean (`AiConfig.java`), requiring no running database or external API credentials:

```bash
./gradlew test
```

When contributing code, ensure you adhere to the [Backend Agent Guidelines](AGENTS.md) (immutable record DTOs, dynamic vector table routing, and error handling conventions).
