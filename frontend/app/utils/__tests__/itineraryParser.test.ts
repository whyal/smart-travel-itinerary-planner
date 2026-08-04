import { parseItineraryFromText, sanitizeJsonString } from "../itineraryParser";

describe("Itinerary Parser with Embedded Newlines & Complex JSON", () => {
  it("parses JSON structure containing unescaped raw newlines in string literals", () => {
    const rawJsonInput = `{"destination": "Tokyo", "days": [{"activities": [{"description": "Explore outer market, enjoy fresh seafood breakfast.", "location": "Tsukiji Outer Market", "time": "08:00 AM"},
{"description": "Savor a mid-range sushi lunch experience.", "location": "Ginza", "time": "12:30 PM"}, {"description": "Relax with traditional Japanese tea and sweets.", "location": "Ginza Traditional\nTeahouse", "time": "03:00 PM"}, {"description": "Enjoy delicious yakitori skewers and drinks.", "location": "Yurakucho Gado-shita", "time": "07:00 PM\n"}], "dayNumber": 1, "theme": "Market & Upscale Bites"}, {"activities": [{"description": "Visit historic temple, try street food snacks.", "location": "Senso-ji Temple & Nakamise-dori\n", "time": "09:30 AM"}, {"description": "Lunch at a renowned tempura restaurant.", "location": "Asakusa", "time": "01:00 PM"}, {"description": "St\nroll through the park, discover local cafes.", "location": "Ueno Park", "time": "03:30 PM"}, {"description": "Experience an authentic izakaya dinner.", "location": "Ameya-Yokocho (\nUeno)", "time": "07:30 PM"}], "dayNumber": 2, "theme": "Traditional Tokyo & Local Flavors"}, {"activities": [{"description": "Witness the famous crossing, explore food halls.", "location": "Shibuya Crossing & Shibuya Scramble Square", "time": "10:00 AM"}, {"description": "Enjoy a classic ramen or tonkatsu lunch.", "location": "Shibuya", "time": "0\n1:00 PM"}, {"description": "Relax in a beautiful Japanese garden.", "location": "Shinjuku Gyoen National Garden", "time": "03:30 PM"}, {"description": "Dine at\na retro izakaya alley, local street food.", "location": "Omoide Yokocho (Shinjuku)", "time": "07:00 PM"}], "dayNumber": 3, "theme": "Modern\nCity & Retro Food Alleys"}]}`;

    const parsed = parseItineraryFromText(rawJsonInput, "Tokyo");
    expect(parsed).not.toBeNull();
    expect(parsed?.destination).toBe("Tokyo");
    expect(parsed?.days.length).toBe(3);

    // Day 1 Checks
    expect(parsed?.days[0].dayNumber).toBe(1);
    expect(parsed?.days[0].theme).toBe("Market & Upscale Bites");
    expect(parsed?.days[0].activities.length).toBe(4);
    expect(parsed?.days[0].activities[2].location).toBe("Ginza Traditional Teahouse");

    // Day 2 Checks
    expect(parsed?.days[1].dayNumber).toBe(2);
    expect(parsed?.days[1].theme).toBe("Traditional Tokyo & Local Flavors");
    expect(parsed?.days[1].activities[0].location).toBe("Senso-ji Temple & Nakamise-dori");

    // Day 3 Checks
    expect(parsed?.days[2].dayNumber).toBe(3);
    expect(parsed?.days[2].theme).toBe("Modern City & Retro Food Alleys");
    expect(parsed?.days[2].activities[3].description).toBe("Dine at a retro izakaya alley, local street food.");
  });

  it("sanitizes unescaped newlines inside string literals properly", () => {
    const dirty = `"Line 1\nLine 2"`;
    const sanitized = sanitizeJsonString(dirty);
    expect(sanitized).toBe(`"Line 1\\nLine 2"`);
  });
});
