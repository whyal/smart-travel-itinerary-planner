# Frontend Agent Guidelines & System Boundaries

This document defines the architectural rules, coding standards, and system invariants for any AI agent or developer working on the **Next.js 16 (React 19) Frontend**.

## 1. System Scope & Identity

The frontend client provides an interactive, real-time travel planning interface with SSE streaming chunk parsing, structured day plans, client-side history, and optional database persistence.

### Key Component & File Map

| Role | File Path | Responsibility |
| :--- | :--- | :--- |
| **Main Orchestrator** | [`app/components/ItineraryForm.tsx`](app/components/ItineraryForm.tsx) | Form state management, SSE fetch stream reader, and UI coordinator. |
| **Drawer & Local Storage** | [`app/components/ItineraryHistory.tsx`](app/components/ItineraryHistory.tsx) | Saved itinerary history drawer and `localStorage` DAO. |
| **Structured Cards View** | [`app/components/itinerary/StructuredItineraryView.tsx`](app/components/itinerary/StructuredItineraryView.tsx) | Filterable day-by-day activity and dining cards. |
| **Stream Parser** | [`app/utils/itineraryParser.ts`](app/utils/itineraryParser.ts) | Sanitizes and parses partial/complete streaming AI responses. |
| **REST Client** | [`app/services/itineraryApi.ts`](app/services/itineraryApi.ts) | Fetch wrapper for backend persistence and knowledge endpoints. |
| **Data Types** | [`app/types/itinerary.ts`](app/types/itinerary.ts) | Core TypeScript interfaces aligned with backend Java records. |
| **History Utilities** | [`app/utils/historyStorage.ts`](app/utils/historyStorage.ts) | LocalStorage CRUD operations for itinerary history. |
| **Global Typography** | [`app/lib/fonts.ts`](app/lib/fonts.ts) & [`app/globals.css`](app/globals.css) | Google Fonts (`font-display`, `font-body`, `font-mono`) and Tailwind v4 theme. |

## 2. Hard Boundaries & Invariants (MUST FOLLOW)

No `any`, no synchronous `setState` in `useEffect`, no hardcoded localhost URLs outside `app/services`, and Server-Component-only `page.tsx`/`layout.tsx` are enforced by `npm run lint` ([`eslint.config.mjs`](eslint.config.mjs)); a missing `"use client"` fails `npm run build`.

### ✅ Architectural Requirements (DO)
- **Shared Types**: Define types in [`app/types/itinerary.ts`](app/types/itinerary.ts) matching backend DTO records.
- **SSE Stream Lifecycle Management**:
  - Always maintain an `AbortController` instance.
  - Abort active streams when a user cancels or when the component unmounts.
- **Session Isolation**:
  - Always generate and pass a unique `conversationId` UUID per generation; the backend uses it to save each itinerary as one row.

### 🚫 Forbidden Patterns (DO NOT)
- **DO NOT duplicate Save buttons**: Ensure save-to-database actions are centralized in [`ItineraryOutputView`](app/components/itinerary/ItineraryOutputView.tsx) and [`ItineraryHistory`](app/components/ItineraryHistory.tsx).

## 3. Verification & Validation Commands

Always run and verify the following commands after making frontend changes:

```bash
# 1. Run Unit and Component Tests
npm test

# 2. Check TypeScript & Next.js Build
npm run build

# 3. Check Linting Rules
npm run lint

# 4. Check Code Formatting
npx prettier --check .
```

All tests and build steps must complete with exit code `0` before marking a task as done.
