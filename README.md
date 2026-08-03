#  AI Travel Itinerary Planner (Frontend)

The web client interfaces with a backend Spring AI backend to provide a type-safe travel planning experience.


##  Key Features

- **Progressive UI Rendering:** Consumes the backend's `text/event-stream` SSE endpoint (`/api/itinerary/stream`) to display itinerary days, activities, and dining options in real-time without waiting for full generation.
- **Dynamic Session Isolation:** Automatically generates and persists unique `conversationId` UUIDs per user session, ensuring multi-turn chat memory remains isolated when communicating with the backend's `MessageChatMemoryAdvisor`.

![Next.js](https://img.shields.io/badge/Next.js_16-000000?style=flat&logo=nextdotjs&logoColor=white)
![React](https://img.shields.io/badge/React_19-20232A?style=flat&logo=react&logoColor=61DAFB)
![TypeScript](https://img.shields.io/badge/TypeScript-3178C6?style=flat&logo=typescript&logoColor=white)
![Tailwind CSS](https://img.shields.io/badge/Tailwind_CSS_v4-06B6D4?style=flat&logo=tailwindcss&logoColor=white)

<div align="center">
  <table>
    <tr>
      <td>
        <img width="600" alt="Itinerary generation form" src="https://github.com/user-attachments/assets/3a0c8ffe-d68c-4b31-b5f7-d933f02999f9" />
        <p align="center"><em>Figure 1: User form desired destination and relevant information.</em></p>
      </td>
    </tr>
    <tr>
      <td>
        <img width="600" alt="Generated Itinerary" src="https://github.com/user-attachments/assets/d17189dd-9a4e-4c1c-9fb6-03eedd130934" /> 
        <p align="center"><em>Figure 2: Generated itinerary of the user's desired destination.</em></p>
      </td>
    </tr>
  </table>
</div>
