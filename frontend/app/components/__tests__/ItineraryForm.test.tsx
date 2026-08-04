import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import ItineraryForm from "../ItineraryForm";
import * as itineraryApi from "../../services/itineraryApi";

// Mock API service module
jest.mock("../../services/itineraryApi", () => ({
  saveItineraryToDatabase: jest.fn(),
}));

// Helper to construct SSE stream reader
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

      const destinationInput = screen.getByLabelText(/destination/i) as HTMLInputElement;
      const daysInput = screen.getByLabelText(/days/i) as HTMLInputElement;
      const paceSelect = screen.getByLabelText(/pace/i) as HTMLSelectElement;
      const interestsInput = screen.getByLabelText(/interests/i) as HTMLInputElement;
      const budgetSelect = screen.getByLabelText(/budget/i) as HTMLSelectElement;

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

  describe("Form Submission (handleSubmit) & SSE Streaming", () => {
    it("submits form data, sends POST request to API, and streams formatted itinerary text", async () => {
      const user = userEvent.setup();

      const sampleSseChunk1 = "data: Day 1: Kyoto Exploration\n";
      const sampleSseChunk2 = "data: (9:00 AM) Kinkaku-ji Temple - Visit Golden Pavilion\n";

      const mockReader = createMockStreamReader([sampleSseChunk1, sampleSseChunk2]);
      (global.fetch as jest.Mock).mockResolvedValueOnce({
        ok: true,
        body: { getReader: () => mockReader },
      });

      render(<ItineraryForm />);

      const submitButton = screen.getByRole("button", {
        name: /generate streamed itinerary/i,
      });

      await user.click(submitButton);

      // Verify POST call was made with formatted payload
      expect(global.fetch).toHaveBeenCalledWith(
        "http://localhost:8080/api/itinerary/stream",
        expect.objectContaining({
          method: "POST",
          headers: expect.objectContaining({
            "Content-Type": "application/json",
          }),
          body: expect.stringContaining("Destination: Osaka"),
        })
      );

      // Wait for stream to be processed
      await waitFor(() => {
        expect(screen.getByText(/Kinkaku-ji Temple/i)).toBeInTheDocument();
      });
    });

    it("handles server HTTP error response during form submit", async () => {
      const consoleErrorSpy = jest.spyOn(console, "error").mockImplementation(() => {});
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
          screen.getByText(/Server returned HTTP 500: Internal Server Error/i)
        ).toBeInTheDocument();
      });

      consoleErrorSpy.mockRestore();
    });

    it("handles network failure during submission", async () => {
      const consoleErrorSpy = jest.spyOn(console, "error").mockImplementation(() => {});
      const user = userEvent.setup();

      (global.fetch as jest.Mock).mockRejectedValueOnce(new Error("Failed to fetch"));

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

      (itineraryApi.saveItineraryToDatabase as jest.Mock).mockResolvedValueOnce(undefined);

      render(<ItineraryForm />);

      // Enable auto-save checkbox
      const autoSaveCheckbox = screen.getByRole("checkbox", {
        name: /auto-save generated itinerary/i,
      });
      await user.click(autoSaveCheckbox);

      // Submit form
      const submitButton = screen.getByRole("button", {
        name: /generate streamed itinerary/i,
      });
      await user.click(submitButton);

      await waitFor(() => {
        expect(itineraryApi.saveItineraryToDatabase).toHaveBeenCalledWith(
          expect.objectContaining({
            destination: "Osaka",
            daysCount: 1,
          })
        );
      });

      expect(
        await screen.findByText(/Itinerary successfully saved to Spring Boot backend database!/i)
      ).toBeInTheDocument();
    });

    it("displays error message if auto-save to database fails", async () => {
      const consoleErrorSpy = jest.spyOn(console, "error").mockImplementation(() => {});
      const user = userEvent.setup();

      const jsonItinerary = JSON.stringify({
        destination: "Osaka",
        days: [
          {
            dayNumber: 1,
            theme: "Sightseeing",
            activities: [
              { time: "9:00 AM", location: "Osaka Castle", description: "Visit castle" },
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
        new Error("Database connection refused")
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
          screen.getByText(/Database Save Failed: Database connection refused/i)
        ).toBeInTheDocument();
      });

      consoleErrorSpy.mockRestore();
    });
  });

  describe("Session Reset & Stop Generation Handlers", () => {
    it("stops stream generation when user clicks Stop Generation", async () => {
      const user = userEvent.setup();

      // Create a reader that never completes immediately
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

      // The button text changes to "Stop Generation" while loading
      const stopButton = await screen.findByRole("button", {
        name: /stop generation/i,
      });
      expect(stopButton).toBeInTheDocument();

      await user.click(stopButton);

      // Verify that after stopping, the form returns to non-loading state
      await waitFor(() => {
        expect(
          screen.getByRole("button", { name: /generate streamed itinerary/i })
        ).toBeInTheDocument();
      });
    });

    it("resets session state when clicking New Session button", async () => {
      const user = userEvent.setup();
      render(<ItineraryForm />);

      const newSessionBtn = screen.getByRole("button", { name: /new session/i });
      await user.click(newSessionBtn);

      // Verify localStorage is initialized with new conversation ID
      expect(localStorage.getItem("itinerary_conversation_id")).toBeTruthy();
    });
  });
});
