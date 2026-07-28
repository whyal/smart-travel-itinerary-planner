import { Activity, DayPlan, ItineraryResponse } from "../types/itinerary";

export function isValidItinerary(obj: any): boolean {
  if (!obj || typeof obj !== "object") return false;
  const dest = obj.destination || obj.Destination || obj.city || obj.location;
  const days = obj.days || obj.Days || obj.dayPlans || obj.dailyPlans || obj.itinerary;
  return Boolean(dest || (Array.isArray(days) && days.length > 0));
}

export function normalizeItinerary(obj: any, defaultDestination: string): ItineraryResponse {
  const dest =
    obj.destination ||
    obj.Destination ||
    obj.city ||
    obj.location ||
    defaultDestination ||
    "Trip Plan";

  const rawDays =
    obj.days ||
    obj.Days ||
    obj.dayPlans ||
    obj.dailyPlans ||
    obj.itinerary ||
    [];

  const days: DayPlan[] = Array.isArray(rawDays)
    ? rawDays.map((d: any, idx: number) => ({
        dayNumber: Number(d.dayNumber ?? d.day ?? d.day_number ?? idx + 1),
        theme: String(d.theme || d.title || d.heading || d.summary || `Day ${idx + 1}`),
        activities: Array.isArray(d.activities || d.plan || d.events)
          ? (d.activities || d.plan || d.events).map((a: any) => ({
              time: String(a.time || a.timeOfDay || a.hour || "Flexible"),
              location: String(a.location || a.place || a.spot || dest),
              description: String(
                a.description || a.details || a.activity || (typeof a === "string" ? a : "")
              ),
            }))
          : [],
      }))
    : [];

  return {
    destination: String(dest),
    days,
  };
}

export function parseMarkdownItinerary(text: string, defaultDestination: string): ItineraryResponse | null {
  if (!text || text.trim().length < 20) return null;

  const lines = text.split("\n");
  const days: DayPlan[] = [];
  let currentDay: DayPlan | null = null;
  let currentActivity: Activity | null = null;

  for (const rawLine of lines) {
    const line = rawLine.trim();

    const dayMatch = line.match(/^(?:#+\s*)?Day\s*(\d+)[:\s-]*(.*)$/i);
    if (dayMatch) {
      if (currentDay) {
        if (currentActivity) currentDay.activities.push(currentActivity);
        days.push(currentDay);
      }
      currentDay = {
        dayNumber: parseInt(dayMatch[1], 10),
        theme: dayMatch[2].replace(/^[:\s-]+/, "").trim() || `Day ${dayMatch[1]} Highlights`,
        activities: [],
      };
      currentActivity = null;
      continue;
    }

    if (currentDay) {
      const timeMatch = line.match(/^(?:[-*•]\s*)?\(?(\d{1,2}:\d{2}\s*(?:AM|PM)?|\d{1,2}\s*(?:AM|PM)|Morning|Afternoon|Evening)\)?[:\s-]*(.*)$/i);
      if (timeMatch) {
        if (currentActivity) currentDay.activities.push(currentActivity);
        const timeStr = timeMatch[1];
        const rest = timeMatch[2].replace(/^\*+|\*+$/g, "").trim();
        const parts = rest.split(/[-–—@at:]/);
        const loc = parts.length > 1 ? parts[0].trim() : defaultDestination;
        const desc = rest;
        currentActivity = {
          time: timeStr,
          location: loc || defaultDestination,
          description: desc || rest,
        };
      } else if (line.startsWith("-") || line.startsWith("*") || line.startsWith("•")) {
        const cleanContent = line.replace(/^[-*•]\s*/, "").trim();
        if (cleanContent) {
          if (!currentActivity) {
            currentActivity = {
              time: "Flexible",
              location: defaultDestination,
              description: cleanContent,
            };
          } else {
            currentActivity.description += " " + cleanContent;
          }
        }
      }
    }
  }

  if (currentDay) {
    if (currentActivity) currentDay.activities.push(currentActivity);
    days.push(currentDay);
  }

  if (days.length > 0) {
    return {
      destination: defaultDestination,
      days,
    };
  }

  return null;
}

export function parseItineraryFromText(
  text: string,
  defaultDestination: string = "Trip Plan"
): ItineraryResponse | null {
  if (!text || !text.trim()) return null;

  let clean = text.trim();

  clean = clean.replace(/^```(?:json)?\s*/i, "").replace(/\s*```$/i, "").trim();

  try {
    const parsed = JSON.parse(clean);
    if (isValidItinerary(parsed)) {
      return normalizeItinerary(parsed, defaultDestination);
    }
  } catch {
    // Continue
  }

  const firstBrace = clean.indexOf("{");
  const lastBrace = clean.lastIndexOf("}");
  if (firstBrace !== -1 && lastBrace > firstBrace) {
    const jsonCandidate = clean.slice(firstBrace, lastBrace + 1);
    try {
      const parsed = JSON.parse(jsonCandidate);
      if (isValidItinerary(parsed)) {
        return normalizeItinerary(parsed, defaultDestination);
      }
    } catch {
      // Continue
    }
  }

  return parseMarkdownItinerary(clean, defaultDestination);
}
