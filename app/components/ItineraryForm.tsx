"use client";

import { useState, useEffect, useRef, FormEvent, ChangeEvent } from "react";
import {
  Sparkles,
  Loader2,
  Square,
  MapPin,
  Clock,
  Calendar,
  RefreshCw,
  Terminal,
  FileText,
  AlertCircle,
  Trash2,
  Compass,
  CheckCircle2,
  Layers,
} from "lucide-react";
import ItineraryHistory, {
  SavedItineraryItem,
  getHistoryList,
  saveToHistory,
  deleteFromHistory,
} from "./ItineraryHistory";

// Matches the Spring AI Java Record structure
interface Activity {
  time: string;
  location: string;
  description: string;
}

interface DayPlan {
  dayNumber: number;
  theme: string;
  activities: Activity[];
}

interface ItineraryResponse {
  destination: string;
  days: DayPlan[];
}

export default function ItineraryForm() {
  const [formData, setFormData] = useState({
    destination: "Osaka",
    days: 3,
    pace: "Moderate",
    interests: "Local food, historical sights",
    budget: "Mid-range",
  });

  const [loading, setLoading] = useState(false);
  const [streamedText, setStreamedText] = useState("");
  const [itinerary, setItinerary] = useState<ItineraryResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [conversationId, setConversationId] = useState<string>("");
  const [activeTab, setActiveTab] = useState<"structured" | "raw">("structured");
  const [selectedDayFilter, setSelectedDayFilter] = useState<number | "all">("all");
  const [historyList, setHistoryList] = useState<SavedItineraryItem[]>([]);

  const abortControllerRef = useRef<AbortController | null>(null);
  const streamEndRef = useRef<HTMLDivElement | null>(null);

  // Initialize session conversation ID & restore saved itinerary on mount
  useEffect(() => {
    if (typeof window !== "undefined") {
      let savedId = localStorage.getItem("itinerary_conversation_id");
      if (!savedId) {
        savedId = crypto.randomUUID();
        localStorage.setItem("itinerary_conversation_id", savedId);
      }
      setConversationId(savedId);

      // Load saved itinerary history list
      const history = getHistoryList();
      setHistoryList(history);

      // Restore previously generated itinerary for this session from localStorage
      const cached = localStorage.getItem(`itinerary_saved_${savedId}`);
      if (cached) {
        try {
          const parsed = JSON.parse(cached);
          if (parsed.itinerary) setItinerary(parsed.itinerary);
          if (parsed.streamedText) setStreamedText(parsed.streamedText);
          if (parsed.formData) setFormData(parsed.formData);
        } catch (e) {
          console.error("Failed to restore saved itinerary:", e);
        }
      }
    }
  }, []);

  const handleSelectHistoryItem = (item: SavedItineraryItem) => {
    setConversationId(item.id);
    setItinerary(item.itinerary);
    setFormData(item.formData);
    setStreamedText(item.streamedText);
    setSelectedDayFilter("all");
    setActiveTab("structured");
    setError(null);
  };

  const handleDeleteHistoryItem = (id: string) => {
    const updated = deleteFromHistory(id);
    setHistoryList(updated);
    if (conversationId === id) {
      setItinerary(null);
      setStreamedText("");
    }
  };

  // Helper to persist current session output to localStorage
  const saveSessionCache = (
    newItinerary: ItineraryResponse | null,
    newStreamedText: string,
    currentFormData: typeof formData,
    sessionUuid: string
  ) => {
    if (typeof window !== "undefined" && sessionUuid) {
      localStorage.setItem(
        `itinerary_saved_${sessionUuid}`,
        JSON.stringify({
          itinerary: newItinerary,
          streamedText: newStreamedText,
          formData: currentFormData,
        })
      );
    }
  };

  // Auto-scroll streaming log container as new text streams in
  useEffect(() => {
    if (loading && streamEndRef.current) {
      streamEndRef.current.scrollIntoView({ behavior: "smooth" });
    }
  }, [streamedText, loading]);

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
  };

  const handleClearSavedData = () => {
    if (typeof window !== "undefined" && conversationId) {
      localStorage.removeItem(`itinerary_saved_${conversationId}`);
    }
    setItinerary(null);
    setStreamedText("");
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
      const response = await fetch(
        "http://localhost:8080/api/itinerary/stream",
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            Accept: "text/event-stream, application/json, text/plain",
          },
          body: JSON.stringify(payload),
          signal: controller.signal,
        }
      );

      if (!response.ok) {
        throw new Error(`Server returned HTTP ${response.status}: ${response.statusText}`);
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

        // Attempt soft JSON parse while streaming
        try {
          const trimmed = accumulatedText.trim();
          if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            const parsed = JSON.parse(trimmed);
            if (parsed.destination && Array.isArray(parsed.days)) {
              setItinerary(parsed);
              finalParsedItinerary = parsed;
            }
          }
        } catch {
          // Streaming text incomplete or markdown - expected during stream
        }
      }

      // Flush decoder buffer
      const finalChunk = decoder.decode();
      if (finalChunk) {
        accumulatedText += finalChunk;
        setStreamedText(accumulatedText);
      }

      // Final attempt to parse complete JSON object
      try {
        const trimmed = accumulatedText.trim();
        if (trimmed.startsWith("{")) {
          const parsed = JSON.parse(trimmed);
          if (parsed.destination && Array.isArray(parsed.days)) {
            setItinerary(parsed);
            finalParsedItinerary = parsed;
          }
        }
      } catch {
        // Formatted plain markdown/text response
      }

      // Persist results in localStorage
      saveSessionCache(finalParsedItinerary, accumulatedText, formData, currentSessionId);
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
      }
    } catch (err: unknown) {
      if (err instanceof Error && err.name === "AbortError") {
        console.log("Stream generation stopped by user.");
      } else {
        const errorMessage =
          err instanceof Error ? err.message : "An unexpected error occurred while streaming.";
        console.error("Streaming error:", err);
        setError(errorMessage);
      }
    } finally {
      setLoading(false);
      abortControllerRef.current = null;
    }
  };

  const handleChange = (
    e: ChangeEvent<HTMLInputElement | HTMLSelectElement>
  ) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  return (
    <div className="max-w-3xl mx-auto space-y-8">
      {/* Form Card */}
      <div className="p-6 sm:p-8 bg-white rounded-2xl shadow-xl border border-slate-100">
        <div className="flex items-center justify-between mb-6">
          <div className="flex items-center space-x-3">
            <div className="p-2.5 bg-blue-50 text-blue-600 rounded-xl">
              <Sparkles className="w-6 h-6" />
            </div>
            <div>
              <h2 className="text-xl font-bold text-slate-900">Plan Your Trip</h2>
              <p className="text-xs text-slate-500 font-mono mt-0.5">
                Session ID: {conversationId.slice(0, 8)}...
              </p>
            </div>
          </div>
          <div className="flex items-center space-x-2">
            <ItineraryHistory
              historyList={historyList}
              currentId={conversationId}
              onSelect={handleSelectHistoryItem}
              onDelete={handleDeleteHistoryItem}
            />
            <button
              type="button"
              onClick={handleResetSession}
              title="Reset Session UUID & Start Fresh"
              className="flex items-center space-x-1 text-xs text-slate-600 hover:text-blue-600 border border-slate-200 hover:border-blue-300 rounded-xl px-3 py-2 transition-colors font-semibold bg-white shadow-2xs hover:shadow-xs"
            >
              <RefreshCw className="w-3.5 h-3.5" />
              <span className="hidden sm:inline">New Session</span>
            </button>
          </div>
        </div>

        <form onSubmit={handleSubmit} className="space-y-5">
          <div>
            <label className="block text-sm font-semibold text-slate-750 mb-1.5">
              Destination
            </label>
            <input
              type="text"
              name="destination"
              value={formData.destination}
              onChange={handleChange}
              placeholder="e.g. Kyoto, Tokyo, Paris"
              className="w-full px-4 py-2.5 rounded-xl border border-slate-200 focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-slate-800 transition"
              required
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-semibold text-slate-750 mb-1.5">
                Days (1-7)
              </label>
              <input
                type="number"
                name="days"
                min="1"
                max="7"
                value={formData.days}
                onChange={handleChange}
                className="w-full px-4 py-2.5 rounded-xl border border-slate-200 focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-slate-800 transition"
              />
            </div>

            <div>
              <label className="block text-sm font-semibold text-slate-750 mb-1.5">
                Pace
              </label>
              <select
                name="pace"
                value={formData.pace}
                onChange={handleChange}
                className="w-full px-4 py-2.5 rounded-xl border border-slate-200 focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-slate-800 transition bg-white"
              >
                <option value="Relaxed">Relaxed</option>
                <option value="Moderate">Moderate</option>
                <option value="Fast-paced">Fast-paced</option>
              </select>
            </div>
          </div>

          <div>
            <label className="block text-sm font-semibold text-slate-750 mb-1.5">
              Interests
            </label>
            <input
              type="text"
              name="interests"
              value={formData.interests}
              onChange={handleChange}
              placeholder="e.g. Ramen, Temples, Anime, Nightlife"
              className="w-full px-4 py-2.5 rounded-xl border border-slate-200 focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-slate-800 transition"
            />
          </div>

          <div>
            <label className="block text-sm font-semibold text-slate-750 mb-1.5">
              Budget
            </label>
            <select
              name="budget"
              value={formData.budget}
              onChange={handleChange}
              className="w-full px-4 py-2.5 rounded-xl border border-slate-200 focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-slate-800 transition bg-white"
            >
              <option value="Budget">Budget</option>
              <option value="Mid-range">Mid-range</option>
              <option value="Luxury">Luxury</option>
            </select>
          </div>

          <div className="pt-2 flex items-center gap-3">
            {!loading ? (
              <button
                type="submit"
                className="w-full flex items-center justify-center space-x-2 bg-gradient-to-r from-blue-600 to-indigo-600 text-white py-3 px-6 rounded-xl font-semibold hover:from-blue-700 hover:to-indigo-700 active:scale-[0.99] shadow-lg shadow-blue-500/20 transition-all"
              >
                <Sparkles className="w-5 h-5" />
                <span>Generate Streamed Itinerary</span>
              </button>
            ) : (
              <button
                type="button"
                onClick={stopStream}
                className="w-full flex items-center justify-center space-x-2 bg-red-50 hover:bg-red-100 text-red-600 border border-red-200 py-3 px-6 rounded-xl font-semibold active:scale-[0.99] transition-all"
              >
                <Square className="w-4 h-4 fill-current" />
                <span>Stop Generation</span>
              </button>
            )}
          </div>
        </form>
      </div>

      {/* Error Banner */}
      {error && (
        <div className="p-4 bg-red-50 border border-red-200 rounded-2xl flex items-start space-x-3 text-red-800">
          <AlertCircle className="w-5 h-5 text-red-500 shrink-0 mt-0.5" />
          <div className="text-sm">
            <p className="font-semibold">Stream Request Failed</p>
            <p className="mt-0.5 text-red-700">{error}</p>
            <p className="mt-2 text-xs text-red-600">
              Ensure your backend service is running locally on <code className="bg-red-100 px-1 rounded">http://localhost:8080</code>.
            </p>
          </div>
        </div>
      )}

      {/* Output Section (Streaming or Persisted Result) */}
      {(loading || streamedText || itinerary) && (
        <div className="bg-white rounded-2xl shadow-xl border border-slate-100 overflow-hidden">
          {/* Header & View Switcher */}
          <div className="px-6 py-4 bg-slate-50 border-b border-slate-200 flex flex-wrap items-center justify-between gap-4">
            <div className="flex items-center space-x-3">
              <div className="flex items-center space-x-2">
                {loading ? (
                  <span className="flex items-center space-x-2 text-blue-600 font-medium text-sm">
                    <Loader2 className="w-4 h-4 animate-spin" />
                    <span>Streaming Response...</span>
                  </span>
                ) : (
                  <span className="flex items-center space-x-2 text-emerald-600 font-medium text-sm">
                    <span className="w-2.5 h-2.5 rounded-full bg-emerald-500" />
                    <span>Itinerary Ready (Saved)</span>
                  </span>
                )}
              </div>
            </div>

            <div className="flex items-center space-x-2">
              {/* Tab selector if structured itinerary or streamed text exists */}
              {(itinerary || streamedText) && (
                <div className="flex bg-slate-200/70 p-1 rounded-xl text-xs font-semibold">
                  <button
                    onClick={() => setActiveTab("structured")}
                    className={`flex items-center space-x-1 px-3 py-1.5 rounded-lg transition-all ${
                      activeTab === "structured"
                        ? "bg-white text-blue-600 shadow-sm"
                        : "text-slate-600 hover:text-slate-900"
                    }`}
                  >
                    <FileText className="w-3.5 h-3.5" />
                    <span>Formatted View</span>
                  </button>
                  <button
                    onClick={() => setActiveTab("raw")}
                    className={`flex items-center space-x-1 px-3 py-1.5 rounded-lg transition-all ${
                      activeTab === "raw"
                        ? "bg-white text-blue-600 shadow-sm"
                        : "text-slate-600 hover:text-slate-900"
                    }`}
                  >
                    <Terminal className="w-3.5 h-3.5" />
                    <span>Raw Stream</span>
                  </button>
                </div>
              )}

              {/* Clear button */}
              {!loading && (
                <button
                  type="button"
                  onClick={handleClearSavedData}
                  title="Clear Saved Output"
                  className="p-1.5 text-slate-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                >
                  <Trash2 className="w-4 h-4" />
                </button>
              )}
            </div>
          </div>

          {/* Body Content */}
          <div className="p-6 sm:p-8">
            {activeTab === "structured" ? (
              itinerary ? (
                <div className="space-y-6 animate-fade-in">
                  {/* Summary & Day Filter Header Card */}
                  <div className="border-b border-slate-100 pb-5">
                    <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                      <div>
                        <div className="flex items-center space-x-2">
                          <span className="px-2.5 py-0.5 bg-blue-100 text-blue-700 font-semibold text-xs rounded-md">
                            {itinerary.days.length} Days Itinerary
                          </span>
                          <span className="text-slate-300">•</span>
                          <span className="text-xs font-medium text-slate-500">{formData.pace} Pace</span>
                        </div>
                        <h3 className="text-2xl font-extrabold text-slate-900 mt-1">
                          {itinerary.destination} Travel Plan
                        </h3>
                      </div>
                    </div>

                    {/* Day Selector Tabs */}
                    <div className="mt-5 flex items-center gap-2 overflow-x-auto pb-1">
                      <button
                        onClick={() => setSelectedDayFilter("all")}
                        className={`px-3.5 py-1.5 rounded-xl text-xs font-bold transition-all flex items-center space-x-1.5 shrink-0 ${
                          selectedDayFilter === "all"
                            ? "bg-blue-600 text-white shadow-md shadow-blue-500/20"
                            : "bg-slate-100 text-slate-600 hover:bg-slate-200"
                        }`}
                      >
                        <Layers className="w-3.5 h-3.5" />
                        <span>All Days ({itinerary.days.length})</span>
                      </button>
                      {itinerary.days.map((day) => (
                        <button
                          key={day.dayNumber}
                          onClick={() => setSelectedDayFilter(day.dayNumber)}
                          className={`px-3.5 py-1.5 rounded-xl text-xs font-bold transition-all shrink-0 ${
                            selectedDayFilter === day.dayNumber
                              ? "bg-blue-600 text-white shadow-md shadow-blue-500/20"
                              : "bg-slate-100 text-slate-600 hover:bg-slate-200"
                          }`}
                        >
                          Day {day.dayNumber}
                        </button>
                      ))}
                    </div>
                  </div>

                  {/* Individual Day Cards Container */}
                  <div className="space-y-6">
                    {itinerary.days
                      .filter((day) => selectedDayFilter === "all" || selectedDayFilter === day.dayNumber)
                      .map((day) => (
                        <div
                          key={day.dayNumber}
                          className="bg-white rounded-2xl shadow-lg border border-slate-200/70 overflow-hidden hover:shadow-xl transition-all duration-200"
                        >
                          {/* Individual Day Header */}
                          <div className="p-5 sm:p-6 bg-gradient-to-r from-slate-900 to-slate-800 text-white flex items-center justify-between">
                            <div className="flex items-center space-x-3">
                              <div className="px-3 py-1 bg-blue-500 text-white rounded-lg text-xs font-extrabold uppercase tracking-wider flex items-center space-x-1">
                                <Calendar className="w-3.5 h-3.5" />
                                <span>Day {day.dayNumber}</span>
                              </div>
                              <h4 className="text-base sm:text-lg font-bold text-white">
                                {day.theme}
                              </h4>
                            </div>
                            <span className="text-xs font-semibold text-slate-300 bg-slate-800/90 px-2.5 py-1 rounded-lg border border-slate-700 shrink-0">
                              {day.activities.length} Activities
                            </span>
                          </div>

                          {/* Individual Day Activities List */}
                          <div className="p-5 sm:p-6 space-y-3.5 bg-slate-50/40">
                            {day.activities.map((act, index) => (
                              <div
                                key={index}
                                className="flex items-start space-x-3.5 p-4 bg-white rounded-xl border border-slate-200/80 shadow-2xs hover:border-blue-300 hover:shadow-xs transition"
                              >
                                <div className="p-2 bg-blue-50 text-blue-600 rounded-xl shrink-0 mt-0.5">
                                  <Clock className="w-4 h-4" />
                                </div>
                                <div className="space-y-1 text-sm flex-1">
                                  <div className="flex flex-wrap items-center gap-2">
                                    <span className="font-bold text-blue-600 font-mono text-xs bg-blue-50 px-2 py-0.5 rounded-md">
                                      {act.time}
                                    </span>
                                    <span className="text-slate-300">•</span>
                                    <span className="font-bold text-slate-900 flex items-center space-x-1">
                                      <MapPin className="w-3.5 h-3.5 text-slate-400 shrink-0" />
                                      <span>{act.location}</span>
                                    </span>
                                  </div>
                                  <p className="text-slate-600 leading-relaxed text-sm pt-0.5">
                                    {act.description}
                                  </p>
                                </div>
                              </div>
                            ))}
                          </div>
                        </div>
                      ))}
                  </div>
                </div>
              ) : loading ? (
                /* User-friendly loading state during generation */
                <div className="space-y-6">
                  {/* Loading Card Header */}
                  <div className="p-6 bg-gradient-to-r from-blue-50 via-indigo-50 to-purple-50 rounded-2xl border border-blue-100/80 shadow-xs relative overflow-hidden">
                    <div className="flex items-start justify-between gap-4">
                      <div className="flex items-center space-x-4">
                        <div className="p-3 bg-white text-blue-600 rounded-2xl shadow-md border border-blue-100 shrink-0">
                          <Compass className="w-7 h-7 animate-spin" style={{ animationDuration: "8s" }} />
                        </div>
                        <div>
                          <h3 className="text-lg font-bold text-slate-900">
                            Crafting Your {formData.destination || "Travel"} Plan...
                          </h3>
                          <p className="text-xs text-slate-600 mt-1 flex items-center gap-2">
                            <span>{formData.days} Days</span> • <span>{formData.pace} Pace</span> • <span>{formData.budget} Budget</span>
                          </p>
                        </div>
                      </div>
                      <span className="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-semibold bg-blue-100 text-blue-700 animate-pulse">
                        Generating AI Plan
                      </span>
                    </div>

                    {/* Dynamic Progress Steps */}
                    <div className="mt-5 grid grid-cols-1 sm:grid-cols-3 gap-2.5 text-xs">
                      <div className="flex items-center space-x-2 p-2.5 bg-white/80 rounded-xl border border-blue-100 shadow-2xs">
                        <CheckCircle2 className="w-4 h-4 text-blue-600 shrink-0" />
                        <span className="font-medium text-slate-700">Analyzing preferences</span>
                      </div>
                      <div className="flex items-center space-x-2 p-2.5 bg-white/80 rounded-xl border border-blue-100 shadow-2xs">
                        <Loader2 className="w-4 h-4 text-indigo-600 animate-spin shrink-0" />
                        <span className="font-medium text-slate-700">Curating activities</span>
                      </div>
                      <div className="flex items-center space-x-2 p-2.5 bg-white/50 rounded-xl border border-dashed border-slate-200 text-slate-400">
                        <Clock className="w-4 h-4 shrink-0" />
                        <span className="font-medium">Finalizing day schedule</span>
                      </div>
                    </div>

                    {/* Stream Byte Progress */}
                    <div className="mt-4 flex items-center justify-between text-xs text-slate-500 font-mono">
                      <div className="flex items-center space-x-2">
                        <span className="w-2 h-2 rounded-full bg-blue-500 animate-ping" />
                        <span>Receiving streamed response...</span>
                      </div>
                      <span>{streamedText.length} chars</span>
                    </div>
                  </div>

                  {/* Skeleton Itinerary Preview */}
                  <div className="space-y-4">
                    {Array.from({ length: Math.min(Number(formData.days) || 3, 3) }).map((_, dayIdx) => (
                      <div
                        key={dayIdx}
                        className="bg-slate-50/70 rounded-2xl p-5 border border-slate-200/80 space-y-4 animate-pulse"
                      >
                        <div className="flex items-center space-x-3">
                          <div className="h-6 w-16 bg-blue-200/80 rounded-lg" />
                          <div className="h-5 w-48 bg-slate-200/80 rounded-md" />
                        </div>

                        <div className="space-y-3 pl-2 sm:pl-4">
                          {[1, 2].map((actIdx) => (
                            <div
                              key={actIdx}
                              className="flex items-start space-x-3 p-3 bg-white rounded-xl border border-slate-100"
                            >
                              <div className="w-7 h-7 bg-slate-100 rounded-lg shrink-0" />
                              <div className="space-y-2 flex-1">
                                <div className="flex items-center space-x-2">
                                  <div className="h-3 w-16 bg-blue-100 rounded" />
                                  <div className="h-3 w-28 bg-slate-200 rounded" />
                                </div>
                                <div className="h-3 w-4/5 bg-slate-100 rounded" />
                              </div>
                            </div>
                          ))}
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              ) : (
                <div className="p-8 text-center bg-slate-50 rounded-2xl border border-dashed border-slate-200">
                  <AlertCircle className="w-8 h-8 text-slate-400 mx-auto mb-2" />
                  <p className="text-slate-700 font-semibold">Response completed</p>
                  <p className="text-xs text-slate-500 mt-1">Switch to "Raw Stream" tab to view raw output.</p>
                </div>
              )
            ) : (
              /* Streaming Log / Raw View */
              <div className="space-y-3">
                <div className="flex items-center justify-between text-xs font-mono text-slate-500 mb-2">
                  <div className="flex items-center space-x-2">
                    <Terminal className="w-4 h-4 text-blue-500" />
                    <span>Real-time Stream Feed</span>
                  </div>
                  <span>{streamedText.length} chars</span>
                </div>
                <div className="p-4 bg-slate-900 text-slate-100 rounded-xl font-mono text-xs sm:text-sm leading-relaxed overflow-x-auto max-h-[480px] overflow-y-auto whitespace-pre-wrap shadow-inner border border-slate-800">
                  {streamedText}
                  {loading && (
                    <span className="inline-block w-2 h-4 ml-1 bg-blue-400 animate-pulse align-middle" />
                  )}
                  <div ref={streamEndRef} />
                </div>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}


