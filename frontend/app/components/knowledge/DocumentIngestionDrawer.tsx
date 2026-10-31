"use client";

import { useState, useCallback, ChangeEvent, FormEvent } from "react";
import {
  BookOpen,
  X,
  Upload,
  FileText,
  Database,
  Search,
  RefreshCw,
  Loader2,
  CheckCircle2,
  AlertCircle,
  File,
} from "lucide-react";
import { RagStatusResponse, RagDocumentResponse } from "../../types/knowledge";
import {
  getKnowledgeStatus,
  uploadKnowledgeFile,
  ingestKnowledgeArticles,
  preloadKnowledge,
  searchKnowledge,
} from "../../services/knowledgeApi";

interface DocumentIngestionDrawerProps {
  defaultDestination?: string;
}

type TabType = "upload" | "articles" | "preload" | "search";

export default function DocumentIngestionDrawer({
  defaultDestination = "Osaka",
}: DocumentIngestionDrawerProps) {
  const [isOpen, setIsOpen] = useState(false);
  const [activeTab, setActiveTab] = useState<TabType>("upload");

  // Status state
  const [status, setStatus] = useState<RagStatusResponse | null>(null);
  const [loadingStatus, setLoadingStatus] = useState(false);

  // Ingestion action states
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [feedback, setFeedback] = useState<{
    type: "success" | "error";
    message: string;
  } | null>(null);

  // Form states
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [fileDestination, setFileDestination] = useState(defaultDestination);

  const [articlesDestination, setArticlesDestination] =
    useState(defaultDestination);
  const [articlesText, setArticlesText] = useState("");

  const [preloadDestination, setPreloadDestination] = useState("Kyoto");

  const [searchQuery, setSearchQuery] = useState("");
  const [searchResults, setSearchResults] = useState<RagDocumentResponse[]>([]);
  const [isSearching, setIsSearching] = useState(false);

  // Fetch status when drawer opens or refreshed
  const fetchStatus = useCallback(async () => {
    setLoadingStatus(true);
    try {
      const data = await getKnowledgeStatus();
      setStatus(data);
    } catch {
      // Backend may be offline or in simple mode without breaking UI
    } finally {
      setLoadingStatus(false);
    }
  }, []);

  const handleOpen = () => {
    setIsOpen(true);
    if (defaultDestination) {
      setFileDestination(defaultDestination);
      setArticlesDestination(defaultDestination);
    }
    fetchStatus();
  };

  // Handle File Upload
  const handleFileChange = (e: ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (files && files.length > 0) {
      setSelectedFile(files[0]);
      setFeedback(null);
    }
  };

  const handleFileUpload = async (e: FormEvent) => {
    e.preventDefault();
    if (!selectedFile) {
      setFeedback({
        type: "error",
        message: "Please select a file to ingest.",
      });
      return;
    }

    setIsSubmitting(true);
    setFeedback(null);
    try {
      const res = await uploadKnowledgeFile(selectedFile, fileDestination);
      setFeedback({
        type: "success",
        message:
          res.message ||
          `Successfully ingested ${res.documentsIngested} documents from ${selectedFile.name}.`,
      });
      setSelectedFile(null);
      await fetchStatus();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "File ingestion failed.";
      setFeedback({ type: "error", message: msg });
    } finally {
      setIsSubmitting(false);
    }
  };

  // Handle Articles Ingestion
  const handleArticlesSubmit = async (e: FormEvent) => {
    e.preventDefault();
    const articles = articlesText
      .split(/\n\s*\n/)
      .map((a) => a.trim())
      .filter((a) => a.length > 0);

    if (articles.length === 0) {
      setFeedback({
        type: "error",
        message: "Please enter at least one article or travel note.",
      });
      return;
    }

    setIsSubmitting(true);
    setFeedback(null);
    try {
      const res = await ingestKnowledgeArticles(articlesDestination, articles);
      setFeedback({
        type: "success",
        message:
          res.message ||
          `Successfully ingested ${res.documentsIngested} articles for ${articlesDestination}.`,
      });
      setArticlesText("");
      await fetchStatus();
    } catch (err: unknown) {
      const msg =
        err instanceof Error ? err.message : "Article ingestion failed.";
      setFeedback({ type: "error", message: msg });
    } finally {
      setIsSubmitting(false);
    }
  };

  // Handle Preload Bundled Data
  const handlePreloadSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setIsSubmitting(true);
    setFeedback(null);
    try {
      const res = await preloadKnowledge(preloadDestination);
      setFeedback({
        type: res.status === "not_found" ? "error" : "success",
        message: res.message,
      });
      await fetchStatus();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Preload failed.";
      setFeedback({ type: "error", message: msg });
    } finally {
      setIsSubmitting(false);
    }
  };

  // Handle Similarity Search Test
  const handleSearchSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!searchQuery.trim()) return;

    setIsSearching(true);
    setFeedback(null);
    try {
      const results = await searchKnowledge(searchQuery, 4);
      setSearchResults(results);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Search query failed.";
      setFeedback({ type: "error", message: msg });
    } finally {
      setIsSearching(false);
    }
  };

  return (
    <>
      {/* Trigger Button */}
      <button
        type="button"
        onClick={handleOpen}
        title="Open Knowledge Base & RAG Ingestion"
        className="flex items-center space-x-2 bg-white hover:bg-slate-50 text-slate-700 border border-slate-200 hover:border-emerald-300 rounded-xl px-3.5 py-2 text-xs sm:text-sm font-semibold shadow-2xs hover:shadow-xs transition-all"
      >
        <BookOpen className="w-4 h-4 text-emerald-600" />
        <span className="hidden sm:inline">Knowledge Base</span>
        <span className="sm:hidden">RAG</span>
        {status && status.totalDocumentsIngested > 0 && (
          <span className="ml-1 px-1.5 py-0.5 bg-emerald-100 text-emerald-800 font-bold rounded-full text-xs">
            {status.totalDocumentsIngested}
          </span>
        )}
      </button>

      {/* Slide-over Drawer / Modal Overlay */}
      {isOpen && (
        <div className="fixed inset-0 z-50 overflow-hidden bg-slate-900/50 backdrop-blur-xs flex justify-end transition-opacity">
          <div className="w-full max-w-xl bg-white h-full shadow-2xl flex flex-col animate-in slide-in-from-right duration-300">
            {/* Header */}
            <div className="p-6 bg-slate-900 text-white flex items-center justify-between">
              <div className="flex items-center space-x-3">
                <div className="p-2.5 bg-emerald-600 rounded-xl text-white">
                  <Database className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="text-lg font-bold text-white">
                    Knowledge Base & RAG
                  </h3>
                  <p className="text-xs text-slate-400">
                    Ingest guides & manage vector store retrieval
                  </p>
                </div>
              </div>
              <button
                type="button"
                onClick={() => setIsOpen(false)}
                className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Scrollable Content */}
            <div className="flex-1 overflow-y-auto p-6 space-y-6">
              {/* Active Vector Store Status Box */}
              <div className="p-4 bg-slate-50 rounded-2xl border border-slate-200">
                <div className="flex items-center justify-between mb-3">
                  <div className="flex items-center space-x-2">
                    <span className="text-xs font-bold uppercase tracking-wider text-slate-500">
                      RAG Vector Space
                    </span>
                    <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-semibold bg-emerald-100 text-emerald-800">
                      {status?.status === "ready" ? "Active" : "Ready"}
                    </span>
                  </div>
                  <button
                    type="button"
                    onClick={fetchStatus}
                    disabled={loadingStatus}
                    title="Refresh Vector Store Stats"
                    className="text-xs text-slate-500 hover:text-slate-800 flex items-center space-x-1 p-1 rounded hover:bg-slate-200/60 transition"
                  >
                    <RefreshCw
                      className={`w-3.5 h-3.5 ${
                        loadingStatus ? "animate-spin" : ""
                      }`}
                    />
                    <span>Refresh</span>
                  </button>
                </div>

                <div className="grid grid-cols-2 gap-3 text-xs mb-3">
                  <div className="p-2.5 bg-white rounded-xl border border-slate-100">
                    <span className="text-slate-400 block mb-0.5">
                      Model & Dims
                    </span>
                    <span className="font-semibold text-slate-800 break-all">
                      {status?.embeddingModel || "gemini-embedding-001"}
                      <span className="text-slate-500 ml-1">
                        ({status?.embeddingDimensions || 3072}d)
                      </span>
                    </span>
                  </div>
                  <div className="p-2.5 bg-white rounded-xl border border-slate-100">
                    <span className="text-slate-400 block mb-0.5">
                      Vector Table
                    </span>
                    <span
                      className="font-mono text-slate-800 font-semibold truncate block"
                      title={status?.vectorTable || ""}
                    >
                      {status?.vectorTable || "vector_store_google_3072"}
                    </span>
                  </div>
                </div>

                {/* Ingested Destinations */}
                <div className="flex items-center justify-between text-xs pt-2 border-t border-slate-200/60">
                  <span className="text-slate-500">
                    Total Indexed:{" "}
                    <strong className="text-slate-900 font-bold">
                      {status?.totalDocumentsIngested ?? 0} docs
                    </strong>
                  </span>
                  <div className="flex flex-wrap gap-1 max-w-[60%] justify-end">
                    {status?.documentCountByDestination &&
                    Object.keys(status.documentCountByDestination).length >
                      0 ? (
                      Object.entries(status.documentCountByDestination).map(
                        ([dest, count]) => (
                          <span
                            key={dest}
                            className="px-2 py-0.5 bg-white border border-slate-200 rounded-md text-[11px] font-medium text-slate-700"
                          >
                            {dest}: {count}
                          </span>
                        ),
                      )
                    ) : (
                      <span className="text-slate-400 italic">
                        No destinations yet
                      </span>
                    )}
                  </div>
                </div>
              </div>

              {/* Feedback Alert */}
              {feedback && (
                <div
                  className={`p-4 rounded-xl flex items-start space-x-3 text-xs leading-relaxed ${
                    feedback.type === "success"
                      ? "bg-emerald-50 text-emerald-900 border border-emerald-200"
                      : "bg-rose-50 text-rose-900 border border-rose-200"
                  }`}
                >
                  {feedback.type === "success" ? (
                    <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0 mt-0.5" />
                  ) : (
                    <AlertCircle className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
                  )}
                  <span className="flex-1">{feedback.message}</span>
                  <button
                    type="button"
                    onClick={() => setFeedback(null)}
                    className="text-slate-400 hover:text-slate-600"
                  >
                    <X className="w-3.5 h-3.5" />
                  </button>
                </div>
              )}

              {/* Navigation Tabs */}
              <div className="flex border-b border-slate-200 text-xs font-semibold">
                <button
                  type="button"
                  onClick={() => {
                    setActiveTab("upload");
                    setFeedback(null);
                  }}
                  className={`pb-2.5 px-3 border-b-2 flex items-center space-x-1.5 transition ${
                    activeTab === "upload"
                      ? "border-emerald-600 text-emerald-700"
                      : "border-transparent text-slate-500 hover:text-slate-800"
                  }`}
                >
                  <Upload className="w-3.5 h-3.5" />
                  <span>File Upload</span>
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setActiveTab("articles");
                    setFeedback(null);
                  }}
                  className={`pb-2.5 px-3 border-b-2 flex items-center space-x-1.5 transition ${
                    activeTab === "articles"
                      ? "border-emerald-600 text-emerald-700"
                      : "border-transparent text-slate-500 hover:text-slate-800"
                  }`}
                >
                  <FileText className="w-3.5 h-3.5" />
                  <span>Articles & Notes</span>
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setActiveTab("preload");
                    setFeedback(null);
                  }}
                  className={`pb-2.5 px-3 border-b-2 flex items-center space-x-1.5 transition ${
                    activeTab === "preload"
                      ? "border-emerald-600 text-emerald-700"
                      : "border-transparent text-slate-500 hover:text-slate-800"
                  }`}
                >
                  <Database className="w-3.5 h-3.5" />
                  <span>Preload Data</span>
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setActiveTab("search");
                    setFeedback(null);
                  }}
                  className={`pb-2.5 px-3 border-b-2 flex items-center space-x-1.5 transition ${
                    activeTab === "search"
                      ? "border-emerald-600 text-emerald-700"
                      : "border-transparent text-slate-500 hover:text-slate-800"
                  }`}
                >
                  <Search className="w-3.5 h-3.5" />
                  <span>Test Search</span>
                </button>
              </div>

              {/* Tab 1: File Upload */}
              {activeTab === "upload" && (
                <form onSubmit={handleFileUpload} className="space-y-4">
                  <div>
                    <label
                      htmlFor="upload-dest"
                      className="block text-xs font-bold text-slate-700 mb-1"
                    >
                      Target Destination
                    </label>
                    <input
                      id="upload-dest"
                      type="text"
                      value={fileDestination}
                      onChange={(e) => setFileDestination(e.target.value)}
                      placeholder="e.g. Kyoto, Osaka, Tokyo"
                      className="w-full text-xs px-3 py-2 border border-slate-200 rounded-xl focus:ring-2 focus:ring-emerald-500 focus:outline-hidden"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-bold text-slate-700 mb-1">
                      Document File (.pdf, .json, .txt, .md)
                    </label>
                    <div className="border-2 border-dashed border-slate-200 rounded-2xl p-6 text-center hover:border-emerald-400 transition bg-slate-50/50">
                      <input
                        type="file"
                        id="knowledge-file-input"
                        accept=".pdf,.json,.txt,.md"
                        onChange={handleFileChange}
                        className="hidden"
                      />
                      <label
                        htmlFor="knowledge-file-input"
                        className="cursor-pointer flex flex-col items-center justify-center space-y-2"
                      >
                        <div className="p-3 bg-emerald-50 text-emerald-600 rounded-xl">
                          <Upload className="w-5 h-5" />
                        </div>
                        <span className="text-xs font-semibold text-slate-700">
                          Click to select a file or drag here
                        </span>
                        <span className="text-[11px] text-slate-400">
                          Supports PDF guidebooks, structured JSON, Markdown, or
                          raw text
                        </span>
                      </label>
                      {selectedFile && (
                        <div className="mt-4 inline-flex items-center space-x-2 bg-emerald-50 text-emerald-800 px-3 py-1.5 rounded-lg text-xs font-medium border border-emerald-200">
                          <File className="w-3.5 h-3.5 text-emerald-600" />
                          <span>{selectedFile.name}</span>
                          <span className="text-emerald-600 text-[10px]">
                            ({(selectedFile.size / 1024).toFixed(1)} KB)
                          </span>
                        </div>
                      )}
                    </div>
                  </div>

                  <button
                    type="submit"
                    disabled={isSubmitting || !selectedFile}
                    className="w-full py-2.5 px-4 bg-emerald-600 hover:bg-emerald-700 disabled:bg-slate-300 text-white text-xs font-bold rounded-xl shadow-xs transition flex items-center justify-center space-x-2"
                  >
                    {isSubmitting ? (
                      <>
                        <Loader2 className="w-4 h-4 animate-spin" />
                        <span>Embedding & Ingesting File...</span>
                      </>
                    ) : (
                      <>
                        <Upload className="w-4 h-4" />
                        <span>Ingest Selected File</span>
                      </>
                    )}
                  </button>
                </form>
              )}

              {/* Tab 2: Articles & Notes */}
              {activeTab === "articles" && (
                <form onSubmit={handleArticlesSubmit} className="space-y-4">
                  <div>
                    <label
                      htmlFor="articles-dest"
                      className="block text-xs font-bold text-slate-700 mb-1"
                    >
                      Destination
                    </label>
                    <input
                      id="articles-dest"
                      type="text"
                      value={articlesDestination}
                      onChange={(e) => setArticlesDestination(e.target.value)}
                      placeholder="e.g. Kyoto, Osaka"
                      className="w-full text-xs px-3 py-2 border border-slate-200 rounded-xl focus:ring-2 focus:ring-emerald-500 focus:outline-hidden"
                    />
                  </div>

                  <div>
                    <label
                      htmlFor="articles-content"
                      className="block text-xs font-bold text-slate-700 mb-1"
                    >
                      Travel Notes or Articles
                    </label>
                    <textarea
                      id="articles-content"
                      rows={6}
                      value={articlesText}
                      onChange={(e) => setArticlesText(e.target.value)}
                      placeholder="Paste travel advice, landmark highlights, or secret spots here. Separate distinct places with double newlines."
                      className="w-full text-xs px-3 py-2 border border-slate-200 rounded-xl focus:ring-2 focus:ring-emerald-500 focus:outline-hidden font-mono"
                    />
                    <span className="text-[11px] text-slate-400 block mt-1">
                      Each paragraph separated by a blank line will be embedded
                      as a distinct vector item.
                    </span>
                  </div>

                  <button
                    type="submit"
                    disabled={isSubmitting || !articlesText.trim()}
                    className="w-full py-2.5 px-4 bg-emerald-600 hover:bg-emerald-700 disabled:bg-slate-300 text-white text-xs font-bold rounded-xl shadow-xs transition flex items-center justify-center space-x-2"
                  >
                    {isSubmitting ? (
                      <>
                        <Loader2 className="w-4 h-4 animate-spin" />
                        <span>Embedding Articles...</span>
                      </>
                    ) : (
                      <>
                        <FileText className="w-4 h-4" />
                        <span>Ingest Articles</span>
                      </>
                    )}
                  </button>
                </form>
              )}

              {/* Tab 3: Preload Bundled Data */}
              {activeTab === "preload" && (
                <form onSubmit={handlePreloadSubmit} className="space-y-4">
                  <p className="text-xs text-slate-600 leading-relaxed">
                    Preloads bundled travel knowledge files packaged in the
                    backend classpath (e.g.{" "}
                    <code>classpath:data/{`{destination}`}/...json</code>).
                  </p>

                  <div>
                    <label
                      htmlFor="preload-dest"
                      className="block text-xs font-bold text-slate-700 mb-1"
                    >
                      Bundled Destination
                    </label>
                    <input
                      id="preload-dest"
                      type="text"
                      value={preloadDestination}
                      onChange={(e) => setPreloadDestination(e.target.value)}
                      placeholder="e.g. Kyoto, Osaka"
                      className="w-full text-xs px-3 py-2 border border-slate-200 rounded-xl focus:ring-2 focus:ring-emerald-500 focus:outline-hidden"
                    />
                  </div>

                  <button
                    type="submit"
                    disabled={isSubmitting || !preloadDestination.trim()}
                    className="w-full py-2.5 px-4 bg-emerald-600 hover:bg-emerald-700 disabled:bg-slate-300 text-white text-xs font-bold rounded-xl shadow-xs transition flex items-center justify-center space-x-2"
                  >
                    {isSubmitting ? (
                      <>
                        <Loader2 className="w-4 h-4 animate-spin" />
                        <span>Preloading Dataset...</span>
                      </>
                    ) : (
                      <>
                        <Database className="w-4 h-4" />
                        <span>Preload Knowledge</span>
                      </>
                    )}
                  </button>
                </form>
              )}

              {/* Tab 4: Test Search */}
              {activeTab === "search" && (
                <div className="space-y-4">
                  <form
                    onSubmit={handleSearchSubmit}
                    className="flex space-x-2"
                  >
                    <input
                      type="text"
                      value={searchQuery}
                      onChange={(e) => setSearchQuery(e.target.value)}
                      placeholder="Test similarity search (e.g. zen temple gardens)..."
                      className="flex-1 text-xs px-3 py-2 border border-slate-200 rounded-xl focus:ring-2 focus:ring-emerald-500 focus:outline-hidden"
                    />
                    <button
                      type="submit"
                      disabled={isSearching || !searchQuery.trim()}
                      className="py-2 px-3.5 bg-slate-900 hover:bg-slate-800 disabled:bg-slate-300 text-white text-xs font-bold rounded-xl transition flex items-center space-x-1"
                    >
                      {isSearching ? (
                        <Loader2 className="w-3.5 h-3.5 animate-spin" />
                      ) : (
                        <Search className="w-3.5 h-3.5" />
                      )}
                      <span>Search</span>
                    </button>
                  </form>

                  {/* Results list */}
                  <div className="space-y-2.5 pt-2">
                    {searchResults.length === 0 ? (
                      <div className="text-center py-8 text-xs text-slate-400 italic">
                        Enter a search query to test RAG similarity retrieval
                        scores.
                      </div>
                    ) : (
                      searchResults.map((item, idx) => (
                        <div
                          key={item.id || idx}
                          className="p-3 bg-slate-50 rounded-xl border border-slate-200 text-xs space-y-1.5"
                        >
                          <div className="flex items-center justify-between">
                            <span className="font-semibold text-slate-800 truncate">
                              {(item.metadata?.title as string) ||
                                (item.metadata?.destination as string) ||
                                `Result #${idx + 1}`}
                            </span>
                            {item.score !== undefined &&
                              item.score !== null && (
                                <span className="px-2 py-0.5 rounded-full text-[11px] font-bold bg-blue-100 text-blue-700">
                                  Score: {item.score.toFixed(3)}
                                </span>
                              )}
                          </div>
                          <p className="text-slate-600 text-[11px] line-clamp-3 leading-relaxed">
                            {item.text}
                          </p>
                        </div>
                      ))
                    )}
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </>
  );
}
