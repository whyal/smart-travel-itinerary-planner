import { ItineraryFormData, ItineraryResponse } from "../types/itinerary";

export interface SaveItineraryPayload {
  conversationId: string;
  destination: string;
  daysCount: number;
  formData: ItineraryFormData;
  itinerary: ItineraryResponse;
  rawText: string;
  createdAt: string;
}

export interface StreamItineraryPayload {
  prompt: string;
  conversationId: string;
}

export const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api";

export async function saveItineraryToDatabase(
  payload: SaveItineraryPayload,
): Promise<void> {
  const response = await fetch(`${API_BASE_URL}/itineraries`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    const errorBody = await response.text().catch(() => "");
    throw new Error(
      errorBody || `Server returned status HTTP ${response.status}`,
    );
  }
}

export async function streamItinerary(
  payload: StreamItineraryPayload,
  signal: AbortSignal,
  onChunk: (chunk: string) => void,
): Promise<void> {
  const response = await fetch(`${API_BASE_URL}/itinerary/stream`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Accept: "text/event-stream, application/json, text/plain",
    },
    body: JSON.stringify(payload),
    signal,
  });

  if (!response.ok) {
    throw new Error(
      `Server returned HTTP ${response.status}: ${response.statusText}`,
    );
  }

  const reader = response.body?.getReader();
  if (!reader) {
    throw new Error("Response body is not a readable stream.");
  }

  const decoder = new TextDecoder("utf-8");
  let buffer = "";

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;

    buffer += decoder.decode(value, { stream: true });
    const lines = buffer.split("\n");
    buffer = lines.pop() ?? "";

    let textChunk = "";
    for (const rawLine of lines) {
      const line = rawLine.endsWith("\r") ? rawLine.slice(0, -1) : rawLine;
      if (line.startsWith("data:")) {
        const content = line.slice(5);
        if (content !== "[DONE]") {
          textChunk += content;
        }
      } else if (
        !line.startsWith("event:") &&
        !line.startsWith("id:") &&
        !line.startsWith("retry:") &&
        line.length > 0
      ) {
        textChunk += line;
      }
    }

    if (textChunk) {
      onChunk(textChunk);
    }
  }

  buffer += decoder.decode();
  if (buffer.length > 0) {
    const lines = buffer.split("\n");
    let textChunk = "";
    for (const rawLine of lines) {
      const line = rawLine.endsWith("\r") ? rawLine.slice(0, -1) : rawLine;
      if (line.startsWith("data:")) {
        const content = line.slice(5);
        if (content !== "[DONE]") {
          textChunk += content;
        }
      } else if (
        !line.startsWith("event:") &&
        !line.startsWith("id:") &&
        !line.startsWith("retry:") &&
        line.length > 0
      ) {
        textChunk += line;
      }
    }
    if (textChunk) {
      onChunk(textChunk);
    }
  }
}
