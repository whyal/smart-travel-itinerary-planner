# AI Travel Itinerary Planner (Frontend)

Interactive web client built with **Next.js 16 (App Router)**, **React 19**, and **Tailwind CSS v4**. It interfaces with the Spring Boot backend to deliver real-time streaming itinerary generation, structured day plans, client history caching, and optional database persistence.

---

## Prerequisites

Ensure you have the following installed:

- **Node.js 20+** (or Node.js 22 LTS)
- **npm** (or yarn / pnpm)
- A running instance of the **Backend API** (default: `http://localhost:8080`)

---

## Getting Started & Setup

### 1. Environment Variables

Create a `.env.local` file in the `frontend/` directory (or copy from `.env.example`):

```bash
cp .env.example .env.local
```

Configure the backend endpoint URL:

```env
NEXT_PUBLIC_API_URL=http://localhost:8080/api
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

### 4. Build & Production

```bash
# Compile and check TypeScript types
npm run build

# Start production server
npm start
```

---

## Testing & Verification

```bash
# Run unit and component tests (Jest + React Testing Library)
npm test

# Run ESLint checks
npm run lint

# Check formatting
npx prettier --check .
```

---

## Key Features

- **Progressive UI Rendering:** Consumes the backend's `text/event-stream` SSE endpoint (`/api/itinerary/stream`) to progressively render itinerary days, activities, and dining options in real time.
- **Dynamic Session Isolation:** Automatically generates and persists unique `conversationId` UUIDs per session, keeping multi-turn chat memory isolated on the backend.
- **Dual View Support:** Easily toggle between structured, filterable day-by-day interactive cards and a raw stream terminal view.
- **Local & Remote Persistence:** Manage itinerary history locally via `localStorage` or persist directly to PostgreSQL via backend REST APIs.

---

## Screenshots

![Itinerary generation form](https://github.com/user-attachments/assets/3a0c8ffe-d68c-4b31-b5f7-d933f02999f9)
*Figure 1: User destination and preference input form.*

![Generated Itinerary](https://github.com/user-attachments/assets/d17189dd-9a4e-4c1c-9fb6-03eedd130934)
*Figure 2: Generated itinerary display.*

