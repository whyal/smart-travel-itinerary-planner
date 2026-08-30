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

export async function saveItineraryToDatabase(payload: SaveItineraryPayload): Promise<void> {
  const response = await fetch(`${API_BASE_URL}/itineraries`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    const errorBody = await response.text().catch(() => "");
    throw new Error(errorBody || `Server returned status HTTP ${response.status}`);
  }
}

/**
 * Streams an itinerary from the backend SSE endpoint.
 * Calls `onChunk` with each decoded text chunk as it arrives.
 * The caller is responsible for accumulating chunks and updating React state.
 *
 * @throws {Error} on non-2xx responses, unreadable streams, or network errors.
 *                 AbortError is rethrown as-is so the caller can distinguish user cancellation.
 */
export async function streamItinerary(
  payload: StreamItineraryPayload,
  signal: AbortSignal,
  onChunk: (chunk: string) => void
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
      `Server returned HTTP ${response.status}: ${response.statusText}`
    );
  }

  const reader = response.body?.getReader();
  if (!reader) {
    throw new Error("Response body is not a readable stream.");
  }

  const decoder = new TextDecoder("utf-8");

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;

    const rawChunk = decoder.decode(value, { stream: true });

    // Process SSE payload format if formatted with "data: ..." lines
    let textChunk = "";
    if (rawChunk.includes("data:")) {
      const lines = rawChunk.split("\n");
      for (const line of lines) {
        if (line.startsWith("data:")) {
          const content = line.slice(5).trimStart();
          if (content !== "[DONE]") {
            textChunk += content + "\n";
          }
        } else if (
          line.trim() &&
          !line.startsWith("event:") &&
          !line.startsWith("id:") &&
          !line.startsWith("retry:")
        ) {
          textChunk += line + "\n";
        }
      }
    } else {
      textChunk = rawChunk;
    }

    if (textChunk) {
      onChunk(textChunk);
    }
  }

  // Flush decoder buffer
  const finalChunk = decoder.decode();
  if (finalChunk) {
    onChunk(finalChunk);
  }
}
