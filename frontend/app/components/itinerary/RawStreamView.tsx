"use client";

import { RefObject, useMemo } from "react";
import { Terminal } from "lucide-react";

interface RawStreamViewProps {
  streamedText: string;
  loading: boolean;
  streamEndRef: RefObject<HTMLDivElement | null>;
}

export default function RawStreamView({
  streamedText,
  loading,
  streamEndRef,
}: RawStreamViewProps) {
  const displayContent = useMemo(() => {
    if (!loading && streamedText) {
      try {
        const clean = streamedText
          .replace(/^```(?:json)?\s*/i, "")
          .replace(/\s*```$/i, "")
          .trim();
        const parsed = JSON.parse(clean);
        return JSON.stringify(parsed, null, 2);
      } catch {
        return streamedText;
      }
    }
    return streamedText;
  }, [loading, streamedText]);

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between text-xs font-mono text-slate-500 mb-2">
        <div className="flex items-center space-x-2">
          <Terminal className="w-4 h-4 text-blue-500" />
          <span>Real-time Stream Feed</span>
        </div>
        <span>{displayContent.length} chars</span>
      </div>
      <div className="p-4 bg-slate-900 text-slate-100 rounded-xl font-mono text-xs sm:text-sm leading-relaxed overflow-x-auto max-h-[480px] overflow-y-auto whitespace-pre-wrap shadow-inner border border-slate-800">
        {displayContent}
        {loading && (
          <span className="inline-block w-2 h-4 ml-1 bg-blue-400 animate-pulse align-middle" />
        )}
        <div ref={streamEndRef} />
      </div>
    </div>
  );
}
