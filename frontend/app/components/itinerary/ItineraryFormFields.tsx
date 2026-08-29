"use client";

import { ChangeEvent, FormEvent } from "react";
import { Sparkles, RefreshCw, Square, Database } from "lucide-react";
import ItineraryHistory from "../ItineraryHistory";
import { ItineraryFormData, SavedItineraryItem } from "../../types/itinerary";

interface ItineraryFormFieldsProps {
  formData: ItineraryFormData;
  loading: boolean;
  conversationId: string;
  historyList: SavedItineraryItem[];
  autoSaveToDb: boolean;
  onChange: (e: ChangeEvent<HTMLInputElement | HTMLSelectElement>) => void;
  onSubmit: (e: FormEvent) => void;
  onStopStream: () => void;
  onResetSession: () => void;
  onAutoSaveChange: (checked: boolean) => void;
  onSelectHistoryItem: (item: SavedItineraryItem) => void;
  onDeleteHistoryItem: (id: string) => void;
  onSaveHistoryItemToDatabase?: (item: SavedItineraryItem) => Promise<void>;
}

export default function ItineraryFormFields({
  formData,
  loading,
  conversationId,
  historyList,
  autoSaveToDb,
  onChange,
  onSubmit,
  onStopStream,
  onResetSession,
  onAutoSaveChange,
  onSelectHistoryItem,
  onDeleteHistoryItem,
  onSaveHistoryItemToDatabase,
}: ItineraryFormFieldsProps) {
  return (
    <div className="p-6 sm:p-8 bg-white rounded-2xl shadow-xl border border-slate-100">
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center space-x-3">
          <div className="p-2.5 bg-blue-50 text-blue-600 rounded-xl">
            <Sparkles className="w-6 h-6" />
          </div>
          <div>
            <h2 className="text-xl font-bold text-slate-900">Plan Your Trip</h2>
            <p className="text-xs text-slate-500 font-mono mt-0.5">
              Session ID: {conversationId ? `${conversationId.slice(0, 8)}...` : "..."}
            </p>
          </div>
        </div>
        <div className="flex items-center space-x-2">
          <ItineraryHistory
            historyList={historyList}
            currentId={conversationId}
            onSelect={onSelectHistoryItem}
            onDelete={onDeleteHistoryItem}
            onSaveToDatabase={onSaveHistoryItemToDatabase}
          />
          <button
            type="button"
            onClick={onResetSession}
            title="Reset Session UUID & Start Fresh"
            className="flex items-center space-x-1 text-xs text-slate-600 hover:text-blue-600 border border-slate-200 hover:border-blue-300 rounded-xl px-3 py-2 transition-colors font-semibold bg-white shadow-2xs hover:shadow-xs"
          >
            <RefreshCw className="w-3.5 h-3.5" />
            <span className="hidden sm:inline">New Session</span>
          </button>
        </div>
      </div>

      <form onSubmit={onSubmit} className="space-y-5">
        <div>
          <label htmlFor="destination" className="block text-sm font-semibold text-slate-750 mb-1.5">
            Destination
          </label>
          <input
            id="destination"
            type="text"
            name="destination"
            value={formData.destination}
            onChange={onChange}
            placeholder="e.g. Kyoto, Tokyo, Paris"
            className="w-full px-4 py-2.5 rounded-xl border border-slate-200 focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-slate-800 transition"
            required
          />
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <label htmlFor="days" className="block text-sm font-semibold text-slate-750 mb-1.5">
              Days (1-7)
            </label>
            <input
              id="days"
              type="number"
              name="days"
              min="1"
              max="7"
              value={formData.days}
              onChange={onChange}
              className="w-full px-4 py-2.5 rounded-xl border border-slate-200 focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-slate-800 transition"
            />
          </div>

          <div>
            <label htmlFor="pace" className="block text-sm font-semibold text-slate-750 mb-1.5">
              Pace
            </label>
            <select
              id="pace"
              name="pace"
              value={formData.pace}
              onChange={onChange}
              className="w-full px-4 py-2.5 rounded-xl border border-slate-200 focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-slate-800 transition bg-white"
            >
              <option value="Relaxed">Relaxed</option>
              <option value="Moderate">Moderate</option>
              <option value="Fast-paced">Fast-paced</option>
            </select>
          </div>
        </div>

        <div>
          <label htmlFor="interests" className="block text-sm font-semibold text-slate-750 mb-1.5">
            Interests
          </label>
          <input
            id="interests"
            type="text"
            name="interests"
            value={formData.interests}
            onChange={onChange}
            placeholder="e.g. Ramen, Temples, Anime, Nightlife"
            className="w-full px-4 py-2.5 rounded-xl border border-slate-200 focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-slate-800 transition"
          />
        </div>

        <div>
          <label htmlFor="budget" className="block text-sm font-semibold text-slate-750 mb-1.5">
            Budget
          </label>
          <select
            id="budget"
            name="budget"
            value={formData.budget}
            onChange={onChange}
            className="w-full px-4 py-2.5 rounded-xl border border-slate-200 focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-slate-800 transition bg-white"
          >
            <option value="Budget">Budget</option>
            <option value="Mid-range">Mid-range</option>
            <option value="Luxury">Luxury</option>
          </select>
        </div>

        <div className="pt-2 flex flex-col space-y-3">
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
              onClick={onStopStream}
              className="w-full flex items-center justify-center space-x-2 bg-red-50 hover:bg-red-100 text-red-600 border border-red-200 py-3 px-6 rounded-xl font-semibold active:scale-[0.99] transition-all"
            >
              <Square className="w-4 h-4 fill-current" />
              <span>Stop Generation</span>
            </button>
          )}

          <label className="flex items-center space-x-2 text-xs text-slate-600 hover:text-slate-800 cursor-pointer pt-1 select-none">
            <input
              type="checkbox"
              checked={autoSaveToDb}
              onChange={(e) => onAutoSaveChange(e.target.checked)}
              className="w-4 h-4 text-emerald-600 rounded border-slate-300 focus:ring-emerald-500 cursor-pointer"
            />
            <span className="flex items-center gap-1.5 font-medium">
              <Database className="w-3.5 h-3.5 text-emerald-600" />
              Auto-save generated itinerary to Spring Boot database
            </span>
          </label>
        </div>
      </form>
    </div>
  );
}
