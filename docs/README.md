# Documentation Hub

Welcome to the AI Travel Itinerary Planner documentation. This directory contains detailed architectural guides, API specifications, and operator runbooks for developing, deploying, and managing the application.

## Documentation Index

| Guide | Description | Target Audience |
| :--- | :--- | :--- |
| [🚀 Quickstart & Operator Guide](QUICKSTART.md) | End-to-end setup in 3 steps, Docker setup, Knowledge Base UI walkthrough, model switching recipes, and troubleshooting FAQ. | Anyone running or testing the app |
| [🧠 Modular Embedding Architecture](EMBEDDINGS.md) | Deep dive into embedding provider abstraction, zero-downtime model switching, dynamic vector table isolation, and HNSW indexing in `pgvector`. | Backend developers & AI engineers |
| [📡 REST & Streaming API Reference](API.md) | Complete OpenAPI-style reference for all Itinerary, Knowledge Base, and Persistence endpoints with request/response schemas and curl examples. | Frontend & backend developers, integrators |

## Subsystem Documentation

For module-specific development, refer to each subsystem's directory:

- [☕ Backend Developer Guide](../backend/README.md): Spring Boot 4, Spring AI 2.0, package structure, and Gradle workflows.
- [⚛️ Frontend Developer Guide](../frontend/README.md): Next.js 16 (App Router), React 19, Tailwind CSS v4, and SSE stream parsing.
- [🤖 Universal Agent Guidelines](../AGENTS.md): Coding invariants, hard boundaries, and test validation commands for AI agents and contributors.
