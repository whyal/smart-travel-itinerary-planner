"use client";

import { AlertCircle } from "lucide-react";

interface ErrorBannerProps {
  error: string | null;
}

export default function ErrorBanner({ error }: ErrorBannerProps) {
  if (!error) return null;

  return (
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
  );
}
