"use client";

import { Calendar, Clock, MapPin, Navigation, Clock3 } from "lucide-react";
import { DayPlan } from "../../types/itinerary";

interface DayPlanCardProps {
  day: DayPlan;
}

export default function DayPlanCard({ day }: DayPlanCardProps) {
  return (
    <div className="bg-white rounded-2xl shadow-lg border border-slate-200/70 overflow-hidden hover:shadow-xl transition-all duration-200">
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
            <div className="space-y-2 text-sm flex-1">
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

              <p className="text-slate-600 leading-relaxed text-sm">
                {act.description}
              </p>

              {(act.gettingThere || act.operatingHours) && (
                <div className="flex flex-wrap gap-2 pt-1 border-t border-slate-100 text-xs">
                  {act.gettingThere && (
                    <div className="inline-flex items-center space-x-1.5 px-2.5 py-1 rounded-md bg-emerald-50 text-emerald-700 border border-emerald-200/70 font-medium">
                      <Navigation className="w-3 h-3 text-emerald-600 shrink-0" />
                      <span>{act.gettingThere}</span>
                    </div>
                  )}
                  {act.operatingHours && (
                    <div className="inline-flex items-center space-x-1.5 px-2.5 py-1 rounded-md bg-amber-50 text-amber-800 border border-amber-200/70 font-medium">
                      <Clock3 className="w-3 h-3 text-amber-600 shrink-0" />
                      <span>{act.operatingHours}</span>
                    </div>
                  )}
                </div>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
