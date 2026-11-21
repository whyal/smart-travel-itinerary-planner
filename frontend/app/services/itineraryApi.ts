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

export const apiHeaders = (extra: Record<string, string> = {}) => ({
  "X-Api-Key": process.env.NEXT_PUBLIC_API_KEY ?? "",
  ...extra,
});

export async function saveItineraryToDatabase(
  payload: SaveItineraryPayload,
): Promise<void> {
  const response = await fetch(`${API_BASE_URL}/itineraries`, {
    method: "POST",
    headers: apiHeaders({ "Content-Type": "application/json" }),
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    const errorBody = await response.text().catch(() => "");
    throw new Error(
      errorBody || `Server returned status HTTP ${response.status}`,
    );
  }
}

const RETRY_MARKER = "[RETRY]";

function extractSseText(lines: string[]): { text: string; retry: boolean } {
  let text = "";
  let retry = false;
  for (const rawLine of lines) {
    const line = rawLine.endsWith("\r") ? rawLine.slice(0, -1) : rawLine;
    if (line.startsWith("data:")) {
      const content = line.slice(5);
      if (content === RETRY_MARKER) {
        text = "";
        retry = true;
      } else if (content !== "[DONE]") {
        text += content;
      }
    } else if (
      !line.startsWith("event:") &&
      !line.startsWith("id:") &&
      !line.startsWith("retry:") &&
      line.length > 0
    ) {
      text += line;
    }
  }
  return { text, retry };
}

export async function streamItinerary(
  payload: StreamItineraryPayload,
  signal: AbortSignal,
  onChunk: (chunk: string) => void,
  onRetry: () => void = () => {},
): Promise<void> {
  const emit = (lines: string[]) => {
    const { text, retry } = extractSseText(lines);
    if (retry) onRetry();
    if (text) onChunk(text);
  };

  const response = await fetch(`${API_BASE_URL}/itinerary/stream`, {
    method: "POST",
    headers: apiHeaders({
      "Content-Type": "application/json",
      Accept: "text/event-stream, application/json, text/plain",
    }),
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
    emit(lines);
  }

  buffer += decoder.decode();
  emit(buffer.split("\n"));
}
