#  AI Travel Itinerary Generator — Frontend Client

The web interface for the AI Travel Itinerary Generator, built with **Next.js (App Router)**, **TypeScript**, and **Tailwind CSS**. 

This client interfaces with a backend Spring AI service to provide a type-safe travel planning experience. It supports dynamic UUID session management and consumes Server-Sent Events (SSE) to render travel itineraries progressively as the AI generates them.

---

##  Key Features

- **Progressive UI Rendering:** Consumes the backend's `text/event-stream` SSE endpoint (`/api/itinerary/stream`) to display itinerary days, activities, and dining options in real-time without waiting for full generation.
- **Type-Safe State Management:** Matches TypeScript interfaces 1:1 with the backend's Java 25+ Records (`Itinerary`, `PromptRequest`) to prevent runtime schema mismatches.
- **Dynamic Session Isolation:** Automatically generates and persists unique `conversationId` UUIDs per user session, ensuring multi-turn chat memory remains isolated when communicating with the backend's `MessageChatMemoryAdvisor`.
- **Responsive & Accessible UI:** Styled with Tailwind CSS for clean, adaptable layouts across mobile, tablet, and desktop viewports.

---

##  Tech Stack

- **Framework:** Next.js (App Router)
- **Language:** TypeScript
- **Styling:** Tailwind CSS
- **Networking:** Native Fetch API (supporting `application/json` and `text/event-stream`)
- **State Management:** React Hooks (`useState`, `useEffect`, `useRef` for SSE stream reading)

## Depiction of the itinerary planner

<div align="center">
  <table>
    <tr>
      <td>
        <img width="600" alt="Screenshot 2026-07-24 234201" src="https://github.com/user-attachments/assets/92591192-f345-4307-b896-22ac7d810363" />        
        <p align="center"><em>Figure 1: User form desired destination and relevant information.</em></p>
      </td>
    </tr>
    <tr>
      <td>
        <img width="600" alt="Screenshot 2026-07-24 234209" src="https://github.com/user-attachments/assets/6bb170d5-d2af-40a2-9141-686d44090d72" />        
        <p align="center"><em>Figure 2: Generated itinerary of the user's desired destination.</em></p>
      </td>
    </tr>
  </table>
</div>
