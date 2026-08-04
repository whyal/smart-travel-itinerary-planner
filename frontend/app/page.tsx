import type { Metadata } from "next";
import ItineraryForm from "./components/ItineraryForm";

// Server Components support built-in SEO metadata natively
export const metadata: Metadata = {
  title: "AI Travel Planner | Spring AI & Next.js",
  description:
    "Generate type-safe, token-efficient travel itineraries powered by Google Gemini and Spring Boot.",
};

export default function Home() {
  return (
    <main className="min-h-screen bg-slate-50 py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-3xl mx-auto text-center mb-8">
        <h1 className="text-3xl font-extrabold text-slate-900 sm:text-4xl tracking-tight">
          AI Travel Itinerary Generator
        </h1>
        <p className="mt-3 text-lg text-slate-600">
          Select your travel preferences below to generate a custom day-by-day
          plan.
        </p>
      </div>

      <ItineraryForm />
    </main>
  );
}
