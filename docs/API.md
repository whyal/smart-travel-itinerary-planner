# REST & Streaming API Reference

This document provides the complete API reference for the AI Travel Itinerary Planner backend (`http://localhost:8080/api`).

All endpoints accept and return JSON unless explicitly specified otherwise (such as Server-Sent Events or file uploads).

## Endpoint Summary

| Category | Method | Path | Description |
| :--- | :--- | :--- | :--- |
| **Itinerary** | `POST` | `/api/itinerary/generate` | Synchronous structured JSON itinerary generation. |
| **Itinerary** | `POST` | `/api/itinerary/stream` | Server-Sent Events (SSE) token streaming for real-time progressive UI rendering. |
| **Persistence** | `GET` | `/api/itineraries` | Retrieve all saved itineraries. |
| **Persistence** | `POST` | `/api/itineraries` | Save an itinerary and its metadata to PostgreSQL. |
| **Persistence** | `GET` | `/api/itineraries/{id}` | Retrieve a saved itinerary by its ID. |
| **Persistence** | `DELETE` | `/api/itineraries/{id}` | Delete a saved itinerary by its ID. |
| **Knowledge** | `POST` | `/api/knowledge/documents` | Ingest structured travel documents with metadata into vector space. |
| **Knowledge** | `POST` | `/api/knowledge/batch` | Batch ingest structured travel documents across multiple destinations. |
| **Knowledge** | `POST` | `/api/knowledge/articles` | Ingest raw travel text/articles into vector space. |
| **Knowledge** | `POST` | `/api/knowledge/upload` | Multipart file upload (`.pdf`, `.json`, `.txt`, `.md`). |
| **Knowledge** | `POST` | `/api/knowledge/preload` | Preload bundled classpath datasets (e.g., Kyoto). |
| **Knowledge** | `GET` | `/api/knowledge/status` | Ingestion statistics, active embedding model, and vector table name. |
| **Knowledge** | `GET` | `/api/knowledge/similarity-search` | Test vector similarity search directly without generating an itinerary. |

## 1. Itinerary Generation Endpoints

### A. Synchronous Generation (`POST /api/itinerary/generate`)

Generates a fully formed structured JSON itinerary in a single synchronous response.

#### Request Headers
```http
Content-Type: application/json
```

#### Request Body
```json
{
  "prompt": "Plan a 3-day cultural trip to Kyoto with moderate pace and medium budget.",
  "conversationId": "3fa85f64-5717-4562-b3fc-2c963f66afa6"
}
```

- `prompt` *(string, required)*: User prompt containing destination and preferences.
- `conversationId` *(string, optional)*: UUID tracking multi-turn chat history. If omitted, no history is retained.

#### Response (`200 OK`)
```json
{
  "destination": "Kyoto",
  "summary": "A 3-day exploration of Kyoto's historic temples, serene gardens, and traditional culinary scenes.",
  "estimatedBudget": "Moderate",
  "travelPace": "Moderate",
  "days": [
    {
      "dayNumber": 1,
      "theme": "Historic Higashiyama & Gion",
      "activities": [
        {
          "timeSlot": "Morning",
          "title": "Kiyomizu-dera Temple",
          "description": "Walk through the wooden stage overlooking Kyoto.",
          "location": "Higashiyama",
          "duration": "2 hours"
        }
      ],
      "dining": [
        {
          "meal": "Lunch",
          "recommendation": "Gion Karyo",
          "cuisine": "Traditional Kaiseki",
          "location": "Gion"
        }
      ]
    }
  ]
}
```

### B. Streaming Generation (`POST /api/itinerary/stream`)

Streams generated tokens as Server-Sent Events (`text/event-stream`). Enables progressive client-side rendering.

#### Request Headers
```http
Content-Type: application/json
Accept: text/event-stream
```

#### Request Body
Same schema as `POST /api/itinerary/generate`.

#### Response Stream (`200 OK`)
```http
Content-Type: text/event-stream;charset=UTF-8
```

The server streams raw text tokens or JSON fragments line-by-line. The stream terminates automatically when generation completes.

```bash
curl -N -X POST "http://localhost:8080/api/itinerary/stream" \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Create a 2-day Tokyo itinerary focusing on tech and modern art.",
    "conversationId": "3fa85f64-5717-4562-b3fc-2c963f66afa6"
  }'
```

## 2. Saved Itineraries (Persistence) Endpoints

### A. List All Saved Itineraries (`GET /api/itineraries`)

Returns all itineraries stored in the database, ordered by creation date descending.

#### Response (`200 OK`)
```json
[
  {
    "id": 1,
    "conversationId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "destination": "Kyoto",
    "daysCount": 3,
    "formData": {
      "destination": "Kyoto",
      "days": 3,
      "pace": "moderate",
      "budget": "medium",
      "interests": ["culture", "food"]
    },
    "itinerary": { ... },
    "rawText": "...",
    "createdAt": "2026-11-02T12:00:00Z"
  }
]
```

### B. Save Itinerary (`POST /api/itineraries`)

Persists a generated itinerary to PostgreSQL.

#### Request Body
```json
{
  "conversationId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "destination": "Kyoto",
  "daysCount": 3,
  "formData": {
    "destination": "Kyoto",
    "days": 3,
    "pace": "moderate",
    "budget": "medium",
    "interests": ["culture", "food"]
  },
  "itinerary": { ... },
  "rawText": "...",
  "createdAt": "2026-11-02T12:00:00Z"
}
```

#### Response (`200 OK`)
Returns the persisted entity with its generated `id`.

### C. Get Itinerary by ID (`GET /api/itineraries/{id}`)

Retrieves a single saved itinerary. Returns `404 Not Found` if the ID does not exist.

### D. Delete Itinerary (`DELETE /api/itineraries/{id}`)

Deletes the specified saved itinerary. Returns `204 No Content` on success.

## 3. Knowledge Base & Vector Store Endpoints

All RAG knowledge endpoints manage documents in the currently active embedding model's isolated table.

### A. Ingest Structured Documents (`POST /api/knowledge/documents`)

Ingests an array of rich travel items into the vector store.

#### Query Parameters
- `destination` *(string, optional)*: Overrides destination tag if not present in item objects.

#### Request Body
```json
[
  {
    "id": "kyoto-fushimi-inari",
    "destination": "Kyoto",
    "title": "Fushimi Inari Taisha",
    "category": "shrine",
    "district": "Fushimi",
    "content": "Famous Shinto shrine dedicated to the god of rice, renowned for its thousands of vermilion torii gates winding up Mount Inari.",
    "tags": ["shrine", "historic", "torii-gates"],
    "suggestedDuration": "2-3 hours",
    "bestTimeToVisit": "Early morning or dusk to avoid crowds",
    "metadata": {
      "admission": "Free",
      "nearestStation": "JR Inari Station"
    }
  }
]
```

### B. Ingest Free-Form Articles (`POST /api/knowledge/articles`)

Quickly ingests raw blog posts or guide notes.

#### Request Body
```json
{
  "destination": "Osaka",
  "articles": [
    "Dotonbori is Osaka's most vibrant nightlife and dining district, famous for neon signs and street food.",
    "Osaka Castle is a historic fortress surrounded by moats and cherry blossom gardens."
  ]
}
```

### C. Upload Guidebook Files (`POST /api/knowledge/upload`)

Uploads documents via `multipart/form-data`. Automatically handles format-specific parsing:
- `.pdf`: Page-by-page token splitting.
- `.json`: Structured array parsing.
- `.txt`, `.md`: Paragraph splitting by blank lines.

```bash
curl -X POST "http://localhost:8080/api/knowledge/upload" \
  -F "file=@/path/to/kyoto_guide.pdf" \
  -F "destination=Kyoto"
```

### D. Preload Bundled Knowledge (`POST /api/knowledge/preload`)

Ingests prepackaged classpath datasets into the active vector table.

```bash
curl -X POST "http://localhost:8080/api/knowledge/preload?destination=Kyoto"
```

### E. Check Knowledge Status (`GET /api/knowledge/status`)

Returns real-time ingestion counts, active provider, model dimensions, and target vector table.

#### Response (`200 OK`)
```json
{
  "status": "ready",
  "totalDocumentsIngested": 12,
  "destinations": ["Kyoto", "Tokyo"],
  "documentCountByDestination": {
    "Kyoto": 8,
    "Tokyo": 4
  },
  "embeddingProvider": "ollama",
  "embeddingModel": "qwen3-embedding:0.6b",
  "embeddingDimensions": 1024,
  "vectorTable": "vector_store_ollama_1024"
}
```

### F. Test Similarity Search (`GET /api/knowledge/similarity-search`)

Runs a semantic vector query directly without calling the chat generation model.

#### Query Parameters
- `query` *(string, required)*: Search phrase (e.g., `zen gardens with stone paths`).
- `topK` *(integer, optional, default: 4)*: Maximum number of retrieved document chunks.

#### Response (`200 OK`)
```json
[
  {
    "id": "kyoto-ryoanji",
    "content": "Ryoan-ji is renowned for its famous dry landscape rock garden...",
    "metadata": {
      "destination": "Kyoto",
      "title": "Ryoan-ji Temple",
      "category": "temple",
      "distance": 0.8124
    }
  }
]
```

## 4. Error Handling Protocol

When an error occurs, the backend returns a standardized `ApiErrorResponse` payload:

```json
{
  "timestamp": "2026-11-02T14:32:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Prompt must not be empty.",
  "path": "/api/itinerary/generate"
}
```

Common status codes:
- `400 Bad Request`: Validation failure or missing required fields.
- `404 Not Found`: Itinerary ID does not exist.
- `500 Internal Server Error`: AI provider failure or database communication error.
