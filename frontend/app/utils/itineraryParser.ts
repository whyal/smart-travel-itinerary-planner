import { Activity, DayPlan, ItineraryResponse } from "../types/itinerary";

function cleanStringValue(val: unknown): string {
  if (val === null || val === undefined) return "";
  return String(val)
    .replace(/\\n/g, " ")
    .replace(/\r?\n/g, " ")
    .replace(/\s+/g, " ")
    .trim();
}

/**
 * Sanitizes JSON string payloads by escaping unescaped control characters (such as raw linebreaks)
 * inside double-quoted string literals so that JSON.parse can process them successfully.
 */
export function sanitizeJsonString(str: string): string {
  if (!str) return str;
  return str.replace(/"([^"\\]*(\\.[^"\\]*)*)"/g, (match) => {
    return match.replace(/\r?\n/g, "\\n");
  });
}

/**
 * Safely parses a JSON string, attempting direct JSON.parse first,
 * and falling back to a sanitized JSON string parse if raw linebreaks exist inside string values.
 */
export function safeJsonParse(jsonStr: string): unknown | null {
  if (!jsonStr) return null;
  const clean = jsonStr.trim();
  try {
    return JSON.parse(clean);
  } catch {
    try {
      const sanitized = sanitizeJsonString(clean);
      return JSON.parse(sanitized);
    } catch {
      return null;
    }
  }
}

/**
 * Unwraps outer JSON structures (e.g. { "itinerary": { ... } }, { "data": { ... } }, or root arrays)
 * to locate the object or array containing itinerary days/plans.
 */
export function unwrapItineraryObject(rawObj: unknown): unknown {
  if (!rawObj) return rawObj;

  if (Array.isArray(rawObj)) {
    return rawObj;
  }

  if (typeof rawObj !== "object" || rawObj === null) return rawObj;
  const obj = rawObj as Record<string, unknown>;

  const directDays =
    obj.days ||
    obj.Days ||
    obj.dayPlans ||
    obj.day_plans ||
    obj.dailyPlans ||
    obj.daily_plans ||
    obj.schedule ||
    obj.daily_schedule ||
    obj.plans;

  if (Array.isArray(directDays) && directDays.length > 0) {
    return rawObj;
  }

  const wrapperKeys = [
    "itinerary",
    "Itinerary",
    "data",
    "response",
    "result",
    "output",
    "plan",
    "trip",
    "payload",
  ];

  for (const key of wrapperKeys) {
    if (obj[key] && typeof obj[key] === "object") {
      const child = unwrapItineraryObject(obj[key]);
      if (isValidItinerary(child)) {
        return child;
      }
    }
  }

  return rawObj;
}

export function isValidItinerary(rawObj: unknown): boolean {
  if (!rawObj) return false;

  if (Array.isArray(rawObj)) {
    return rawObj.length > 0;
  }

  if (typeof rawObj !== "object" || rawObj === null) return false;

  const obj = unwrapItineraryObject(rawObj);

  if (Array.isArray(obj)) {
    return obj.length > 0;
  }

  if (typeof obj !== "object" || obj === null) return false;
  const target = obj as Record<string, unknown>;

  const dest =
    target.destination ||
    target.Destination ||
    target.city ||
    target.location ||
    target.title ||
    target.dest;

  const days =
    target.days ||
    target.Days ||
    target.dayPlans ||
    target.day_plans ||
    target.dailyPlans ||
    target.daily_plans ||
    target.schedule ||
    target.daily_schedule ||
    target.plans ||
    target.itinerary;

  const hasDaysArray = Array.isArray(days) && days.length > 0;

  return Boolean(dest || hasDaysArray);
}

export function normalizeItinerary(
  rawObj: unknown,
  defaultDestination: string = "Trip Plan"
): ItineraryResponse {
  if (!rawObj) {
    return { destination: defaultDestination, days: [] };
  }

  const unwrapped = unwrapItineraryObject(rawObj);

  let dest = defaultDestination || "Trip Plan";
  let rawDays: unknown[] = [];

  if (Array.isArray(unwrapped)) {
    rawDays = unwrapped;
  } else if (typeof unwrapped === "object" && unwrapped !== null) {
    const target = unwrapped as Record<string, unknown>;
    dest = cleanStringValue(
      target.destination ||
        target.Destination ||
        target.city ||
        target.location ||
        target.title ||
        target.dest ||
        defaultDestination ||
        "Trip Plan"
    );

    const extracted =
      target.days ||
      target.Days ||
      target.dayPlans ||
      target.day_plans ||
      target.dailyPlans ||
      target.daily_plans ||
      target.schedule ||
      target.daily_schedule ||
      target.plans ||
      target.itinerary;

    rawDays = Array.isArray(extracted) ? extracted : [];
  }

  const days: DayPlan[] = rawDays.map((d: unknown, idx: number) => {
    if (typeof d === "string") {
      return {
        dayNumber: idx + 1,
        theme: `Day ${idx + 1}`,
        activities: [
          {
            time: "Flexible",
            location: cleanStringValue(dest),
            description: cleanStringValue(d),
          },
        ],
      };
    }

    const item = (typeof d === "object" && d !== null ? d : {}) as Record<
      string,
      unknown
    >;

    const dayNum = Number(
      item.dayNumber ?? item.day ?? item.day_number ?? item.dayNo ?? item.day_no ?? idx + 1
    );

    const theme = cleanStringValue(
      item.theme ||
        item.title ||
        item.heading ||
        item.summary ||
        item.dayTitle ||
        item.day_title ||
        `Day ${dayNum}`
    );

    const rawActivities =
      item.activities ||
      item.activity ||
      item.plan ||
      item.plans ||
      item.events ||
      item.schedule ||
      item.items ||
      item.highlights;

    const activities: Activity[] = Array.isArray(rawActivities)
      ? rawActivities.map((a: unknown) => {
          if (typeof a === "string") {
            return {
              time: "Flexible",
              location: cleanStringValue(dest),
              description: cleanStringValue(a),
            };
          }
          const actObj = (
            typeof a === "object" && a !== null ? a : {}
          ) as Record<string, unknown>;

          return {
            time: cleanStringValue(
              actObj.time || actObj.timeOfDay || actObj.time_of_day || actObj.hour || "Flexible"
            ),
            location: cleanStringValue(
              actObj.location || actObj.place || actObj.spot || actObj.venue || dest
            ),
            description: cleanStringValue(
              actObj.description || actObj.details || actObj.activity || actObj.title || actObj.name || ""
            ),
          };
        })
      : [];

    return {
      dayNumber: isNaN(dayNum) ? idx + 1 : dayNum,
      theme,
      activities,
    };
  });

  return {
    destination: cleanStringValue(dest),
    days,
  };
}

export function parseMarkdownItinerary(
  text: string,
  defaultDestination: string
): ItineraryResponse | null {
  if (!text || text.trim().length < 10) return null;

  const lines = text.split("\n");
  const days: DayPlan[] = [];
  let currentDay: DayPlan | null = null;
  let currentActivity: Activity | null = null;

  for (const rawLine of lines) {
    const line = rawLine.trim();

    const dayMatch = line.match(
      /^(?:#+\s*)?(?:Day|\*\*Day)\s*(\d+)[:\s-]*(.*)$/i
    );
    if (dayMatch) {
      if (currentDay) {
        if (currentActivity) currentDay.activities.push(currentActivity);
        days.push(currentDay);
      }
      const dayNum = parseInt(dayMatch[1], 10);
      const cleanTheme = dayMatch[2]
        .replace(/^\*\*|\*\*$/g, "")
        .replace(/^[:\s-]+/, "")
        .trim();
      currentDay = {
        dayNumber: dayNum,
        theme: cleanStringValue(cleanTheme || `Day ${dayNum} Highlights`),
        activities: [],
      };
      currentActivity = null;
      continue;
    }

    if (currentDay) {
      const timeMatch = line.match(
        /^(?:[-*•]\s*)?\(?(\d{1,2}:\d{2}\s*(?:AM|PM)?|\d{1,2}\s*(?:AM|PM)|Morning|Afternoon|Evening)\)?[:\s-]*(.*)$/i
      );
      if (timeMatch) {
        if (currentActivity) currentDay.activities.push(currentActivity);
        const timeStr = timeMatch[1];
        const rest = timeMatch[2].replace(/^\*+|\*+$/g, "").trim();
        const parts = rest.split(/[-–—@at:]/);
        const loc = parts.length > 1 ? parts[0].trim() : defaultDestination;
        const desc = rest;
        currentActivity = {
          time: cleanStringValue(timeStr),
          location: cleanStringValue(loc || defaultDestination),
          description: cleanStringValue(desc || rest),
        };
      } else if (
        line.startsWith("-") ||
        line.startsWith("*") ||
        line.startsWith("•")
      ) {
        const cleanContent = line.replace(/^[-*•]\s*/, "").trim();
        if (cleanContent) {
          if (!currentActivity) {
            currentActivity = {
              time: "Flexible",
              location: cleanStringValue(defaultDestination),
              description: cleanStringValue(cleanContent),
            };
          } else {
            currentActivity.description += " " + cleanStringValue(cleanContent);
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
      destination: cleanStringValue(defaultDestination),
      days,
    };
  }

  return null;
}

/**
 * Fallback parser that guarantees non-empty text can always be converted into
 * a structured ItineraryResponse with at least 1 day and structured activities.
 */
export function buildFallbackItinerary(
  text: string,
  defaultDestination: string = "Trip Plan"
): ItineraryResponse {
  const lines = text
    .split("\n")
    .map((l) => l.trim())
    .filter((l) => l.length > 0 && !l.startsWith("```"));

  const activities: Activity[] = lines.slice(0, 15).map((line) => {
    const timeMatch = line.match(
      /^(Morning|Afternoon|Evening|\d{1,2}:\d{2}\s*(?:AM|PM)?)/i
    );
    const time = timeMatch ? timeMatch[1] : "Flexible";
    const description = line.replace(/^[-*•#\d.]+\s*/, "").trim();
    return {
      time: cleanStringValue(time),
      location: cleanStringValue(defaultDestination),
      description: cleanStringValue(description || line),
    };
  });

  return {
    destination: cleanStringValue(defaultDestination),
    days: [
      {
        dayNumber: 1,
        theme: "Custom Travel Itinerary",
        activities:
          activities.length > 0
            ? activities
            : [
                {
                  time: "Flexible",
                  location: cleanStringValue(defaultDestination),
                  description: cleanStringValue(text.slice(0, 300)),
                },
              ],
      },
    ],
  };
}

export function parseItineraryFromText(
  text: string,
  defaultDestination: string = "Trip Plan"
): ItineraryResponse | null {
  if (!text || !text.trim()) return null;

  let clean = text.trim();

  clean = clean
    .replace(/^```(?:json)?\s*/i, "")
    .replace(/\s*```$/i, "")
    .trim();

  // 1. Direct or Sanitized JSON parse attempt
  const parsed = safeJsonParse(clean);
  if (parsed && isValidItinerary(parsed)) {
    const normalized = normalizeItinerary(parsed, defaultDestination);
    if (normalized.days.length > 0) return normalized;
  }

  // 2. Search for candidate JSON object {...}
  const firstBrace = clean.indexOf("{");
  const lastBrace = clean.lastIndexOf("}");
  if (firstBrace !== -1 && lastBrace > firstBrace) {
    const jsonCandidate = clean.slice(firstBrace, lastBrace + 1);
    const parsedCandidate = safeJsonParse(jsonCandidate);
    if (parsedCandidate && isValidItinerary(parsedCandidate)) {
      const normalized = normalizeItinerary(parsedCandidate, defaultDestination);
      if (normalized.days.length > 0) return normalized;
    }
  }

  // 3. Search for candidate JSON array [...]
  const firstBracket = clean.indexOf("[");
  const lastBracket = clean.lastIndexOf("]");
  if (firstBracket !== -1 && lastBracket > firstBracket) {
    const arrayCandidate = clean.slice(firstBracket, lastBracket + 1);
    const parsedCandidate = safeJsonParse(arrayCandidate);
    if (parsedCandidate && isValidItinerary(parsedCandidate)) {
      const normalized = normalizeItinerary(parsedCandidate, defaultDestination);
      if (normalized.days.length > 0) return normalized;
    }
  }

  // 4. Markdown itinerary parse
  const mdResult = parseMarkdownItinerary(clean, defaultDestination);
  if (mdResult && mdResult.days.length > 0) {
    return mdResult;
  }

  // 5. Fallback Guarantee: Convert any remaining non-empty response into structured view
  if (clean.length > 10) {
    return buildFallbackItinerary(clean, defaultDestination);
  }

  return null;
}

