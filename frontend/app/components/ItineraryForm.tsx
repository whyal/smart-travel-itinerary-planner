"use client";

import { useState, useEffect, useRef, FormEvent, ChangeEvent } from "react";
import {
  ItineraryFormData,
  ItineraryResponse,
  SavedItineraryItem,
  SaveToDbStatus,
} from "../types/itinerary";
import { parseItineraryFromText } from "../utils/itineraryParser";
import { API_BASE_URL, saveItineraryToDatabase } from "../services/itineraryApi";
import {
  getHistoryList,
  saveToHistory,
  deleteFromHistory,
  updateHistoryItemSavedToDb,
} from "./ItineraryHistory";
import ItineraryFormFields from "./itinerary/ItineraryFormFields";
import ErrorBanner from "./itinerary/ErrorBanner";
import ItineraryOutputView from "./itinerary/ItineraryOutputView";

const defaultForm: ItineraryFormData = {
  destination: "Osaka",
  days: 3,
  pace: "Moderate",
  interests: "Local food, historical sights",
  budget: "Mid-range",
};

export default function ItineraryForm() {
  const [formData, setFormData] = useState<ItineraryFormData>(defaultForm);
  const [loading, setLoading] = useState(false);
  const [streamedText, setStreamedText] = useState("");
  const [itinerary, setItinerary] = useState<ItineraryResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [conversationId, setConversationId] = useState<string>("");
  const [activeTab, setActiveTab] = useState<"structured" | "raw">(
    "structured"
  );
  const [selectedDayFilter, setSelectedDayFilter] = useState<number | "all">(
    "all"
  );
  const [historyList, setHistoryList] = useState<SavedItineraryItem[]>([]);

  // Database Save States & Settings
  const [saveToDbStatus, setSaveToDbStatus] = useState<SaveToDbStatus>("idle");
  const [saveToDbMessage, setSaveToDbMessage] = useState<string | null>(null);
  const [autoSaveToDb, setAutoSaveToDb] = useState<boolean>(false);

  const abortControllerRef = useRef<AbortController | null>(null);
  const streamEndRef = useRef<HTMLDivElement | null>(null);

  // Restore session and past history from localStorage after mounting (hydration-safe)
  useEffect(() => {
    queueMicrotask(() => {
      let savedId = localStorage.getItem("itinerary_conversation_id");
      if (!savedId) {
        savedId = crypto.randomUUID();
        localStorage.setItem("itinerary_conversation_id", savedId);
      }

      const history = getHistoryList();
      setHistoryList(history);

      const cached = localStorage.getItem(`itinerary_saved_${savedId}`);
      if (cached) {
        try {
          const parsed = JSON.parse(cached);
          const savedText = parsed.streamedText || "";
          const savedForm = parsed.formData || defaultForm;
          let savedItinerary = parsed.itinerary || null;

          if (!savedItinerary && savedText) {
            savedItinerary = parseItineraryFromText(
              savedText,
              savedForm.destination
            );
          }

          setConversationId(savedId);
          setFormData(savedForm);
          setStreamedText(savedText);
          setItinerary(savedItinerary);
          return;
        } catch (e) {
          console.error("Failed to restore saved session state:", e);
        }
      }

      if (history.length > 0) {
        const latest = history[0];
        setConversationId(latest.id);
        setFormData(latest.formData);
        setStreamedText(latest.streamedText);
        setItinerary(latest.itinerary);
      } else {
        setConversationId(savedId);
      }
    });
  }, []);

  // Auto-scroll streaming log container as new text streams in
  useEffect(() => {
    if (loading && streamEndRef.current) {
      streamEndRef.current.scrollIntoView({ behavior: "smooth" });
    }
  }, [streamedText, loading]);

  const handleSelectHistoryItem = (item: SavedItineraryItem) => {
    setConversationId(item.id);
    setItinerary(item.itinerary);
    setFormData(item.formData);
    setStreamedText(item.streamedText);
    setSelectedDayFilter("all");
    setActiveTab("structured");
    setError(null);
    setSaveToDbStatus("idle");
    setSaveToDbMessage(null);
  };

  const handleDeleteHistoryItem = (id: string) => {
    const updated = deleteFromHistory(id);
    setHistoryList(updated);
    if (conversationId === id) {
      setItinerary(null);
      setStreamedText("");
      setSaveToDbStatus("idle");
      setSaveToDbMessage(null);
    }
  };

  const handleSaveToDatabase = async (
    targetItinerary?: ItineraryResponse | null,
    targetText?: string,
    targetId?: string,
    targetFormData?: ItineraryFormData,
  ): Promise<boolean> => {
    const currentItinerary = targetItinerary || itinerary;
    if (!currentItinerary) return false;

    const rawStream = targetText !== undefined ? targetText : streamedText;
    const sessionUuid = targetId || conversationId || crypto.randomUUID();
    const currentForm = targetFormData || formData;

    setSaveToDbStatus("saving");
    setSaveToDbMessage(null);

    const payload = {
      conversationId: sessionUuid,
      destination: currentItinerary.destination,
      daysCount: currentItinerary.days.length,
      formData: currentForm,
      itinerary: currentItinerary,
      rawText: rawStream,
      createdAt: new Date().toISOString(),
    };

    try {
      await saveItineraryToDatabase(payload);
      setSaveToDbStatus("saved");
      setSaveToDbMessage(
        "Itinerary successfully saved to Spring Boot backend database!",
      );
      const updated = updateHistoryItemSavedToDb(sessionUuid, true);
      setHistoryList(updated);
      return true;
    } catch (err: unknown) {
      const errorMessage =
        err instanceof Error
          ? err.message
          : "Failed to connect to Spring Boot backend.";
      console.error("Save to DB error:", err);
      setSaveToDbStatus("error");
      setSaveToDbMessage(`Database Save Failed: ${errorMessage}`);
      return false;
    }
  };

  const handleSaveHistoryItemToDatabase = async (item: SavedItineraryItem) => {
    const success = await handleSaveToDatabase(
      item.itinerary,
      item.streamedText,
      item.id,
      item.formData,
    );
    if (!success) {
      throw new Error("Failed to save itinerary to database.");
    }
  };

  // Helper to persist current session output to localStorage
  const saveSessionCache = (
    newItinerary: ItineraryResponse | null,
    newStreamedText: string,
    currentFormData: ItineraryFormData,
    sessionUuid: string,
  ) => {
    if (typeof window !== "undefined" && sessionUuid) {
      localStorage.setItem(
        `itinerary_saved_${sessionUuid}`,
        JSON.stringify({
          itinerary: newItinerary,
          streamedText: newStreamedText,
          formData: currentFormData,
        }),
      );
    }
  };

  const handleResetSession = () => {
    if (typeof window !== "undefined" && conversationId) {
      localStorage.removeItem(`itinerary_saved_${conversationId}`);
    }
    const newId = crypto.randomUUID();
    if (typeof window !== "undefined") {
      localStorage.setItem("itinerary_conversation_id", newId);
    }
    setConversationId(newId);
    setItinerary(null);
    setStreamedText("");
    setError(null);
    setSaveToDbStatus("idle");
    setSaveToDbMessage(null);
  };

  const handleClearSavedData = () => {
    if (typeof window !== "undefined" && conversationId) {
      localStorage.removeItem(`itinerary_saved_${conversationId}`);
    }
    setItinerary(null);
    setStreamedText("");
    setSaveToDbStatus("idle");
    setSaveToDbMessage(null);
  };

  const formatToKeyValuePrompt = (): string => {
    return `Destination: ${formData.destination}
Days: ${formData.days}
Pace: ${formData.pace}
Interests: ${formData.interests}
Budget: ${formData.budget}

Generate a ${formData.days}-day itinerary matching these constraints.`;
  };

  const stopStream = () => {
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
      abortControllerRef.current = null;
      setLoading(false);
    }
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setStreamedText("");
    setItinerary(null);
    setError(null);
    setSaveToDbStatus("idle");
    setSaveToDbMessage(null);
    setSelectedDayFilter("all");

    const controller = new AbortController();
    abortControllerRef.current = controller;

    const currentSessionId = conversationId || crypto.randomUUID();
    const payload = {
      prompt: formatToKeyValuePrompt(),
      conversationId: currentSessionId,
    };

    let accumulatedText = "";
    let finalParsedItinerary: ItineraryResponse | null = null;

    try {
      const response = await fetch(`${API_BASE_URL}/itinerary/stream`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Accept: "text/event-stream, application/json, text/plain",
        },
        body: JSON.stringify(payload),
        signal: controller.signal,
      });

      if (!response.ok) {
        throw new Error(
          `Server returned HTTP ${response.status}: ${response.statusText}`,
        );
      }

      const reader = response.body?.getReader();
      if (!reader) {
        throw new Error("Response body is not a readable stream.");
      }

      const decoder = new TextDecoder("utf-8");

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        const rawChunk = decoder.decode(value, { stream: true });

        // Process SSE payload format if formatted with "data: ..." lines
        let textChunk = "";
        if (rawChunk.includes("data:")) {
          const lines = rawChunk.split("\n");
          for (const line of lines) {
            if (line.startsWith("data:")) {
              const content = line.slice(5).trimStart();
              if (content !== "[DONE]") {
                textChunk += content + "\n";
              }
            } else if (
              line.trim() &&
              !line.startsWith("event:") &&
              !line.startsWith("id:") &&
              !line.startsWith("retry:")
            ) {
              textChunk += line + "\n";
            }
          }
        } else {
          textChunk = rawChunk;
        }

        accumulatedText += textChunk;
        setStreamedText(accumulatedText);

        // Attempt soft itinerary parse while streaming
        const streamedParsed = parseItineraryFromText(
          accumulatedText,
          formData.destination,
        );
        if (streamedParsed) {
          setItinerary(streamedParsed);
          finalParsedItinerary = streamedParsed;
        }
      }

      // Flush decoder buffer
      const finalChunk = decoder.decode();
      if (finalChunk) {
        accumulatedText += finalChunk;
        setStreamedText(accumulatedText);
      }

      // Final attempt to parse complete itinerary
      const finalParsed = parseItineraryFromText(
        accumulatedText,
        formData.destination,
      );
      if (finalParsed) {
        setItinerary(finalParsed);
        finalParsedItinerary = finalParsed;
      }

      // Persist results in localStorage
      saveSessionCache(
        finalParsedItinerary,
        accumulatedText,
        formData,
        currentSessionId,
      );
      if (finalParsedItinerary) {
        const newItem: SavedItineraryItem = {
          id: currentSessionId,
          createdAt: Date.now(),
          formData: { ...formData },
          itinerary: finalParsedItinerary,
          streamedText: accumulatedText,
        };
        const updatedHistory = saveToHistory(newItem);
        setHistoryList(updatedHistory);

        // Optionally auto-save to backend database if enabled
        if (autoSaveToDb) {
          handleSaveToDatabase(
            finalParsedItinerary,
            accumulatedText,
            currentSessionId,
            formData,
          );
        }
      }
    } catch (err: unknown) {
      if (err instanceof Error && err.name === "AbortError") {
        console.log("Stream generation stopped by user.");
      } else {
        const errorMessage =
          err instanceof Error
            ? err.message
            : "An unexpected error occurred while streaming.";
        console.error("Streaming error:", err);
        setError(errorMessage);
      }
    } finally {
      setLoading(false);
      abortControllerRef.current = null;
    }
  };

  const handleChange = (
    e: ChangeEvent<HTMLInputElement | HTMLSelectElement>,
  ) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  return (
    <div className="max-w-3xl mx-auto space-y-8">
      {/* Form Fields Card */}
      <ItineraryFormFields
        formData={formData}
        loading={loading}
        conversationId={conversationId}
        historyList={historyList}
        autoSaveToDb={autoSaveToDb}
        onChange={handleChange}
        onSubmit={handleSubmit}
        onStopStream={stopStream}
        onResetSession={handleResetSession}
        onAutoSaveChange={setAutoSaveToDb}
        onSelectHistoryItem={handleSelectHistoryItem}
        onDeleteHistoryItem={handleDeleteHistoryItem}
        onSaveHistoryItemToDatabase={handleSaveHistoryItemToDatabase}
      />

      {/* Error Banner */}
      <ErrorBanner error={error} />

      {/* Output View (Formatted / Raw Stream / Loading) */}
      <ItineraryOutputView
        loading={loading}
        streamedText={streamedText}
        itinerary={itinerary}
        formData={formData}
        activeTab={activeTab}
        selectedDayFilter={selectedDayFilter}
        saveToDbStatus={saveToDbStatus}
        saveToDbMessage={saveToDbMessage}
        streamEndRef={streamEndRef}
        onActiveTabChange={setActiveTab}
        onSelectDayFilter={setSelectedDayFilter}
        onSaveToDatabase={() => handleSaveToDatabase()}
        onDismissSaveMessage={() => setSaveToDbMessage(null)}
        onClearSavedData={handleClearSavedData}
      />
    </div>
  );
}
