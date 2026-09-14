# AI Travel Itinerary Planner (Frontend)

The web client interfaces with the Spring Boot / Spring AI backend to provide a travel planning user interface.

---

## Key Features

- **Progressive UI Rendering:** Consumes the backend's `text/event-stream` SSE endpoint (`/api/itinerary/stream`) to display itinerary days, activities, and dining options in real time during generation.
- **Dynamic Session Isolation:** Automatically generates and persists unique `conversationId` UUIDs per user session, ensuring multi-turn chat memory remains isolated when communicating with the backend's `MessageChatMemoryAdvisor`.

---

## Screenshots

![Itinerary generation form](https://github.com/user-attachments/assets/3a0c8ffe-d68c-4b31-b5f7-d933f02999f9)
*Figure 1: User destination and preference input form.*

![Generated Itinerary](https://github.com/user-attachments/assets/d17189dd-9a4e-4c1c-9fb6-03eedd130934)
*Figure 2: Generated itinerary display.*
