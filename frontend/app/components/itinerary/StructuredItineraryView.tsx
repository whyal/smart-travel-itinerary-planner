"use client";

import {
  CheckCircle2,
  AlertCircle,
  X,
  Layers,
} from "lucide-react";
import { ItineraryFormData, ItineraryResponse, SaveToDbStatus } from "../../types/itinerary";
import DayPlanCard from "./DayPlanCard";

interface StructuredItineraryViewProps {
  itinerary: ItineraryResponse;
  formData: ItineraryFormData;
  saveToDbStatus: SaveToDbStatus;
  saveToDbMessage: string | null;
  selectedDayFilter: number | "all";
  onSelectDayFilter: (day: number | "all") => void;
  onDismissSaveMessage: () => void;
}

export default function StructuredItineraryView({
  itinerary,
  formData,
  saveToDbStatus,
  saveToDbMessage,
  selectedDayFilter,
  onSelectDayFilter,
  onDismissSaveMessage,
}: StructuredItineraryViewProps) {
  const filteredDays = itinerary.days.filter(
    (day) => selectedDayFilter === "all" || selectedDayFilter === day.dayNumber
  );

  return (
    <div className="space-y-6 animate-in fade-in">
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
            <h3 className="text-2xl font-extrabold text-slate-900 mt-1 font-display">
              {itinerary.destination} Travel Plan
            </h3>
          </div>
        </div>

        {/* Database Save Status Feedback Notification Banner */}
        {saveToDbMessage && (
          <div
            className={`mt-4 p-3.5 rounded-xl text-xs font-medium flex items-center justify-between gap-3 shadow-2xs border ${
              saveToDbStatus === "saved"
                ? "bg-emerald-50 text-emerald-800 border-emerald-200"
                : "bg-amber-50 text-amber-900 border-amber-200"
            }`}
          >
            <div className="flex items-center space-x-2">
              {saveToDbStatus === "saved" ? (
                <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
              ) : (
                <AlertCircle className="w-4 h-4 text-amber-600 shrink-0" />
              )}
              <span>{saveToDbMessage}</span>
            </div>
            <button
              onClick={onDismissSaveMessage}
              className="p-1 text-slate-400 hover:text-slate-600 transition-colors"
              title="Dismiss message"
            >
              <X className="w-3.5 h-3.5" />
            </button>
          </div>
        )}

        {/* Day Selector Tabs */}
        <div className="mt-5 flex items-center gap-2 overflow-x-auto pb-1">
          <button
            onClick={() => onSelectDayFilter("all")}
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
              onClick={() => onSelectDayFilter(day.dayNumber)}
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
        {filteredDays.map((day) => (
          <DayPlanCard key={day.dayNumber} day={day} />
        ))}
      </div>
    </div>
  );
}
