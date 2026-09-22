# AI Travel Itinerary Planner (Frontend)

The web client interfaces with the Spring Boot / Spring AI backend to provide a travel planning user interface.

## Key Features

- **Progressive UI Rendering:** Consumes the backend's `text/event-stream` SSE endpoint (`/api/itinerary/stream`) to display itinerary days, activities, and dining options in real time during generation.
- **Dynamic Session Isolation:** Automatically generates and persists unique `conversationId` UUIDs per user session, ensuring multi-turn chat memory remains isolated when communicating with the backend's `MessageChatMemoryAdvisor`.
