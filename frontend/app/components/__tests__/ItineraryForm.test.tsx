import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import ItineraryForm from "../ItineraryForm";
import * as itineraryApi from "../../services/itineraryApi";
jest.mock("../../services/itineraryApi", () => ({
  ...jest.requireActual("../../services/itineraryApi"),
  saveItineraryToDatabase: jest.fn(),
}));
function createMockStreamReader(chunks: string[]) {
  const encoder = new TextEncoder();
  let index = 0;
  return {
    read: jest.fn().mockImplementation(() => {
      if (index < chunks.length) {
        const value = encoder.encode(chunks[index]);
        index++;
        return Promise.resolve({ done: false, value });
      }
      return Promise.resolve({ done: true, value: undefined });
    }),
  };
}

describe("ItineraryForm Component", () => {
  const originalFetch = global.fetch;

  beforeEach(() => {
    jest.clearAllMocks();
    localStorage.clear();
    global.fetch = jest.fn();
  });

  afterEach(() => {
    global.fetch = originalFetch;
  });

  describe("Initial Rendering & Input Handlers", () => {
    it("renders initial form fields with default values", () => {
      render(<ItineraryForm />);

      const destinationInput = screen.getByLabelText(
        /destination/i,
      ) as HTMLInputElement;
      const daysInput = screen.getByLabelText(/days/i) as HTMLInputElement;
      const paceSelect = screen.getByLabelText(/pace/i) as HTMLSelectElement;
      const interestsInput = screen.getByLabelText(
        /interests/i,
      ) as HTMLInputElement;
      const budgetSelect = screen.getByLabelText(
        /budget/i,
      ) as HTMLSelectElement;

      expect(destinationInput.value).toBe("Osaka");
      expect(daysInput.value).toBe("3");
      expect(paceSelect.value).toBe("Moderate");
      expect(interestsInput.value).toBe("Local food, historical sights");
      expect(budgetSelect.value).toBe("Mid-range");
    });

    it("handles input change events correctly", async () => {
      const user = userEvent.setup();
      render(<ItineraryForm />);

      const destinationInput = screen.getByLabelText(/destination/i);
      const daysInput = screen.getByLabelText(/days/i);
      const paceSelect = screen.getByLabelText(/pace/i);

      await user.clear(destinationInput);
      await user.type(destinationInput, "Tokyo");
      expect(destinationInput).toHaveValue("Tokyo");

      await user.clear(daysInput);
      await user.type(daysInput, "5");
      expect(daysInput).toHaveValue(5);

      await user.selectOptions(paceSelect, "Fast-paced");
      expect(paceSelect).toHaveValue("Fast-paced");
    });
  });

  describe("Additional notes", () => {
    const submitAndGetPrompt = async (notes?: string) => {
      const user = userEvent.setup();
      (global.fetch as jest.Mock).mockResolvedValueOnce({
        ok: true,
        body: { getReader: () => createMockStreamReader([]) },
      });
      render(<ItineraryForm />);
      if (notes !== undefined) {
        await user.type(screen.getByLabelText(/anything else/i), notes);
      }
      await user.click(
        screen.getByRole("button", { name: /generate streamed itinerary/i }),
      );
      const body = (global.fetch as jest.Mock).mock.calls[0][1].body;
      return JSON.parse(body).prompt as string;
    };

    it("includes trimmed notes in the prompt when provided", async () => {
      const prompt = await submitAndGetPrompt("  vegetarian  ");
      expect(prompt).toContain("Budget: Mid-range\nAdditional notes: vegetarian");
    });

    it("omits the notes line when notes are blank", async () => {
      const prompt = await submitAndGetPrompt();
      expect(prompt).not.toContain("Additional notes");
    });
  });

  describe("Start date", () => {
    it("lists a weekday date for each day in the prompt when a start date is set", async () => {
      const user = userEvent.setup();
      (global.fetch as jest.Mock).mockResolvedValueOnce({
        ok: true,
        body: { getReader: () => createMockStreamReader([]) },
      });
      render(<ItineraryForm />);
      await user.type(screen.getByLabelText(/start date/i), "2026-12-05");
      await user.click(
        screen.getByRole("button", { name: /generate streamed itinerary/i }),
      );
      const prompt = JSON.parse(
        (global.fetch as jest.Mock).mock.calls[0][1].body,
      ).prompt as string;
      expect(prompt).toContain("Dates:\nDay 1: Sat, 5 Dec 2026");
      expect(prompt).toContain("Day 3: Mon, 7 Dec 2026");
    });

    it("omits the dates block when no start date is set", async () => {
      const user = userEvent.setup();
      (global.fetch as jest.Mock).mockResolvedValueOnce({
        ok: true,
        body: { getReader: () => createMockStreamReader([]) },
      });
      render(<ItineraryForm />);
      await user.click(
        screen.getByRole("button", { name: /generate streamed itinerary/i }),
      );
      const prompt = JSON.parse(
        (global.fetch as jest.Mock).mock.calls[0][1].body,
      ).prompt as string;
      expect(prompt).not.toContain("Dates:");
    });
  });

  describe("Form Submission (handleSubmit) & SSE Streaming", () => {
    it("submits form data, sends POST request to API, and streams formatted itinerary text", async () => {
      const user = userEvent.setup();

      const sampleJson = JSON.stringify({
        destination: "Kyoto",
        days: [
          {
            dayNumber: 1,
            theme: "Kyoto Exploration",
            activities: [
              {
                time: "09:00 AM",
                location: "Kinkaku-ji Temple",
                description: "Visit Golden Pavilion",
              },
            ],
          },
        ],
      });

      const mockReader = createMockStreamReader([`data: ${sampleJson}
`]);
      (global.fetch as jest.Mock).mockResolvedValueOnce({
        ok: true,
        body: { getReader: () => mockReader },
      });

      render(<ItineraryForm />);

      const submitButton = screen.getByRole("button", {
        name: /generate streamed itinerary/i,
      });

      await user.click(submitButton);
      expect(global.fetch).toHaveBeenCalledWith(
        "http://localhost:8080/api/itinerary/stream",
        expect.objectContaining({
          method: "POST",
          headers: expect.objectContaining({
            "Content-Type": "application/json",
          }),
          body: expect.stringContaining("Destination: Osaka"),
        }),
      );
      await waitFor(() => {
        expect(screen.getByText(/Kinkaku-ji Temple/i)).toBeInTheDocument();
      });
    });

    it("discards the draft and shows only the repaired itinerary after a retry event", async () => {
      const user = userEvent.setup();
      const itineraryAt = (location: string) =>
        JSON.stringify({
          destination: "Osaka",
          days: [
            {
              dayNumber: 1,
              theme: "Day one",
              activities: [{ time: "09:00 AM", location, description: "Visit" }],
            },
          ],
        });
      (global.fetch as jest.Mock).mockResolvedValueOnce({
        ok: true,
        body: {
          getReader: () =>
            createMockStreamReader([
              `data:${itineraryAt("Draft Venue")}\n\n`,
              `data:[RETRY]\n\ndata:${itineraryAt("Repaired Venue")}\n\n`,
            ]),
        },
      });

      render(<ItineraryForm />);
      await user.click(
        screen.getByRole("button", { name: /generate streamed itinerary/i }),
      );

      await waitFor(() => {
        expect(screen.getByText(/Repaired Venue/i)).toBeInTheDocument();
      });
      expect(screen.queryByText(/Draft Venue/i)).not.toBeInTheDocument();
    });

    it("clears destination, interests and notes after a successful generation", async () => {
      const user = userEvent.setup();
      const sampleJson = JSON.stringify({
        destination: "Osaka",
        days: [
          {
            dayNumber: 1,
            theme: "Day one",
            activities: [
              { time: "09:00 AM", location: "Osaka Castle", description: "Visit" },
            ],
          },
        ],
      });
      (global.fetch as jest.Mock).mockResolvedValueOnce({
        ok: true,
        body: { getReader: () => createMockStreamReader([`data: ${sampleJson}\n`]) },
      });

      render(<ItineraryForm />);
      await user.click(
        screen.getByRole("button", { name: /generate streamed itinerary/i }),
      );

      await waitFor(() => {
        expect(screen.getByLabelText(/destination/i)).toHaveValue("");
      });
      expect(screen.getByLabelText(/interests/i)).toHaveValue("");
    });

    it("keeps both itineraries in history when generating twice in one session", async () => {
      const user = userEvent.setup();
      const streamFor = (destination: string, location: string) => ({
        ok: true,
        body: {
          getReader: () =>
            createMockStreamReader([
              `data:${JSON.stringify({
                destination,
                days: [
                  {
                    dayNumber: 1,
                    theme: "Day one",
                    activities: [{ time: "09:00 AM", location, description: "Visit" }],
                  },
                ],
              })}\n`,
            ]),
        },
      });
      (global.fetch as jest.Mock)
        .mockResolvedValueOnce(streamFor("Osaka", "Osaka Castle"))
        .mockResolvedValueOnce(streamFor("Kyoto", "Kinkaku-ji"));

      render(<ItineraryForm />);
      const generate = screen.getByRole("button", {
        name: /generate streamed itinerary/i,
      });
      await user.click(generate);
      await waitFor(() => expect(screen.getByText(/Osaka Castle/i)).toBeInTheDocument());
      await user.type(screen.getByLabelText(/destination/i), "Kyoto");
      await user.type(screen.getByLabelText(/interests/i), "Temples");
      await user.click(generate);
      await waitFor(() => expect(screen.getByText(/Kinkaku-ji/i)).toBeInTheDocument());

      const history = JSON.parse(localStorage.getItem("itinerary_history_list") ?? "[]");
      expect(history.map((h: { itinerary: { destination: string } }) => h.itinerary.destination)).toEqual([
        "Kyoto",
        "Osaka",
      ]);
      const ids = (global.fetch as jest.Mock).mock.calls.map(
        (c) => JSON.parse(c[1].body).conversationId,
      );
      expect(ids[0]).not.toEqual(ids[1]);
    });

    it("handles server HTTP error response during form submit", async () => {
      const consoleErrorSpy = jest
        .spyOn(console, "error")
        .mockImplementation(() => {});
      const user = userEvent.setup();

      (global.fetch as jest.Mock).mockResolvedValueOnce({
        ok: false,
        status: 500,
        statusText: "Internal Server Error",
      });

      render(<ItineraryForm />);

      const submitButton = screen.getByRole("button", {
        name: /generate streamed itinerary/i,
      });

      await user.click(submitButton);

      await waitFor(() => {
        expect(
          screen.getByText(/Server returned HTTP 500: Internal Server Error/i),
        ).toBeInTheDocument();
      });

      consoleErrorSpy.mockRestore();
    });

    it("shows an error and saves nothing when the stream is not a valid itinerary", async () => {
      const user = userEvent.setup();
      const mockReader = createMockStreamReader([
        'data: {"destination":"Osaka","days":[{"dayNumber":1,"activities":[{"time":"09:00 AM","loc\n',
      ]);
      (global.fetch as jest.Mock).mockResolvedValueOnce({
        ok: true,
        body: { getReader: () => mockReader },
      });

      render(<ItineraryForm />);
      await user.click(
        screen.getByRole("button", { name: /generate streamed itinerary/i }),
      );

      await waitFor(() => {
        expect(
          screen.getByText(/incomplete or not a valid itinerary/i),
        ).toBeInTheDocument();
      });
      expect(localStorage.getItem("itinerary_history_list")).toBeNull();
    });

    it("handles network failure during submission", async () => {
      const consoleErrorSpy = jest
        .spyOn(console, "error")
        .mockImplementation(() => {});
      const user = userEvent.setup();

      (global.fetch as jest.Mock).mockRejectedValueOnce(
        new Error("Failed to fetch"),
      );

      render(<ItineraryForm />);

      const submitButton = screen.getByRole("button", {
        name: /generate streamed itinerary/i,
      });

      await user.click(submitButton);

      await waitFor(() => {
        expect(screen.getByText(/Failed to fetch/i)).toBeInTheDocument();
      });

      consoleErrorSpy.mockRestore();
    });
  });

  describe("Auto-save & Manual DB Save Handlers", () => {
    it("automatically saves itinerary to database post-stream when auto-save checkbox is checked", async () => {
      const user = userEvent.setup();

      const jsonItinerary = JSON.stringify({
        destination: "Osaka",
        days: [
          {
            dayNumber: 1,
            theme: "Food Tour",
            activities: [
              {
                time: "10:00 AM",
                location: "Dotonbori",
                description: "Eat Street Food",
              },
            ],
          },
        ],
      });

      const mockReader = createMockStreamReader([`data: ${jsonItinerary}\n`]);
      (global.fetch as jest.Mock).mockResolvedValueOnce({
        ok: true,
        body: { getReader: () => mockReader },
      });

      (itineraryApi.saveItineraryToDatabase as jest.Mock).mockResolvedValueOnce(
        undefined,
      );

      render(<ItineraryForm />);
      const autoSaveCheckbox = screen.getByRole("checkbox", {
        name: /auto-save generated itinerary/i,
      });
      await user.click(autoSaveCheckbox);
      const submitButton = screen.getByRole("button", {
        name: /generate streamed itinerary/i,
      });
      await user.click(submitButton);

      await waitFor(() => {
        expect(itineraryApi.saveItineraryToDatabase).toHaveBeenCalledWith(
          expect.objectContaining({
            destination: "Osaka",
            daysCount: 1,
          }),
        );
      });

      expect(
        await screen.findByText(
          /Itinerary successfully saved to Spring Boot backend database!/i,
        ),
      ).toBeInTheDocument();
    });

    it("displays error message if auto-save to database fails", async () => {
      const consoleErrorSpy = jest
        .spyOn(console, "error")
        .mockImplementation(() => {});
      const user = userEvent.setup();

      const jsonItinerary = JSON.stringify({
        destination: "Osaka",
        days: [
          {
            dayNumber: 1,
            theme: "Sightseeing",
            activities: [
              {
                time: "9:00 AM",
                location: "Osaka Castle",
                description: "Visit castle",
              },
            ],
          },
        ],
      });

      const mockReader = createMockStreamReader([`data: ${jsonItinerary}\n`]);
      (global.fetch as jest.Mock).mockResolvedValueOnce({
        ok: true,
        body: { getReader: () => mockReader },
      });

      (itineraryApi.saveItineraryToDatabase as jest.Mock).mockRejectedValueOnce(
        new Error("Database connection refused"),
      );

      render(<ItineraryForm />);

      const autoSaveCheckbox = screen.getByRole("checkbox", {
        name: /auto-save generated itinerary/i,
      });
      await user.click(autoSaveCheckbox);

      const submitButton = screen.getByRole("button", {
        name: /generate streamed itinerary/i,
      });
      await user.click(submitButton);

      await waitFor(() => {
        expect(
          screen.getByText(
            /Database Save Failed: Database connection refused/i,
          ),
        ).toBeInTheDocument();
      });

      consoleErrorSpy.mockRestore();
    });

    it("saves a past itinerary to database from Past Itineraries drawer when Save to DB button is clicked", async () => {
      const user = userEvent.setup();
      const pastItem = {
        id: "past-session-123",
        createdAt: Date.now() - 10000,
        formData: {
          destination: "Tokyo",
          days: 2,
          pace: "Relaxed",
          interests: "Culture",
          budget: "Luxury",
        },
        itinerary: {
          destination: "Tokyo",
          days: [
            {
              dayNumber: 1,
              theme: "Temples",
              activities: [
                {
                  time: "10:00 AM",
                  location: "Senso-ji",
                  description: "Visit temple",
                },
              ],
            },
          ],
        },
        streamedText: "Day 1: Temples",
      };
      localStorage.setItem(
        "itinerary_history_list",
        JSON.stringify([pastItem]),
      );

      (itineraryApi.saveItineraryToDatabase as jest.Mock).mockResolvedValueOnce(
        undefined,
      );

      render(<ItineraryForm />);
      const historyBtn = screen.getByRole("button", {
        name: /past itineraries/i,
      });
      await user.click(historyBtn);
      expect(screen.getByText("Tokyo")).toBeInTheDocument();
      const saveDbBtns = screen.getAllByRole("button", { name: /save to db/i });
      await user.click(saveDbBtns[0]);
      await waitFor(() => {
        expect(itineraryApi.saveItineraryToDatabase).toHaveBeenCalledWith(
          expect.objectContaining({
            conversationId: "past-session-123",
            destination: "Tokyo",
            daysCount: 1,
          }),
        );
      });
      const savedBadges = await screen.findAllByText(/saved to db/i);
      expect(savedBadges.length).toBeGreaterThan(0);
    });
  });

  describe("Session Reset & Stop Generation Handlers", () => {
    it("stops stream generation when user clicks Stop Generation", async () => {
      const user = userEvent.setup();
      const mockReader = {
        read: jest.fn().mockReturnValue(new Promise(() => {})), // Hangs indefinitely
      };

      (global.fetch as jest.Mock).mockResolvedValueOnce({
        ok: true,
        body: { getReader: () => mockReader },
      });

      render(<ItineraryForm />);

      const submitButton = screen.getByRole("button", {
        name: /generate streamed itinerary/i,
      });
      await user.click(submitButton);
      const stopButton = await screen.findByRole("button", {
        name: /stop generation/i,
      });
      expect(stopButton).toBeInTheDocument();

      await user.click(stopButton);
      await waitFor(() => {
        expect(
          screen.getByRole("button", { name: /generate streamed itinerary/i }),
        ).toBeInTheDocument();
      });
    });

    it("resets session state when clicking New Session button", async () => {
      const user = userEvent.setup();
      render(<ItineraryForm />);

      const newSessionBtn = screen.getByRole("button", {
        name: /new session/i,
      });
      await user.click(newSessionBtn);
      expect(localStorage.getItem("itinerary_conversation_id")).toBeTruthy();
    });
  });
});
