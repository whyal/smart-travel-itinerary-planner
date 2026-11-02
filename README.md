# AI Travel Itinerary Planner

![Next.js](https://img.shields.io/badge/Next.js-16-black?logo=next.js)
![React](https://img.shields.io/badge/React-19-61DAFB?logo=react&logoColor=black)
![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.1-6DB33F?logo=springboot&logoColor=white)
![Spring AI](https://img.shields.io/badge/Spring_AI-2.0-6DB33F?logo=spring&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-pgvector-4169E1?logo=postgresql&logoColor=white)

An intelligent full-stack travel planner that produces personalized day-by-day itineraries. Powered by Spring AI, Google Gemini, PostgreSQL `pgvector`, and Next.js 16, featuring real-time Server-Sent Events (SSE) streaming and dynamic Retrieval-Augmented Generation (RAG).

![Itinerary Generation Interface](https://github.com/user-attachments/assets/3a0c8ffe-d68c-4b31-b5f7-d933f02999f9)

## 📚 Documentation Directory

Choose the guide that matches what you want to do:

| Guide | Purpose | Target Reader |
| :--- | :--- | :--- |
| [🚀 Quickstart & Operator Guide](docs/QUICKSTART.md) | Full 3-step setup, Docker database, Knowledge Base UI walkthrough, model switching recipes, and troubleshooting FAQ. | Anyone running or testing the app |
| [🧠 Modular Embedding Architecture](docs/EMBEDDINGS.md) | Deep dive into embedding abstractions, hot-swapping Cloud vs OSS models, and table isolation in `pgvector`. | AI engineers & backend developers |
| [📡 REST & Streaming API Reference](docs/API.md) | Complete specification for Itinerary generation, SSE streaming, Persistence, and Knowledge Base APIs. | Integrators & full-stack developers |
| [☕ Backend Developer Guide](backend/README.md) | Spring Boot 4 architecture, service layers, Gradle workflows, and backend testing. | Backend developers |
| [⚛️ Frontend Developer Guide](frontend/README.md) | Next.js 16 App Router, React 19 architecture, SSE stream parsing, and UI component hierarchy. | Frontend developers |
| [🤖 Universal Agent Guidelines](AGENTS.md) | Hard invariants, architectural boundaries, and test validation rules for AI agents and contributors. | AI coding assistants & contributors |

## ⚡ Fast Track: Running the System in 3 Steps

### 1. Start the Database (PostgreSQL + pgvector)

```bash
cd backend
docker compose up -d
```

*(Runs PostgreSQL with the `pgvector` extension on port `5432`.)*

### 2. Start the Backend (Spring Boot)

```bash
cd backend
export GEMINI_API_KEY="your-gemini-api-key"
export DB_USERNAME="postgres"
export DB_PASSWORD="your_postgres_password"

./gradlew bootRun
```

*(Starts the REST & SSE API on `http://localhost:8080`.)*

### 3. Start the Frontend (Next.js)

```bash
cd frontend
npm install
npm run dev
```

Open **`http://localhost:3000`** in your browser.

> For complete details, prerequisites, and WSL2 tips, see the [Quickstart & Operator Guide](docs/QUICKSTART.md).

## ✨ Key Features

- **Progressive Streaming:** Uses reactive Server-Sent Events (`POST /api/itinerary/stream`) to stream generated itinerary chunks in real time without lag.
- **Dynamic RAG Grounding:** Upload travel guides (`.pdf`, `.json`, `.txt`, `.md`) or paste articles to ground recommendations with authentic local spots.
- **Hot-Swappable Embeddings:** Switch between Google Gemini, local Ollama (`qwen3-embedding:0.6b`), or OpenAI/TEI containers with zero code changes.
- **Vector Space Isolation:** PostgreSQL dynamically routes embeddings into isolated tables (`vector_store_{provider}_{dimensions}`) to prevent vector dimension collisions.
- **Dual Presentation Views:** Toggle between structured, filterable day-by-day activity cards and a raw streaming terminal output.
- **Session Persistence:** Save itineraries to PostgreSQL or keep them in browser `localStorage`.

## 🏗️ System Architecture

```
smart-travel-itinerary-planner/
├── frontend/             # Next.js 16 (React 19, Tailwind CSS v4, TypeScript)
│   ├── app/              # App Router, components, SSE stream parser, services
│   └── README.md         # Frontend developer guide
├── backend/              # Spring Boot 4 + Spring AI 2.0 (Java 21)
│   ├── src/              # Controllers, services, embedding config, repositories
│   └── README.md         # Backend developer guide
├── docs/                 # Centralized documentation
│   ├── QUICKSTART.md     # Full operator guide & troubleshooting
│   ├── EMBEDDINGS.md     # Vector embedding & model switching architecture
│   └── API.md            # REST & SSE endpoint reference
└── AGENTS.md             # Universal guidelines & invariants for AI agents
```

## 🧪 Verification & Testing

Verify that all subsystems pass tests before submitting changes:

```bash
# Verify Backend (in-memory vector store, no live DB needed)
cd backend && ./gradlew test

# Verify Frontend (unit tests, build, lint)
cd frontend
npm test
npm run build
npm run lint
```
