# AI Travel Itinerary Generator — Backend API

Core Spring Boot service for the AI Travel Itinerary Generator. Built with **Spring Boot 4.1.0**, **Spring AI 2.0.0**, and **Java 21**, providing AI-driven travel planning with Google Gemini, context-aware RAG, modular vector embeddings, and reactive SSE streaming.

---

## Prerequisites

Ensure you have the following installed before setting up the backend:

- **Java Development Kit (JDK) 21+**
- **PostgreSQL 15+** with the **`pgvector`** extension installed
- **Google Gemini API Key** (or local Ollama / OpenAI-compatible endpoint if using OSS embeddings)

---

## Getting Started & Setup

### 1. Database Configuration

Create the PostgreSQL database and ensure the `vector` extension is enabled:

```sql
CREATE DATABASE tripplanner;
\c tripplanner;
CREATE EXTENSION IF NOT EXISTS vector;
```

### 2. Environment Variables

Create a `.env` file in the `backend/` directory (or copy from `.env.example`):

```bash
cp .env.example .env
```

Configure your credentials in `backend/.env`:

```env
GEMINI_API_KEY=your_gemini_api_key_here
DB_USERNAME=postgres
DB_PASSWORD=your_postgres_password_here
# Optional overrides:
# SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/tripplanner
# EMBEDDING_PROVIDER=google # Options: google, ollama, openai-compatible
```

### 3. Build & Run

Run the application using the Gradle wrapper:

```bash
# Start the development server (runs on http://localhost:8080)
./gradlew bootRun

# Run unit and integration tests (uses in-memory vector store, no live DB required)
./gradlew test

# Build executable JAR
./gradlew build
```

---

## Embedding Model Configuration

The backend supports hot-swapping embedding models via environment variables without code changes:

| Provider | `EMBEDDING_PROVIDER` | `EMBEDDING_MODEL` | `EMBEDDING_DIMENSIONS` | Notes |
| :--- | :--- | :--- | :--- | :--- |
| **Google Gemini (Default)** | `google` | `gemini-embedding-001` | `3072` | Cloud-hosted, requires `GEMINI_API_KEY` |
| **Ollama (Local OSS)** | `ollama` | `qwen3-embedding:0.6b` | `1024` | Requires Ollama at `http://localhost:11434` |
| **TEI / vLLM (OSS)** | `openai-compatible` | `Qwen/Qwen3-Embedding-0.6B` | `1024` | Requires TEI at `http://localhost:8000` |

> For in-depth embedding recipes, vector dimensions, and schema partitioning details, see [`docs/EMBEDDINGS.md`](../docs/EMBEDDINGS.md).

---

## REST API Overview

### Itinerary Generation & Persistence
- `POST /api/itinerary/generate`: Synchronous structured JSON itinerary generation.
- `GET /api/itinerary/stream`: Reactive Server-Sent Events (SSE) token streaming for progressive UI rendering.
- `GET /api/itineraries`: Retrieve all saved itineraries.
- `POST /api/itineraries`: Save an itinerary to the database.
- `GET /api/itineraries/{id}`: Retrieve a specific saved itinerary.
- `DELETE /api/itineraries/{id}`: Delete a saved itinerary.

### Knowledge Base & Ingestion (`/api/admin/ingest`)
- `POST /api/admin/ingest/documents`: Ingest structured travel documents with metadata.
- `POST /api/admin/ingest/batch`: Ingest a batch of destination documents.
- `POST /api/admin/ingest/articles`: Ingest raw travel articles into vector space.
- `POST /api/admin/ingest/upload`: Multi-part manual file upload (`.pdf`, `.json`, `.txt`, `.md`).
- `POST /api/admin/ingest/preload`: Preload default datasets from classpath.
- `GET /api/admin/ingest/status`: Check vector store ingestion statistics per destination.
- `GET /api/admin/ingest/similarity-search?query=...&topK=...`: Test vector store similarity retrieval.

## Features

- **Dual-Mode Execution:** Synchronous typed records (`.entity()`) and reactive streaming (`Flux<String>`) over SSE.
- **Provider-Level Structured Output:** Native JSON mode on Google GenAI (`.useProviderStructuredOutput()`) to guarantee schema compliance without markdown wrapping.
- **Dynamic Vector Table Isolation:** Automatically isolates embeddings by provider and dimensions (`vector_store_{provider}_{dimensions}`) to prevent vector dimension collisions.
- **Session-Isolated Chat Memory:** Isolates multi-turn chat sessions per user via `conversationId` UUIDs.

