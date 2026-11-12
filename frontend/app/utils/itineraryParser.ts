import { Activity, DayPlan, ItineraryResponse } from "../types/itinerary";

function cleanStringValue(val: unknown): string {
  if (val === null || val === undefined) return "";
  return String(val)
    .replace(/\\n/g, " ")
    .replace(/\r?\n/g, " ")
    .replace(/\s+/g, " ")
    .trim();
}

export function stripMetadataHeader(text: unknown): string {
  if (text === null || text === undefined) return "";
  const cleaned = String(text)
    .replace(
      /^\s*(?:Title|Destination|District|Category|Location|Venue|Attraction|Suggested Duration|Best Time to Visit|Source|Getting There|Transit|Transport|Operating Hours|Hours|Time|Time of Day):\s*/i,
      "",
    )
    .replace(/\s*\(District:[^)]*\)/i, "");
  return cleanStringValue(cleaned);
}

export function cleanDocumentDescription(
  text: unknown,
  fallbackLocation?: string,
): string {
  if (text === null || text === undefined) {
    return fallbackLocation ? `Explore ${fallbackLocation}.` : "";
  }
  const stripped = String(text)
    .replace(
      /^\s*(?:Destination|Title|District|Category|Best Time to Visit|Suggested Duration|Source):.*$/gim,
      "",
    )
    .replace(
      /\b(?:Destination|Title|District|Category|Best Time to Visit|Suggested Duration|Source):\s*[^.:\n]+(?:\.|\n|$|(?=\s*\b(?:Destination|Title|District|Category|Best Time to Visit|Suggested Duration|Source):))/gi,
      "",
    )
    .replace(
      /^\s*(?:Title|Destination|District|Category|Location|Venue|Attraction|Suggested Duration|Best Time to Visit|Source|Getting There|Operating Hours|Description):\s*/i,
      "",
    )
    .replace(
      /\b(?:Title|District|Category|Best Time to Visit|Suggested Duration):\s*/gi,
      "",
    );
  const cleaned = cleanStringValue(stripped);
  if (!cleaned && fallbackLocation) {
    return `Explore ${fallbackLocation}.`;
  }
  return cleaned;
}

export function stripDocumentMetadataPrefixes(text: unknown): string {
  return stripMetadataHeader(text);
}

export function sanitizeJsonString(str: string): string {
  if (!str) return str;
  return str.replace(/"([^"\\]*(\\.[^"\\]*)*)"/g, (match) => {
    return match.replace(/\r?\n/g, "\\n");
  });
}

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
  defaultDestination: string = "Trip Plan",
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
        "Trip Plan",
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
      item.dayNumber ??
        item.day ??
        item.day_number ??
        item.dayNo ??
        item.day_no ??
        idx + 1,
    );

    const rawTheme = cleanStringValue(
      item.theme ||
        item.title ||
        item.heading ||
        item.summary ||
        item.dayTitle ||
        item.day_title ||
        `Day ${dayNum}`,
    );
    const theme = rawTheme.replace(/^(?:Day\s*\d+[:\s-]*|Theme[:\s-]*)/i, "").trim() || `Day ${dayNum} Highlights`;

    const rawActivities =
      item.activities ||
      item.activity ||
      item.attractions ||
      item.attraction ||
      item.places ||
      item.placesToVisit ||
      item.places_to_visit ||
      item.sights ||
      item.sightseeing ||
      item.stops ||
      item.locations ||
      item.plan ||
      item.plans ||
      item.events ||
      item.schedule ||
      item.items ||
      item.highlights;

    let candidateActivities: unknown[] = [];
    if (Array.isArray(rawActivities)) {
      candidateActivities = rawActivities;
    } else if (typeof rawActivities === "object" && rawActivities !== null) {
      candidateActivities = Object.entries(rawActivities).map(
        ([slot, val]) => {
          if (typeof val === "object" && val !== null) {
            return { time: slot, ...(val as Record<string, unknown>) };
          }
          return { time: slot, description: String(val) };
        },
      );
    } else {
      const slotKeys = ["morning", "afternoon", "evening", "night"];
      const foundSlots = slotKeys.filter((k) => k in item && item[k]);
      if (foundSlots.length > 0) {
        candidateActivities = foundSlots.map((k) => {
          const val = item[k];
          const timeLabel = k.charAt(0).toUpperCase() + k.slice(1);
          if (typeof val === "object" && val !== null) {
            return { time: timeLabel, ...(val as Record<string, unknown>) };
          }
          return { time: timeLabel, description: String(val) };
        });
      }
    }

    const activities: Activity[] = candidateActivities.map((a: unknown, actIdx: number) => {
      if (typeof a === "string") {
        const cleanDesc = cleanDocumentDescription(a, cleanStringValue(dest));
        return {
          time: ["09:00 AM", "01:00 PM", "04:30 PM", "07:30 PM"][actIdx] || "Flexible",
          location: cleanStringValue(dest),
          description: cleanDesc,
          gettingThere: "Accessible via local transit or walking",
          operatingHours: "Flexible / General visiting hours",
        };
      }
      const actObj = (
        typeof a === "object" && a !== null ? a : {}
      ) as Record<string, unknown>;

      let gettingThere = stripMetadataHeader(
        cleanStringValue(
          actObj.gettingThere ||
            actObj.getting_there ||
            actObj.transit ||
            actObj.transport ||
            actObj.howToGetThere ||
            actObj.how_to_get_there ||
            "",
        ),
      );
      let operatingHours = stripMetadataHeader(
        cleanStringValue(
          actObj.operatingHours ||
            actObj.operating_hours ||
            actObj.openingHours ||
            actObj.opening_hours ||
            actObj.hours ||
            "",
        ),
      );

      let description = cleanDocumentDescription(
        actObj.description ||
          actObj.details ||
          actObj.activity ||
          actObj.title ||
          actObj.name ||
          "",
      );

      if (!gettingThere && description) {
        const transitMatch = description.match(
          /(?:getting there|transit|transport):\s*([^.]+)/i,
        );
        if (transitMatch) {
          gettingThere = transitMatch[1].trim();
          description = description.replace(transitMatch[0], "").trim();
        }
      }

      if (!operatingHours && description) {
        const hoursMatch = description.match(
          /(?:operating hours|opening hours|hours):\s*([^.]+)/i,
        );
        if (hoursMatch) {
          operatingHours = hoursMatch[1].trim();
          description = description.replace(hoursMatch[0], "").trim();
        }
      }

      const rawLocation = cleanStringValue(
        actObj.location ||
          actObj.place ||
          actObj.spot ||
          actObj.venue ||
          dest,
      );
      const location = stripMetadataHeader(rawLocation) || dest;

      let time = cleanStringValue(
        actObj.time ||
          actObj.timeOfDay ||
          actObj.time_of_day ||
          actObj.hour ||
          "",
      );
      time = time.replace(/^(?:time|time of day):\s*/i, "").trim();
      if (!time || time.toLowerCase() === "flexible") {
        time = ["09:00 AM", "01:00 PM", "04:30 PM", "07:30 PM"][actIdx] || "Flexible";
      }

      return {
        time,
        location,
        description: description || `Explore ${location}.`,
        gettingThere: gettingThere || "Accessible via local transit or walking",
        operatingHours: operatingHours || "Flexible / General visiting hours",
      };
    });

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
  defaultDestination: string,
): ItineraryResponse | null {
  if (!text || text.trim().length < 10) return null;

  const lines = text.split("\n");
  const days: DayPlan[] = [];
  let currentDay: DayPlan | null = null;
  let currentActivity: Activity | null = null;

  for (const rawLine of lines) {
    const line = rawLine.trim();

    const dayMatch = line.match(
      /^(?:#+\s*)?(?:Day|\*\*Day)\s*(\d+)[:\s-]*(.*)$/i,
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
      const isIndented = /^\s+[-*•]/.test(rawLine);
      const timeMatch = line.match(
        /^(?:[-*•]\s*)?\(?(\d{1,2}:\d{2}\s*(?:AM|PM)?|\d{1,2}\s*(?:AM|PM)|Morning|Afternoon|Evening)\)?[:\s-]*(.*)$/i,
      );
      if (timeMatch) {
        if (currentActivity) currentDay.activities.push(currentActivity);
        const timeStr = timeMatch[1];
        const rest = timeMatch[2].replace(/^\*+|\*+$/g, "").trim();
        const parts = rest.split(/\s*[-–—:]\s*|\s+@\s+|\s+at\s+/i);
        const loc = parts.length > 1 ? parts[0].trim() : defaultDestination;
        const desc = rest;
        currentActivity = {
          time: cleanStringValue(timeStr),
          location: stripDocumentMetadataPrefixes(cleanStringValue(loc || defaultDestination)),
          description: stripDocumentMetadataPrefixes(cleanStringValue(desc || rest)),
          gettingThere: "Accessible via local transit or walking",
          operatingHours: "Flexible / General visiting hours",
        };
      } else if (isIndented && currentActivity) {
        const cleanContent = line.replace(/^[-*•]\s*/, "").trim();
        if (cleanContent) {
          currentActivity.description += " " + cleanStringValue(cleanContent);
        }
      } else if (
        line.startsWith("-") ||
        line.startsWith("*") ||
        line.startsWith("•") ||
        /^\d+[\.\)]\s+/.test(line)
      ) {
        const cleanContent = line.replace(/^(?:[-*•]|\d+[\.\)])\s*/, "").trim();
        if (cleanContent) {
          if (currentActivity) {
            currentDay.activities.push(currentActivity);
          }
          const boldMatch = cleanContent.match(/^\*\*([^*]+)\*\*[:\s-]*(.*)$/);
          if (boldMatch) {
            currentActivity = {
              time: "Flexible",
              location: stripDocumentMetadataPrefixes(cleanStringValue(boldMatch[1] || defaultDestination)),
              description: stripDocumentMetadataPrefixes(cleanStringValue(boldMatch[2] || boldMatch[1])),
              gettingThere: "Accessible via local transit or walking",
              operatingHours: "Flexible / General visiting hours",
            };
          } else {
            const parts = cleanContent.split(/\s*[-–—:]\s+/);
            const loc = parts.length > 1 ? parts[0].trim() : defaultDestination;
            currentActivity = {
              time: "Flexible",
              location: stripDocumentMetadataPrefixes(cleanStringValue(loc || defaultDestination)),
              description: stripDocumentMetadataPrefixes(cleanStringValue(cleanContent)),
              gettingThere: "Accessible via local transit or walking",
              operatingHours: "Flexible / General visiting hours",
            };
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

export function buildFallbackItinerary(
  text: string,
  defaultDestination: string = "Trip Plan",
): ItineraryResponse {
  const lines = text
    .split("\n")
    .map((l) => l.trim())
    .filter((l) => l.length > 0 && !l.startsWith("```"));

  const activities: Activity[] = lines.slice(0, 15).map((line) => {
    const timeMatch = line.match(
      /^(Morning|Afternoon|Evening|\d{1,2}:\d{2}\s*(?:AM|PM)?)/i,
    );
    const time = timeMatch ? timeMatch[1] : "Flexible";
    const description = line.replace(/^[-*•#\d.]+\s*/, "").trim();
    return {
      time: cleanStringValue(time),
      location: stripDocumentMetadataPrefixes(cleanStringValue(defaultDestination)),
      description: stripDocumentMetadataPrefixes(cleanStringValue(description || line)),
      gettingThere: "Accessible via local transit or walking",
      operatingHours: "Flexible / General visiting hours",
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
  defaultDestination: string = "Trip Plan",
): ItineraryResponse | null {
  if (!text || !text.trim()) return null;

  let clean = text.trim();

  clean = clean
    .replace(/^```(?:json)?\s*/i, "")
    .replace(/\s*```$/i, "")
    .trim();
  const parsed = safeJsonParse(clean);
  if (parsed && isValidItinerary(parsed)) {
    const normalized = normalizeItinerary(parsed, defaultDestination);
    if (normalized.days.length > 0) return normalized;
  }
  const firstBrace = clean.indexOf("{");
  const lastBrace = clean.lastIndexOf("}");
  if (firstBrace !== -1 && lastBrace > firstBrace) {
    const jsonCandidate = clean.slice(firstBrace, lastBrace + 1);
    const parsedCandidate = safeJsonParse(jsonCandidate);
    if (parsedCandidate && isValidItinerary(parsedCandidate)) {
      const normalized = normalizeItinerary(
        parsedCandidate,
        defaultDestination,
      );
      if (normalized.days.length > 0) return normalized;
    }
  }
  const firstBracket = clean.indexOf("[");
  const lastBracket = clean.lastIndexOf("]");
  if (firstBracket !== -1 && lastBracket > firstBracket) {
    const arrayCandidate = clean.slice(firstBracket, lastBracket + 1);
    const parsedCandidate = safeJsonParse(arrayCandidate);
    if (parsedCandidate && isValidItinerary(parsedCandidate)) {
      const normalized = normalizeItinerary(
        parsedCandidate,
        defaultDestination,
      );
      if (normalized.days.length > 0) return normalized;
    }
  }
  const mdResult = parseMarkdownItinerary(clean, defaultDestination);
  if (mdResult && mdResult.days.length > 0) {
    return mdResult;
  }
  if (clean.length > 10) {
    return buildFallbackItinerary(clean, defaultDestination);
  }

  return null;
}
