"use client";

import { RefObject } from "react";
import {
  Loader2,
  Check,
  Database,
  FileText,
  Terminal,
  Trash2,
  AlertCircle,
} from "lucide-react";
import { ItineraryFormData, ItineraryResponse, SaveToDbStatus } from "../../types/itinerary";
import StructuredItineraryView from "./StructuredItineraryView";
import ItineraryLoadingSkeleton from "./ItineraryLoadingSkeleton";
import RawStreamView from "./RawStreamView";

interface ItineraryOutputViewProps {
  loading: boolean;
  streamedText: string;
  itinerary: ItineraryResponse | null;
  formData: ItineraryFormData;
  activeTab: "structured" | "raw";
  selectedDayFilter: number | "all";
  saveToDbStatus: SaveToDbStatus;
  saveToDbMessage: string | null;
  streamEndRef: RefObject<HTMLDivElement | null>;
  onActiveTabChange: (tab: "structured" | "raw") => void;
  onSelectDayFilter: (day: number | "all") => void;
  onSaveToDatabase: () => void;
  onDismissSaveMessage: () => void;
  onClearSavedData: () => void;
}

export default function ItineraryOutputView({
  loading,
  streamedText,
  itinerary,
  formData,
  activeTab,
  selectedDayFilter,
  saveToDbStatus,
  saveToDbMessage,
  streamEndRef,
  onActiveTabChange,
  onSelectDayFilter,
  onSaveToDatabase,
  onDismissSaveMessage,
  onClearSavedData,
}: ItineraryOutputViewProps) {
  if (!loading && !streamedText && !itinerary) {
    return null;
  }

  return (
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
          {/* Save to DB button in top bar */}
          {!loading && (itinerary || streamedText) && (
            <div>
              {saveToDbStatus === "saved" ? (
                <div className="flex items-center space-x-1.5 px-3 py-1.5 bg-emerald-50 text-emerald-700 border border-emerald-200 rounded-xl text-xs font-bold shadow-2xs">
                  <Check className="w-4 h-4 text-emerald-600" />
                  <span className="hidden sm:inline">Saved to DB</span>
                </div>
              ) : saveToDbStatus === "saving" ? (
                <button
                  disabled
                  className="flex items-center space-x-1.5 px-3 py-1.5 bg-emerald-600 text-white rounded-xl text-xs font-bold opacity-80 cursor-not-allowed shadow-2xs"
                >
                  <Loader2 className="w-4 h-4 animate-spin" />
                  <span className="hidden sm:inline">Saving...</span>
                </button>
              ) : (
                <button
                  type="button"
                  onClick={onSaveToDatabase}
                  className={`flex items-center space-x-1.5 px-3 py-1.5 rounded-xl text-xs font-bold transition-all shadow-2xs ${
                    saveToDbStatus === "error"
                      ? "bg-amber-600 hover:bg-amber-700 text-white"
                      : "bg-emerald-600 hover:bg-emerald-700 text-white shadow-emerald-600/20"
                  }`}
                >
                  <Database className="w-3.5 h-3.5" />
                  <span className="hidden sm:inline">
                    {saveToDbStatus === "error" ? "Retry Save DB" : "Save to DB"}
                  </span>
                </button>
              )}
            </div>
          )}

          {/* Tab selector if structured itinerary or streamed text exists */}
          {(itinerary || streamedText) && (
            <div className="flex bg-slate-200/70 p-1 rounded-xl text-xs font-semibold">
              <button
                onClick={() => onActiveTabChange("structured")}
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
                onClick={() => onActiveTabChange("raw")}
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
              onClick={onClearSavedData}
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
            <StructuredItineraryView
              itinerary={itinerary}
              formData={formData}
              saveToDbStatus={saveToDbStatus}
              saveToDbMessage={saveToDbMessage}
              selectedDayFilter={selectedDayFilter}
              onSelectDayFilter={onSelectDayFilter}
              onSaveToDatabase={onSaveToDatabase}
              onDismissSaveMessage={onDismissSaveMessage}
            />
          ) : loading ? (
            <ItineraryLoadingSkeleton
              formData={formData}
              streamedTextLength={streamedText.length}
            />
          ) : (
            <div className="p-8 text-center bg-slate-50 rounded-2xl border border-dashed border-slate-200">
              <AlertCircle className="w-8 h-8 text-slate-400 mx-auto mb-2" />
              <p className="text-slate-700 font-semibold">Response completed</p>
              <p className="text-xs text-slate-500 mt-1">Switch to "Raw Stream" tab to view raw output.</p>
            </div>
          )
        ) : (
          <RawStreamView
            streamedText={streamedText}
            loading={loading}
            streamEndRef={streamEndRef}
          />
        )}
      </div>
    </div>
  );
}
