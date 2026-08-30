<!-- BEGIN:nextjs-agent-rules -->
# This is NOT the Next.js you know

This version has breaking changes — APIs, conventions, and file structure may all differ from your training data. Read the relevant guide in `node_modules/next/dist/docs/` before writing any code. Heed deprecation notices.
<!-- END:nextjs-agent-rules -->

# AGENTS.md — AI Agent Guide for AI Travel Itinerary Planner Frontend

Welcome! This document serves as the guide for AI agents and developers working on the **AI Travel Itinerary Generator Frontend**. It outlines the project architecture, directory structure, development workflows, testing commands, and coding conventions.

---

## 1. Project Overview & Architecture

This repository contains the frontend client for the **AI Travel Itinerary Generator**, built with **Next.js 16 (App Router)**, **React 19**, **TypeScript**, and **Tailwind CSS v4**.

### Key Architecture & Features
- **Backend Integration**: Communicates with a Spring Boot / Spring AI backend service:
  - **SSE Streaming (`text/event-stream`)**: Consumes live streaming itinerary responses to render plans dynamically as the AI generates them.
  - **REST Persistence API**: Posts saved itineraries (`POST /api/itineraries`) via `saveItineraryToDatabase` in [itineraryApi.ts](app/services/itineraryApi.ts).
- **Session Isolation**: Manages a unique `conversationId` UUID per user session stored in `localStorage` to isolate multi-turn memory in backend Spring AI chat memory advisors.
- **Parsing & Real-Time Rendering**: Formats raw stream chunks via [itineraryParser.ts](app/utils/itineraryParser.ts) into structured [ItineraryResponse](app/types/itinerary.ts) types, allowing toggleable switching between **Structured View** and **Raw Stream View**.
- **Local History & Persistence**: Supports client-side history via `localStorage` as well as optional automated/manual backend DB persistence.

---

## 2. Directory Structure

```
.
├── app/
│   ├── components/
│   │   ├── ItineraryForm.tsx          # Main client component handling form state & SSE streaming
│   │   ├── ItineraryHistory.tsx       # Saved itinerary drawer & localStorage utilities
│   │   ├── itinerary/
│   │   │   ├── DayPlanCard.tsx        # Card component displaying single-day activities & dining
│   │   │   ├── ErrorBanner.tsx        # User-facing stream/network error banner
│   │   │   ├── ItineraryFormFields.tsx# Form inputs (destination, days, pace, budget, interests)
│   │   │   ├── ItineraryLoadingSkeleton.tsx # Skeleton loading animation during initial stream
│   │   │   ├── ItineraryOutputView.tsx # Output container switching between structured & raw views
│   │   │   ├── RawStreamView.tsx      # Pre-formatted raw text stream viewer
│   │   │   └── StructuredItineraryView.tsx # Filterable day-by-day structured cards view
│   │   └── __tests__/
│   │       └── ItineraryForm.test.tsx # React Testing Library component integration tests
│   ├── lib/
│   │   └── fonts.ts                   # Google Fonts configuration (Fraunces, Inter, IBM Plex Mono)
│   ├── services/
│   │   └── itineraryApi.ts            # Fetch wrapper for backend REST API requests
│   ├── types/
│   │   └── itinerary.ts               # Core TypeScript interfaces matching backend models
│   ├── utils/
│   │   ├── historyStorage.ts          # LocalStorage persistence utilities for saved history
│   │   ├── itineraryParser.ts         # Stream text parser for structured itinerary extraction
│   │   └── __tests__/
│   │       └── itineraryParser.test.ts # Parser unit tests
│   ├── globals.css                    # Tailwind CSS directives & global styling rules
│   ├── layout.tsx                     # Root App Router layout with fonts & metadata
│   └── page.tsx                       # App homepage (Server Component)
├── public/                            # Static assets
├── eslint.config.mjs                  # ESLint flat configuration
├── jest.config.js & jest.setup.js     # Jest test runner setup
├── next.config.ts                     # Next.js configuration
├── package.json                       # Dependencies & npm scripts
└── tsconfig.json                      # TypeScript compiler options
```

---

## 3. Development & Verification Workflows

### Command Suite
- **Development Server**: `npm run dev`
- **Build & Compile**: `npm run build`
- **Run Unit/Integration Tests**: `npm test` (or `npx jest`)
- **Linting**: `npm run lint`

### Mandatory Verification Checklist
Before completing any coding task:
1. **Run Unit Tests**: Execute `npm test` and verify that all test suites pass.
2. **Build Verification**: Run `npm run build` to ensure there are no TypeScript errors or Next.js build issues.
3. **Lint Check**: Run `npm run lint` and resolve any flagged code issues or warnings.

---

## 4. Coding Conventions & Guidelines

### TypeScript & Types
- **Strict Typing**: Maintain strict TypeScript checking (`"strict": true` in `tsconfig.json`). Avoid using `any`; define explicit interfaces in [itinerary.ts](app/types/itinerary.ts).
- **Backend Model Alignment**: Keep TypeScript types in sync 1:1 with backend Spring Boot records (`ItineraryResponse`, `DayPlan`, `Activity`, `ItineraryFormData`).
- **Path Aliases**: Use `@/*` for imports mapping to the project root (e.g., `@/app/types/itinerary`).

### React & Next.js Standards
- **Component Classification**:
  - Keep route files (`app/page.tsx`, `app/layout.tsx`) as **Server Components** for SEO metadata rendering.
  - Mark interactive UI components with `"use client"` directive.
- **Effect Hygiene**: Avoid triggering synchronous `setState` inside `useEffect` bodies to prevent cascading re-renders.
- **Stream & Memory Management**: Always attach cleanup logic (e.g., calling `abortController.abort()`) when canceling SSE streams or unmounting stream components.

### UI & Styling Standards
- **Tailwind CSS v4**: Use utility-first styling with Tailwind CSS. Follow established color palettes (`slate-50`, `slate-900`, `teal-600`, etc.).
- **Typography**: Apply Google Font CSS variables (`font-display`, `font-body`, `font-mono`) defined in [fonts.ts](app/lib/fonts.ts) and configured via `@theme` in [globals.css](app/globals.css).
- **Icons**: Use icons imported from `lucide-react`.

---

## 5. Environment & API Configuration

- `.env.local` sets `NEXT_PUBLIC_API_URL` (default: `http://localhost:8080/api`).
- Endpoints:
  - SSE Stream: `${NEXT_PUBLIC_API_URL}/itinerary/stream`
  - Save Itinerary: `POST ${NEXT_PUBLIC_API_URL}/itineraries`
