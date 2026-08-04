"use client";

import { useState } from "react";
import {
  History,
  Trash2,
  Eye,
  Calendar,
  MapPin,
  Clock,
  X,
  ChevronRight,
  Bookmark,
} from "lucide-react";

import {
  Activity,
  DayPlan,
  ItineraryResponse,
  SavedItineraryItem,
  ItineraryFormData,
} from "../types/itinerary";

export type { Activity, DayPlan, ItineraryResponse, SavedItineraryItem, ItineraryFormData };

const HISTORY_KEY = "itinerary_history_list";

export const getHistoryList = (): SavedItineraryItem[] => {
  if (typeof window === "undefined") return [];
  try {
    const raw = localStorage.getItem(HISTORY_KEY);
    return raw ? JSON.parse(raw) : [];
  } catch (e) {
    console.error("Failed to load itinerary history:", e);
    return [];
  }
};

export const saveToHistory = (item: SavedItineraryItem): SavedItineraryItem[] => {
  if (typeof window === "undefined") return [];
  try {
    const current = getHistoryList();
    const filtered = current.filter((i) => i.id !== item.id);
    const updated = [item, ...filtered];
    localStorage.setItem(HISTORY_KEY, JSON.stringify(updated));
    return updated;
  } catch (e) {
    console.error("Failed to save itinerary to history:", e);
    return getHistoryList();
  }
};

export const deleteFromHistory = (id: string): SavedItineraryItem[] => {
  if (typeof window === "undefined") return [];
  try {
    const current = getHistoryList();
    const updated = current.filter((i) => i.id !== id);
    localStorage.setItem(HISTORY_KEY, JSON.stringify(updated));
    return updated;
  } catch (e) {
    console.error("Failed to delete itinerary from history:", e);
    return getHistoryList();
  }
};

interface ItineraryHistoryProps {
  historyList: SavedItineraryItem[];
  currentId: string;
  onSelect: (item: SavedItineraryItem) => void;
  onDelete: (id: string) => void;
  onClearAll?: () => void;
}

export default function ItineraryHistory({
  historyList,
  currentId,
  onSelect,
  onDelete,
}: ItineraryHistoryProps) {
  const [isOpen, setIsOpen] = useState(false);

  const formatDate = (timestamp: number) => {
    try {
      const date = new Date(timestamp);
      return new Intl.DateTimeFormat("en-US", {
        month: "short",
        day: "numeric",
        year: "numeric",
        hour: "numeric",
        minute: "2-digit",
      }).format(date);
    } catch {
      return "Saved Itinerary";
    }
  };

  return (
    <>
      {/* History Trigger Button */}
      <button
        type="button"
        onClick={() => setIsOpen(true)}
        className="flex items-center space-x-2 bg-white hover:bg-slate-50 text-slate-700 border border-slate-200 hover:border-blue-300 rounded-xl px-3.5 py-2 text-xs sm:text-sm font-semibold shadow-2xs hover:shadow-xs transition-all"
      >
        <History className="w-4 h-4 text-blue-600" />
        <span>Past Itineraries</span>
        {historyList.length > 0 && (
          <span className="ml-1.5 px-2 py-0.5 bg-blue-100 text-blue-700 font-extrabold rounded-full text-xs">
            {historyList.length}
          </span>
        )}
      </button>

      {/* Slide-over Drawer / Modal Overlay */}
      {isOpen && (
        <div className="fixed inset-0 z-50 overflow-hidden bg-slate-900/50 backdrop-blur-xs flex justify-end transition-opacity">
          <div className="w-full max-w-md bg-white h-full shadow-2xl flex flex-col animate-in slide-in-from-right duration-300">
            {/* Drawer Header */}
            <div className="p-6 bg-slate-900 text-white flex items-center justify-between">
              <div className="flex items-center space-x-3">
                <div className="p-2 bg-blue-600 rounded-xl">
                  <Bookmark className="w-5 h-5 text-white" />
                </div>
                <div>
                  <h3 className="text-lg font-bold text-white">Saved Itineraries</h3>
                  <p className="text-xs text-slate-400">
                    {historyList.length} past trip plan{historyList.length === 1 ? "" : "s"} saved
                  </p>
                </div>
              </div>
              <button
                onClick={() => setIsOpen(false)}
                className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Drawer Body / List of Saved Cards */}
            <div className="flex-1 overflow-y-auto p-6 space-y-4">
              {historyList.length === 0 ? (
                <div className="h-64 flex flex-col items-center justify-center text-center p-6 bg-slate-50 rounded-2xl border border-dashed border-slate-200 text-slate-500">
                  <History className="w-10 h-10 text-slate-300 mb-3" />
                  <p className="font-semibold text-slate-700">No Past Itineraries Yet</p>
                  <p className="text-xs text-slate-500 mt-1 max-w-xs">
                    Generated itineraries will be automatically saved here so you can view or delete them anytime.
                  </p>
                </div>
              ) : (
                historyList.map((item) => {
                  const isCurrent = item.id === currentId;
                  return (
                    <div
                      key={item.id}
                      className={`p-5 rounded-2xl border transition-all duration-200 bg-white shadow-2xs hover:shadow-md ${
                        isCurrent
                          ? "border-blue-500 ring-2 ring-blue-500/20 bg-blue-50/20"
                          : "border-slate-200 hover:border-blue-300"
                      }`}
                    >
                      {/* Destination Title & Date */}
                      <div className="flex items-start justify-between gap-3 mb-2">
                        <div>
                          <div className="flex items-center space-x-2">
                            <span className="font-extrabold text-slate-900 text-base flex items-center gap-1.5">
                              <MapPin className="w-4 h-4 text-blue-600" />
                              {item.itinerary.destination}
                            </span>
                            {isCurrent && (
                              <span className="px-2 py-0.5 bg-blue-100 text-blue-700 font-bold text-[10px] uppercase rounded-md">
                                Active
                              </span>
                            )}
                          </div>
                          <p className="text-xs text-slate-400 mt-1 flex items-center gap-1">
                            <Clock className="w-3 h-3" />
                            {formatDate(item.createdAt)}
                          </p>
                        </div>
                        {/* Delete Button */}
                        <button
                          type="button"
                          onClick={() => onDelete(item.id)}
                          title="Delete this itinerary"
                          className="p-1.5 text-slate-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors shrink-0"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </div>

                      {/* Details Badges */}
                      <div className="flex flex-wrap gap-1.5 my-3 text-xs">
                        <span className="px-2.5 py-1 bg-slate-100 text-slate-700 font-semibold rounded-lg flex items-center space-x-1">
                          <Calendar className="w-3 h-3 text-slate-500" />
                          <span>{item.itinerary.days.length} Days</span>
                        </span>
                        <span className="px-2.5 py-1 bg-slate-100 text-slate-700 font-semibold rounded-lg">
                          {item.formData.pace} Pace
                        </span>
                        <span className="px-2.5 py-1 bg-slate-100 text-slate-700 font-semibold rounded-lg">
                          {item.formData.budget}
                        </span>
                      </div>

                      {/* View Button */}
                      <div className="pt-2 border-t border-slate-100 flex justify-end">
                        <button
                          type="button"
                          onClick={() => {
                            onSelect(item);
                            setIsOpen(false);
                          }}
                          className="flex items-center space-x-1.5 text-xs font-bold text-blue-600 hover:text-blue-700 hover:bg-blue-50 px-3 py-1.5 rounded-lg transition"
                        >
                          <Eye className="w-3.5 h-3.5" />
                          <span>View Itinerary Cards</span>
                          <ChevronRight className="w-3.5 h-3.5" />
                        </button>
                      </div>
                    </div>
                  );
                })
              )}
            </div>
          </div>
        </div>
      )}
    </>
  );
}
