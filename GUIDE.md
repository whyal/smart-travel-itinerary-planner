# 📖 AI Travel Planner — Quickstart & Operator Guide

Welcome! This guide is designed for developers, students, and tech-savvy users who want to run the system, generate AI itineraries, manage travel guides, and **switch embedding models** without headaches.

---

## 1. Quick Start in 3 Steps

### Prerequisites
Make sure you have:
- **Docker Desktop** installed and running
- **Java 21+** (`java -version`)
- **Node.js 18+** (`node -v`)

---

### Step 1: Start the Database (PostgreSQL + pgvector)

From the project root:

```bash
cd backend
# Make sure your .env has DB credentials (DB_USERNAME=postgres, DB_PASSWORD=...)
docker compose up -d
```

*(This starts PostgreSQL with the `pgvector` vector database extension on port `5432`.)*

---

### Step 2: Start the Backend (Spring Boot)

From the `backend/` folder:

```bash
# Export your Gemini API key (free from Google AI Studio)
export GEMINI_API_KEY="your-gemini-api-key"
export DB_USERNAME="postgres"
export DB_PASSWORD="your_postgres_password"

# Launch the backend
./gradlew bootRun
```

The backend starts at `http://localhost:8080`.

---

### Step 3: Start the Frontend (Next.js)

In a new terminal window:

```bash
cd frontend
npm install   # (only needed the first time)
npm run dev
```

Open your browser to **`http://localhost:3000`**. You are ready to plan trips!

---

## 2. Using the System

### A. Generating an Itinerary
1. On the main page, choose your **Destination** (e.g., *Osaka*, *Kyoto*, *Tokyo*).
2. Pick your travel pace, interests, and budget.
3. Click **Generate Streamed Itinerary**.
4. The AI streams back a day-by-day plan with neighborhood grouping, transit directions, and timing.

### B. Using the "Knowledge Base" (RAG)
RAG (Retrieval-Augmented Generation) gives the AI factual knowledge from local guides so it doesn't hallucinate.

1. Click the **"Knowledge Base"** button in the top-right of the planner form.
2. In the drawer that opens, you can:
   - **Upload Guidebooks**: Upload any `.pdf`, `.json`, `.txt`, or `.md` file.
   - **Paste Articles**: Paste travel blog tips or restaurant lists directly into the text box.
   - **Preload Data**: Click **Preload Knowledge** for *Kyoto* to load bundled datasets instantly.
   - **Test Search**: Type a query (e.g., *"quiet zen gardens"*) to see what matching passages the AI will retrieve and check their similarity scores.

---

## 3. How to Switch Embedding Models (The Foolproof Guide)

### Why would you switch?
- **Cloud vs. Local**: You want to run 100% offline for free using **Ollama** instead of Google Gemini.
- **Better Accuracy**: A newer open-source embedding model was released (like `bge-m3`).

### The Golden Rule of Embeddings
> [!IMPORTANT]
> **Embeddings cannot be mixed.** 
> Vectors created by Google Gemini cannot be compared with vectors created by Ollama. 
> 
> **How this app protects you**: The system automatically creates a dedicated database table for each model (e.g. `vector_store_google_3072` vs `vector_store_ollama_1024`). Your old data is never overwritten or deleted!

---

### Ready-to-Use Recipes

You can switch models **with 0 code changes** simply by setting environment variables before running `./gradlew bootRun`:

#### Recipe A: Google Gemini (Default / Cloud)
*Fast, high quality, requires internet & API key.*

```bash
export EMBEDDING_PROVIDER=google
export EMBEDDING_MODEL=gemini-embedding-001
export EMBEDDING_DIMENSIONS=3072
export GEMINI_API_KEY="your-gemini-key"

./gradlew bootRun
```

---

#### Recipe B: Ollama (Free / 100% Offline / Local GPU)
*Runs locally on your laptop with no API keys or costs.*

1. Install [Ollama](https://ollama.com/) and pull your chosen model:
   ```bash
   # Option 1: BGE-M3 (SOTA multilingual model)
   ollama pull bge-m3

   # Option 2: Qwen3 (Fast & lightweight)
   ollama pull qwen3-embedding:0.6b
   ```

2. Start the backend with:
   ```bash
   export EMBEDDING_PROVIDER=ollama
   export EMBEDDING_MODEL=bge-m3        # or qwen3-embedding:0.6b
   export EMBEDDING_DIMENSIONS=1024
   export OLLAMA_BASE_URL=http://localhost:11434

   ./gradlew bootRun
   ```

---

#### Recipe C: OpenAI / OpenAI-Compatible (TEI, vLLM)
*For OpenAI cloud or self-hosted GPU containers.*

```bash
export EMBEDDING_PROVIDER=openai-compatible
export EMBEDDING_MODEL=text-embedding-3-small
export EMBEDDING_DIMENSIONS=1536
export OPENAI_COMPAT_BASE_URL=https://api.openai.com/v1
export OPENAI_COMPAT_API_KEY="sk-..."

./gradlew bootRun
```

---

### What to Do Immediately After Switching Models (Cold Start)

Because the new model writes to a brand new table, that table is initially empty. 

1. Open `http://localhost:3000`.
2. Click **"Knowledge Base"**.
3. Notice the status card now shows your new model and its new table name (e.g. `vector_store_ollama_1024`).
4. Ingest your documents:
   - Go to **Preload Data** and click **Preload Knowledge** for *Kyoto*, or
   - Go to **File Upload** and upload your destination guide.
5. You're done! Your queries now use your new embedding model.

> [!TIP]
> **Switching Back is Instant**: If you switch back to `google`, all your previous Gemini vectors are still waiting in `vector_store_google_3072`—nothing was lost!

---

## 4. Key Configuration Files Reference

If you prefer editing configuration files instead of exporting terminal variables:

| File | Purpose |
| :--- | :--- |
| [`backend/src/main/resources/application.yaml`](backend/src/main/resources/application.yaml) | Main backend configuration where default model, dimensions, and URLs are set. |
| [`backend/.env`](backend/.env) | Local database credentials and API keys. |
| [`frontend/app/services/itineraryApi.ts`](frontend/app/services/itineraryApi.ts) | Frontend API connection (defaults to `http://localhost:8080/api`). |

---

## 5. Troubleshooting & FAQ

#### Q: I generated an itinerary, but it didn't use any of my uploaded spots?
**A**: Make sure the **Destination** in the form matches the destination you tagged when ingesting the document (e.g., "Kyoto"). Also, check the **Test Search** tab in the Knowledge Base drawer to ensure your query scores above `0.68`.

#### Q: Docker database fails with `port 5432 already in use`?
**A**: You have a local PostgreSQL server already running. Either stop your local PostgreSQL (`sudo service postgresql stop`) or change the host port in `docker-compose.yml` to `"5433:5432"`.

#### Q: Ollama says `connection refused`?
**A**: Make sure the Ollama app or daemon is running in the background (`ollama serve`).
