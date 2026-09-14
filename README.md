# AI Travel Planner

![Next.js](https://img.shields.io/badge/Next.js-16-black?logo=next.js)
![React](https://img.shields.io/badge/React-19-61DAFB?logo=react&logoColor=black)
![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.1-6DB33F?logo=springboot&logoColor=white)
![Spring AI](https://img.shields.io/badge/Spring_AI-2.0-6DB33F?logo=spring&logoColor=white)
![pgvector](https://img.shields.io/badge/PostgreSQL-pgvector-4169E1?logo=postgresql&logoColor=white)

A travel planning application that generates personalized itineraries based on user preferences. Built with a React/Next.js frontend and a Spring Boot backend integrating Google Gemini, Retrieval-Augmented Generation (RAG), and vector search.

---

## Features

- AI-generated travel itineraries based on budget, dates, and interests
- Modular embedding architecture supporting Google Gemini and local OSS models (e.g., `Qwen3-Embedding-0.6B`)
- Day-by-day itinerary breakdown
- RESTful API architecture with Server-Sent Events (SSE) streaming for real-time generation

---

## Tech Stack

### Frontend
- Next.js 16 (App Router)
- React 19
- TypeScript
- Tailwind CSS

### Backend
- Java 21
- Spring Boot 4
- Spring AI 2.0
- Google Gemini
- PostgreSQL with pgvector

---

## Project Structure

| Module | Description |
| --- | --- |
| `frontend/` | Web client built with Next.js and React |
| `backend/` | Spring Boot REST API with AI integration |

For implementation details, setup instructions, and architecture, see the README inside each module.

---

## Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/whyal/smart-travel-itinerary-planner.git
cd smart-travel-itinerary-planner
```

### 2. Start the backend

```bash
cd backend
# Follow instructions in backend/README.md
```

### 3. Start the frontend

```bash
cd frontend
# Follow instructions in frontend/README.md
```
