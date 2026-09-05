# Universal Agent Guidelines & Project Invariants

Welcome! This repository is the **AI Travel Itinerary Planner**, a full-stack application featuring a Spring Boot 4 / Spring AI backend and a Next.js 16 (React 19) frontend with dynamic RAG embeddings and SSE streaming.

This document serves as the universal, agent-agnostic entrypoint for any AI coding assistant or model (e.g. Gemini, Claude, GPT, DeepSeek, Qwen, Llama, Cursor, Copilot, Antigravity, Aider).

---

## 1. System Architecture & Subsystem Index

```
smart-travel-itinerary-planner/
├── backend/          # Spring Boot 4 + Spring AI 2.0 (Java 21)
├── frontend/         # Next.js 16 App Router + React 19 + Tailwind CSS v4 (TypeScript)
├── docs/             # System documentation (e.g. EMBEDDINGS.md)
└── AGENTS.md         # Root instruction and boundary definition
```

### Module Navigation
When working on specific subsystems, strictly adhere to the domain boundary files:
- **Backend Guidelines & Invariants**: [`backend/AGENTS.md`](backend/AGENTS.md)
- **Frontend Guidelines & Invariants**: [`frontend/AGENTS.md`](frontend/AGENTS.md)
- **Modular Embedding Architecture**: [`docs/EMBEDDINGS.md`](docs/EMBEDDINGS.md)

---

## 2. Universal Hard Boundaries (Non-Negotiables)

All AI agents and models working on this codebase **MUST** follow these invariants:

1. 🚫 **No Hardcoded Secrets or Local Endpoints**:
   - Never commit API keys (`GEMINI_API_KEY`, etc.) or hardcode local URLs.
   - Always resolve environment variables with sensible defaults (e.g. `process.env.NEXT_PUBLIC_API_URL` or `${SPRING_DATASOURCE_URL:...}`).
2. 🚫 **No Cross-Layer Boundary Violations**:
   - Keep Frontend (client UI/state/SSE parsing) and Backend (business logic/PostgreSQL/VectorStore/AI ChatClient) decoupled.
   - Match DTO types 1:1 between backend Java records and frontend TypeScript interfaces.
3. 🚫 **No Unvalidated Code Changes**:
   - Never consider a task finished without running the corresponding verification test suite.
4. ✅ **Preserve Established Architecture & Conventions**:
   - Respect Spring Boot dependency injection and configuration properties.
   - Respect React Server Components vs. Client Components conventions in Next.js 16.

---

## 3. Standard Verification Commands

Any agent modifying code must execute and verify the respective test suites:

| Subsystem | Command | Purpose |
| :--- | :--- | :--- |
| **Backend** | `./gradlew test` (from `/backend`) | Runs all unit and integration tests (in-memory, no live DB needed). |
| **Frontend Tests** | `npm test` (from `/frontend`) | Runs Jest unit and component tests. |
| **Frontend Build** | `npm run build` (from `/frontend`) | Validates TypeScript types and Next.js bundle compilation. |
| **Frontend Lint** | `npm run lint` (from `/frontend`) | Validates ESLint rules. |

---

## 4. Model Switching & Embedding Invariants
- The system supports hot-swapping embedding models (Google Gemini, Ollama OSS `Qwen3-Embedding-0.6B`, OpenAI-compatible/TEI) without application code changes.
- Vector spaces are dynamically isolated in PostgreSQL `pgvector` via `vector_store_{provider}_{dimensions}`.
- For complete embedding instructions and recipes, see [`docs/EMBEDDINGS.md`](docs/EMBEDDINGS.md).
