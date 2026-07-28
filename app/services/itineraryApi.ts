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

export async function saveItineraryToDatabase(payload: SaveItineraryPayload): Promise<void> {
  const apiBase = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api/v1";

  const response = await fetch(`${apiBase}/itineraries`, {
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
