import {
  parseItineraryFromText,
  sanitizeJsonString,
  safeJsonParse,
  unwrapItineraryObject,
  isValidItinerary,
  normalizeItinerary,
  parseMarkdownItinerary,
  buildFallbackItinerary,
} from "../itineraryParser";

describe("itineraryParser", () => {
  describe("sanitizeJsonString & safeJsonParse", () => {
    it("handles empty or falsy strings gracefully", () => {
      expect(sanitizeJsonString("")).toBe("");
      expect(safeJsonParse("")).toBeNull();
      expect(safeJsonParse("   ")).toBeNull();
    });

    it("parses valid standard JSON directly", () => {
      const json = '{"destination": "Rome", "days": []}';
      const result = safeJsonParse(json) as Record<string, unknown>;
      expect(result).not.toBeNull();
      expect(result.destination).toBe("Rome");
    });

    it("sanitizes and parses JSON containing unescaped raw newlines inside strings", () => {
      const dirty = `{"destination": "Tokyo",\n"notes": "First line\nSecond line"}`;
      const parsed = safeJsonParse(dirty) as Record<string, unknown>;
      expect(parsed).not.toBeNull();
      expect(parsed.destination).toBe("Tokyo");
      expect(parsed.notes).toBe("First line\nSecond line");
    });

    it("returns null for completely invalid JSON that cannot be recovered", () => {
      expect(safeJsonParse("{ broken json without closing")).toBeNull();
    });
  });

  describe("unwrapItineraryObject & isValidItinerary", () => {
    it("returns false for null, undefined, or empty objects", () => {
      expect(isValidItinerary(null)).toBe(false);
      expect(isValidItinerary(undefined)).toBe(false);
      expect(isValidItinerary({})).toBe(false);
      expect(isValidItinerary([])).toBe(false);
    });

    it("unwraps nested wrapper keys like itinerary, data, payload", () => {
      const wrapped = {
        data: {
          itinerary: {
            destination: "Kyoto",
            days: [{ dayNumber: 1, theme: "Temples", activities: [] }],
          },
        },
      };

      const unwrapped = unwrapItineraryObject(wrapped) as Record<string, unknown>;
      expect(unwrapped.destination).toBe("Kyoto");
      expect(isValidItinerary(wrapped)).toBe(true);
    });

    it("recognizes alternative property names for days array (dayPlans, schedule, plans)", () => {
      const objWithSchedule = {
        city: "Barcelona",
        schedule: [{ day_number: 1, title: "Gaudí Tour", items: [] }],
      };
      expect(isValidItinerary(objWithSchedule)).toBe(true);
    });
  });

  describe("normalizeItinerary", () => {
    it("normalizes array of string activities and day strings", () => {
      const rawDays = [
        "Visit the Eiffel Tower in the morning and Louvre in afternoon",
      ];
      const normalized = normalizeItinerary(rawDays, "Paris");

      expect(normalized.destination).toBe("Paris");
      expect(normalized.days.length).toBe(1);
      expect(normalized.days[0].dayNumber).toBe(1);
      expect(normalized.days[0].activities[0].description).toBe(
        "Visit the Eiffel Tower in the morning and Louvre in afternoon"
      );
    });

    it("normalizes optional metadata like transit and operating hours", () => {
      const raw = {
        destination: "Kyoto",
        days: [
          {
            dayNumber: 1,
            theme: "Historic Higashiyama",
            activities: [
              {
                time: "09:00 AM",
                location: "Kiyomizu-dera",
                description: "Famous wooden stage temple",
                transit: "Take bus 206 from Kyoto Station",
                operating_hours: "6:00 AM - 6:00 PM",
              },
            ],
          },
        ],
      };

      const normalized = normalizeItinerary(raw, "Kyoto");
      const act = normalized.days[0].activities[0];
      expect(act.gettingThere).toBe("Take bus 206 from Kyoto Station");
      expect(act.operatingHours).toBe("6:00 AM - 6:00 PM");
    });
  });

  describe("parseMarkdownItinerary", () => {
    it("returns null if text is too short or lacks day structure", () => {
      expect(parseMarkdownItinerary("", "Tokyo")).toBeNull();
      expect(parseMarkdownItinerary("Just a short intro", "Tokyo")).toBeNull();
    });

    it("parses structured Markdown with Day headings and timed activity lines", () => {
      const markdown = `
# 3-Day Kyoto Itinerary

## Day 1: Arashiyama Bamboo & Monkeys
- 09:00 AM: Arashiyama Bamboo Grove - Walk through the serene bamboo forest.
- 11:30 AM: Tenryu-ji Temple - UNESCO World Heritage Zen garden.
- 01:00 PM: Arashiyama Main Street - Enjoy Kyoto-style soba lunch.
- 03:00 PM: Iwatayama Monkey Park - Scenic overlook with wild macaques.

## Day 2: Historic Temples & Gion
- 09:30 AM: Kinkaku-ji - Visit the iconic Golden Pavilion.
- 01:30 PM: Nishiki Market - Explore street food stalls.
- Evening: Gion District - Evening stroll through historic geisha quarter.
`;

      const result = parseMarkdownItinerary(markdown, "Kyoto");
      expect(result).not.toBeNull();
      expect(result?.destination).toBe("Kyoto");
      expect(result?.days.length).toBe(2);

      // Day 1 Checks
      expect(result?.days[0].dayNumber).toBe(1);
      expect(result?.days[0].theme).toBe("Arashiyama Bamboo & Monkeys");
      expect(result?.days[0].activities.length).toBe(4);
      expect(result?.days[0].activities[0].time).toBe("09:00 AM");
      expect(result?.days[0].activities[0].location).toBe("Arashiyama Bamboo Grove");

      // Day 2 Checks
      expect(result?.days[1].dayNumber).toBe(2);
      expect(result?.days[1].theme).toBe("Historic Temples & Gion");
      expect(result?.days[1].activities.length).toBe(3);
      expect(result?.days[1].activities[2].time).toBe("Evening");
    });

    it("accumulates multi-line bullet points into activity descriptions", () => {
      const markdown = `
Day 1: London Highlights
- 10:00 AM: British Museum
  - Free admission, recommended 3 hours
  - See the Rosetta Stone and Egyptian mummies
`;

      const result = parseMarkdownItinerary(markdown, "London");
      expect(result).not.toBeNull();
      expect(result?.days[0].activities.length).toBe(1);
      expect(result?.days[0].activities[0].description).toContain("Rosetta Stone");
    });
  });

  describe("buildFallbackItinerary", () => {
    it("generates a structured single-day fallback plan from arbitrary prose text", () => {
      const prose = `Morning: Visit Belém Tower and taste Pastel de Nata at Pastéis de Belém.
Afternoon: Ride Tram 28 through Alfama and visit São Jorge Castle.
Evening: Dine at a traditional Fado restaurant in Bairro Alto.`;

      const fallback = buildFallbackItinerary(prose, "Lisbon");
      expect(fallback.destination).toBe("Lisbon");
      expect(fallback.days.length).toBe(1);
      expect(fallback.days[0].dayNumber).toBe(1);
      expect(fallback.days[0].activities.length).toBe(3);
      expect(fallback.days[0].activities[0].time).toBe("Morning");
    });
  });

  describe("parseItineraryFromText (End-to-End Integration)", () => {
    it("returns null for empty or whitespace text", () => {
      expect(parseItineraryFromText("")).toBeNull();
      expect(parseItineraryFromText("   \n\t  ")).toBeNull();
    });

    it("extracts JSON embedded inside markdown code fence blocks", () => {
      const text = `
Here is your requested itinerary:
\`\`\`json
{
  "destination": "Seoul",
  "days": [
    {
      "dayNumber": 1,
      "theme": "Palaces & Markets",
      "activities": [
        { "time": "10:00 AM", "location": "Gyeongbokgung Palace", "description": "Changing of the guard ceremony." }
      ]
    }
  ]
}
\`\`\`
Hope you have a fantastic trip!
`;

      const result = parseItineraryFromText(text, "Seoul");
      expect(result).not.toBeNull();
      expect(result?.destination).toBe("Seoul");
      expect(result?.days.length).toBe(1);
      expect(result?.days[0].activities[0].location).toBe("Gyeongbokgung Palace");
    });

    it("extracts JSON array embedded inside conversational text", () => {
      const text = `
Sure! Here is the plan:
[
  {
    "day": 1,
    "title": "Arrival & Beach Walk",
    "activities": [{ "time": "02:00 PM", "location": "Waikiki Beach", "description": "Check-in and sunset swim." }]
  }
]
Let me know if you need modifications!
`;

      const result = parseItineraryFromText(text, "Honolulu");
      expect(result).not.toBeNull();
      expect(result?.destination).toBe("Honolulu");
      expect(result?.days[0].dayNumber).toBe(1);
      expect(result?.days[0].theme).toBe("Arrival & Beach Walk");
    });

    it("falls back to markdown parsing when text is formatted as markdown", () => {
      const markdown = `
# 1-Day Florence Art Tour
## Day 1: Renaissance Art & Architecture
- 09:00 AM: Uffizi Gallery - Botticelli and Da Vinci masterpieces.
- 02:00 PM: Florence Cathedral - Climb the Brunelleschi Dome.
`;

      const result = parseItineraryFromText(markdown, "Florence");
      expect(result).not.toBeNull();
      expect(result?.destination).toBe("Florence");
      expect(result?.days.length).toBe(1);
      expect(result?.days[0].theme).toBe("Renaissance Art & Architecture");
    });

    it("parses complex JSON with unescaped newlines in string literals", () => {
      const rawJsonInput = `{"destination": "Tokyo", "days": [{"activities": [{"description": "Explore outer market, enjoy fresh seafood breakfast.", "location": "Tsukiji Outer Market", "time": "08:00 AM"},
{"description": "Savor a mid-range sushi lunch experience.", "location": "Ginza", "time": "12:30 PM"}, {"description": "Relax with traditional Japanese tea and sweets.", "location": "Ginza Traditional\nTeahouse", "time": "03:00 PM"}, {"description": "Enjoy delicious yakitori skewers and drinks.", "location": "Yurakucho Gado-shita", "time": "07:00 PM\n"}], "dayNumber": 1, "theme": "Market & Upscale Bites"}]}`;

      const parsed = parseItineraryFromText(rawJsonInput, "Tokyo");
      expect(parsed).not.toBeNull();
      expect(parsed?.destination).toBe("Tokyo");
      expect(parsed?.days[0].activities[2].location).toBe("Ginza Traditional Teahouse");
    });

    it("uses fallback generator for unstructured text responses", () => {
      const plainText = `
First, visit the historic center of Prague and check out the Astronomical Clock.
Then cross Charles Bridge over the Vltava River in the afternoon.
Finish with a dinner in Malá Strana and enjoy Czech beer.
`;

      const result = parseItineraryFromText(plainText, "Prague");
      expect(result).not.toBeNull();
      expect(result?.destination).toBe("Prague");
      expect(result?.days.length).toBe(1);
      expect(result?.days[0].activities.length).toBeGreaterThan(0);
    });
  });
});

