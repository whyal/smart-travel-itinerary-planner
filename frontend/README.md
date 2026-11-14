# AI Travel Itinerary Planner — Frontend Client

Interactive web client built with **Next.js 16 (App Router)**, **React 19**, **TypeScript**, and **Tailwind CSS v4**. It interfaces with the Spring Boot backend to deliver real-time streaming itinerary generation, structured day plans, client-side history caching, and optional database persistence.

![Itinerary Generation Interface](https://github.com/user-attachments/assets/3a0c8ffe-d68c-4b31-b5f7-d933f02999f9)

## Prerequisites

- **Node.js 20+** (or Node.js 22 LTS recommended)
- **npm** (or yarn / pnpm)
- A running instance of the [Backend Service](../backend/README.md) (default: `http://localhost:8080`)

## Quick Start & Setup

### 1. Configure Environment Variables

Create a `.env.local` file in the `frontend/` directory (or copy from `.env.example`):

```bash
cp .env.example .env.local
```

Set the backend API URL:

```env
NEXT_PUBLIC_API_URL=http://localhost:8080/api
NEXT_PUBLIC_API_KEY=dev-local-key
```

### 2. Install Dependencies

```bash
npm install
```

### 3. Run Development Server

```bash
npm run dev
```

Open [http://localhost:3000](http://localhost:3000) in your browser.

### 4. Build for Production

```bash
# Verify TypeScript types and compile Next.js production bundle
npm run build

# Start production server
npm start
```

## Architecture & Component Map

The application follows Next.js App Router patterns with clear separation between Server and Client components:

```
frontend/app/
├── components/
│   ├── ItineraryForm.tsx              # Main orchestrator: form state, SSE streaming, conversationId
│   ├── ItineraryHistory.tsx           # Drawer for browsing and loading saved trip history
│   ├── itinerary/
│   │   ├── ItineraryFormFields.tsx    # Destination, dates, budget, pace inputs
│   │   ├── StructuredItineraryView.tsx# Filterable day-by-day activity and dining cards
│   │   ├── DayPlanCard.tsx            # Individual day plan card with morning/afternoon/evening slots
│   │   ├── ItineraryLoadingSkeleton.tsx# Animated loading skeleton during generation
│   │   ├── ItineraryOutputView.tsx    # Container coordinating structured view and save actions
│   │   └── ErrorBanner.tsx            # Error alert with retry suggestions
│   └── knowledge/
│       └── DocumentIngestionDrawer.tsx# Modal drawer for file uploads, articles, and RAG status
├── services/
│   ├── itineraryApi.ts                # Fetch wrapper for SSE streaming and itinerary persistence
│   └── knowledgeApi.ts                # Fetch wrapper for RAG file upload, article ingestion, and search
├── utils/
│   ├── itineraryParser.ts             # Parser handling partial SSE chunks and markdown cleaning
│   └── historyStorage.ts              # LocalStorage DAO for client-side trip history
├── types/
│   ├── itinerary.ts                   # TypeScript interfaces matching backend Java DTO records
│   └── knowledge.ts                   # Interfaces for documents, upload status, and search results
├── layout.tsx                         # Root Server Component shell (fonts, metadata)
└── page.tsx                           # Main Server Component page mounting ItineraryForm
```

## SSE Streaming Pipeline

Real-time streaming is handled through a reactive pipeline:

1. **Trigger**: When the user clicks "Generate Streamed Itinerary", `ItineraryForm.tsx` creates an `AbortController` and invokes `streamItinerary()` in `app/services/itineraryApi.ts`.
2. **Transfer**: The backend streams chunks as `text/event-stream` from `POST /api/itinerary/stream`.
3. **Chunk Processing**: The frontend reads chunks via a `ReadableStreamDefaultReader` and `TextDecoder`, stripping SSE `data:` prefixes while collecting tokens.
4. **Parsing**: `app/utils/itineraryParser.ts` continuously sanitizes raw text (stripping markdown code fences like ` ```json `), extracts valid JSON substrings, and attempts non-blocking progressive parse updates so cards render incrementally.
5. **Session Isolation**: A unique `conversationId` UUID is generated per session (`crypto.randomUUID()`) and passed with each request, allowing Spring AI's multi-turn chat memory advisors to retain context without cross-user leakage.

## Testing & Quality Verification

```bash
# Run unit and component tests (Jest + React Testing Library)
npm test

# Run ESLint validation
npm run lint

# Check code formatting
npx prettier --check .
```

When contributing, ensure adherence to the [Frontend Agent Guidelines](AGENTS.md) (strict TypeScript types matching backend DTO records, explicit `"use client"` directives, and proper `AbortController` cleanups).

## Visual Previews

![Generated Itinerary Display](https://github.com/user-attachments/assets/d17189dd-9a4e-4c1c-9fb6-03eedd130934)
*Figure: Day-by-day structured activity cards and dining recommendations.*
