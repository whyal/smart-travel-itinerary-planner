import {
  IngestionResponse,
  RagDocumentResponse,
  RagStatusResponse,
} from "../types/knowledge";

import { API_BASE_URL, apiHeaders } from "./itineraryApi";

export async function getKnowledgeStatus(): Promise<RagStatusResponse> {
  const response = await fetch(`${API_BASE_URL}/knowledge/status`, {
    headers: apiHeaders(),
  });
  if (!response.ok) {
    throw new Error(`Failed to fetch status: HTTP ${response.status}`);
  }
  return response.json();
}

export async function uploadKnowledgeFile(
  file: File,
  destination?: string,
): Promise<IngestionResponse> {
  const formData = new FormData();
  formData.append("file", file);
  if (destination && destination.trim()) {
    formData.append("destination", destination.trim());
  }

  const response = await fetch(`${API_BASE_URL}/knowledge/upload`, {
    method: "POST",
    headers: apiHeaders(),
    body: formData,
  });

  const data = await response.json().catch(() => null);
  if (!response.ok) {
    throw new Error(
      data?.message || `Upload failed with status HTTP ${response.status}`,
    );
  }
  return data;
}

export async function ingestKnowledgeArticles(
  destination: string,
  articles: string[],
): Promise<IngestionResponse> {
  const response = await fetch(`${API_BASE_URL}/knowledge/articles`, {
    method: "POST",
    headers: apiHeaders({ "Content-Type": "application/json" }),
    body: JSON.stringify({
      destination: destination.trim(),
      articles: articles.filter((a) => a.trim().length > 0),
    }),
  });

  const data = await response.json().catch(() => null);
  if (!response.ok) {
    throw new Error(
      data?.message || `Ingestion failed with status HTTP ${response.status}`,
    );
  }
  return data;
}

export async function searchKnowledge(
  query: string,
  topK: number = 4,
): Promise<RagDocumentResponse[]> {
  const queryParam = encodeURIComponent(query.trim());
  const response = await fetch(
    `${API_BASE_URL}/knowledge/similarity-search?query=${queryParam}&topK=${topK}`,
    { headers: apiHeaders() },
  );

  const data = await response.json().catch(() => null);
  if (!response.ok) {
    throw new Error(
      data?.message || `Search failed with status HTTP ${response.status}`,
    );
  }
  return Array.isArray(data) ? data : [];
}
