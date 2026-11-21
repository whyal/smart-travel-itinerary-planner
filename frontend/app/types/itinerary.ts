export type ActivityCategory =
  | "SIGHTSEEING"
  | "DINING"
  | "ARRIVAL"
  | "CHECK_IN"
  | "CHECK_OUT"
  | "DEPARTURE";

export interface Activity {
  time: string;
  location: string;
  description: string;
  gettingThere?: string;
  operatingHours?: string;
  travelMinutesFromPrevious?: number;
  durationMinutes?: number;
  category?: ActivityCategory;
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
  notes?: string;
  startDate?: string;
}

export interface SavedItineraryItem {
  id: string;
  createdAt: number;
  formData: ItineraryFormData;
  itinerary: ItineraryResponse;
  streamedText: string;
  savedToDb?: boolean;
}

export type SaveToDbStatus = "idle" | "saving" | "saved" | "error";
