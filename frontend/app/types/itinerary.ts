export interface Activity {
  time: string;
  location: string;
  description: string;
}

export interface DayPlan {
  dayNumber: number;
  theme: string;
  activities: Activity[];
}

export interface ItineraryResponse {
  destination: string;
  days: DayPlan[];
}

export interface ItineraryFormData {
  destination: string;
  days: number;
  pace: string;
  interests: string;
  budget: string;
}

export interface SavedItineraryItem {
  id: string;
  createdAt: number;
  formData: ItineraryFormData;
  itinerary: ItineraryResponse;
  streamedText: string;
}

export type SaveToDbStatus = "idle" | "saving" | "saved" | "error";
