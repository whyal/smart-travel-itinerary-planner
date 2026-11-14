import {
  parseItineraryFromText,
  sanitizeJsonString,
  safeJsonParse,
  unwrapItineraryObject,
  isValidItinerary,
  normalizeItinerary,
  stripDocumentMetadataPrefixes,
  getProperty,
} from "../itineraryParser";

describe("itineraryParser", () => {
  describe("getProperty resilience", () => {
    it("retrieves property values despite surrounding whitespace or newlines in keys", () => {
      const obj = {
        "\nactivities\n": [{ time: "09:00 AM", location: "Beijing", description: "Visit Forbidden City" }],
        " destination ": "Beijing",
      };
      expect(getProperty(obj, "activities")).toEqual([
        { time: "09:00 AM", location: "Beijing", description: "Visit Forbidden City" },
      ]);
      expect(getProperty(obj, "destination")).toBe("Beijing");
    });
  });
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

    it("strips metadata headers and excessive whitespace from single string values", () => {
      expect(stripDocumentMetadataPrefixes("Title: Osaka Castle (District: Chuo)")).toBe("Osaka Castle");
      expect(stripDocumentMetadataPrefixes("  Transit:   Take subway line   ")).toBe("Take subway line");
      expect(stripDocumentMetadataPrefixes("")).toBe("");
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

      const unwrapped = unwrapItineraryObject(wrapped) as Record<
        string,
        unknown
      >;
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
    it("normalizes day plans with whitespace in keys and date property", () => {
      const raw = {
        destination: "Beijing",
        " days ": [
          {
            date: "Day 1 - Imperial Exploration",
            " activities ": [
              {
                time: "09:00 AM",
                location: "Forbidden City",
                description: "Explore the imperial palace halls and courtyards",
              },
            ],
          },
        ],
      };

      const normalized = normalizeItinerary(raw, "Beijing");
      expect(normalized.days).toHaveLength(1);
      expect(normalized.days[0].theme).toBe("Imperial Exploration");
      expect(normalized.days[0].activities).toHaveLength(1);
      expect(normalized.days[0].activities[0].location).toBe("Forbidden City");
    });

    it("normalizes array of string activities and day strings", () => {
      const rawDays = [
        "Visit the Eiffel Tower in the morning and Louvre in afternoon",
      ];
      const normalized = normalizeItinerary(rawDays, "Paris");

      expect(normalized.destination).toBe("Paris");
      expect(normalized.days.length).toBe(1);
      expect(normalized.days[0].dayNumber).toBe(1);
      expect(normalized.days[0].activities[0].description).toBe(
        "Visit the Eiffel Tower in the morning and Louvre in afternoon",
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

    it("normalizes alternative activity key names such as attractions, places, sights", () => {
      const rawWithAttractions = {
        destination: "Osaka",
        days: [
          {
            dayNumber: 1,
            theme: "Food & Sights",
            attractions: [
              {
                time: "Morning",
                location: "Osaka Castle",
                description: "Explore fortress grounds",
              },
              {
                time: "Evening",
                location: "Dotonbori",
                description: "Street food and neon signs",
              },
            ],
          },
        ],
      };

      const normalized = normalizeItinerary(rawWithAttractions, "Osaka");
      expect(normalized.days[0].activities.length).toBe(2);
      expect(normalized.days[0].activities[0].location).toBe("Osaka Castle");
      expect(normalized.days[0].activities[1].location).toBe("Dotonbori");
    });

    it("drops activities whose location repeats on a later day", () => {
      const normalized = normalizeItinerary(
        {
          destination: "Osaka",
          days: [
            { dayNumber: 1, activities: [{ time: "09:00 AM", location: "Osaka Castle" }] },
            {
              dayNumber: 2,
              activities: [
                { time: "09:00 AM", location: " osaka castle " },
                { time: "01:00 PM", location: "Dotonbori" },
              ],
            },
          ],
        },
        "Osaka",
      );
      expect(normalized.days[1].activities.map((a) => a.location)).toEqual(["Dotonbori"]);
    });

    it("normalizes activities structured as time-slot object maps", () => {
      const rawWithSlotMap = {
        destination: "Tokyo",
        days: [
          {
            dayNumber: 1,
            theme: "Tokyo Highlights",
            activities: {
              Morning: { location: "Senso-ji", description: "Historic temple" },
              Afternoon: { location: "Akihabara", description: "Electronics district" },
            },
          },
        ],
      };

      const normalized = normalizeItinerary(rawWithSlotMap, "Tokyo");
      expect(normalized.days[0].activities.length).toBe(2);
      expect(normalized.days[0].activities[0].location).toBe("Senso-ji");
      expect(normalized.days[0].activities[1].location).toBe("Akihabara");
    });

    it("strips document metadata prefixes and normalizes inconsistent document copy", () => {
      const rawWithMetadata = {
        destination: "  Osaka  ",
        days: [
          {
            dayNumber: 1,
            theme: "Day 1: Castle & Dotonbori",
            activities: [
              {
                time: "09:00 AM",
                location: "Title: Osaka Castle",
                description:
                  "Destination: Osaka\nTitle: Osaka Castle\nDistrict: Chuo\nCategory: Historical\n\nStroll through fortress grounds.",
                gettingThere: "Transit: 5 min walk from Station",
                operatingHours: "Hours: 09:00 AM - 05:00 PM",
              },
            ],
          },
        ],
      };

      const normalized = normalizeItinerary(rawWithMetadata, "Osaka");
      const act = normalized.days[0].activities[0];
      expect(normalized.days[0].theme).toBe("Castle & Dotonbori");
      expect(act.location).toBe("Osaka Castle");
      expect(act.description).toBe("Stroll through fortress grounds.");
      expect(act.gettingThere).toBe("5 min walk from Station");
      expect(act.operatingHours).toBe("09:00 AM - 05:00 PM");
    });

    it("provides clean consistent UI defaults for missing transit and operating hours", () => {
      const rawMinimal = {
        destination: "Kyoto",
        days: [
          {
            dayNumber: 1,
            theme: "Temples",
            activities: [
              {
                location: "Kinkaku-ji",
                description: "Visit the Golden Pavilion",
              },
            ],
          },
        ],
      };

      const normalized = normalizeItinerary(rawMinimal, "Kyoto");
      const act = normalized.days[0].activities[0];
      expect(act.time).toBe("09:00 AM");
      expect(act.gettingThere).toBe("Accessible via local transit or walking");
      expect(act.operatingHours).toBe("Flexible / General visiting hours");
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
      expect(result?.days[0].activities[0].location).toBe(
        "Gyeongbokgung Palace",
      );
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

    it("parses complex JSON with unescaped newlines in string literals", () => {
      const rawJsonInput = `{"destination": "Tokyo", "days": [{"activities": [{"description": "Explore outer market, enjoy fresh seafood breakfast.", "location": "Tsukiji Outer Market", "time": "08:00 AM"},
{"description": "Savor a mid-range sushi lunch experience.", "location": "Ginza", "time": "12:30 PM"}, {"description": "Relax with traditional Japanese tea and sweets.", "location": "Ginza Traditional\nTeahouse", "time": "03:00 PM"}, {"description": "Enjoy delicious yakitori skewers and drinks.", "location": "Yurakucho Gado-shita", "time": "07:00 PM\n"}], "dayNumber": 1, "theme": "Market & Upscale Bites"}]}`;

      const parsed = parseItineraryFromText(rawJsonInput, "Tokyo");
      expect(parsed).not.toBeNull();
      expect(parsed?.destination).toBe("Tokyo");
      expect(parsed?.days[0].activities[2].location).toBe(
        "Ginza Traditional Teahouse",
      );
    });

    it("returns null for prose or truncated JSON instead of inventing a plan", () => {
      expect(parseItineraryFromText("Sorry, I cannot help with that request.", "Prague")).toBeNull();
      expect(
        parseItineraryFromText('{"destination":"Prague","days":[{"dayNumber":1,"theme":"Old Town","activities":[{"time":"09:00 AM","loc', "Prague"),
      ).toBeNull();
    });
  });
});
