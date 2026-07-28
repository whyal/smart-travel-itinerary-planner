"use client";

import { Compass, CheckCircle2, Loader2, Clock } from "lucide-react";
import { ItineraryFormData } from "../../types/itinerary";

interface ItineraryLoadingSkeletonProps {
  formData: ItineraryFormData;
  streamedTextLength: number;
}

export default function ItineraryLoadingSkeleton({
  formData,
  streamedTextLength,
}: ItineraryLoadingSkeletonProps) {
  const skeletonDayCount = Math.min(Number(formData.days) || 3, 3);

  return (
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
          <span>{streamedTextLength} chars</span>
        </div>
      </div>

      {/* Skeleton Itinerary Preview */}
      <div className="space-y-4">
        {Array.from({ length: skeletonDayCount }).map((_, dayIdx) => (
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
  );
}
