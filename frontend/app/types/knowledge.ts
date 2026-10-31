export interface RagStatusResponse {
  status: string;
  totalDocumentsIngested: number;
  destinations?: string[];
  documentCountByDestination?: Record<string, number>;
  embeddingProvider?: string;
  embeddingModel?: string;
  embeddingDimensions?: number;
  vectorTable?: string;
}

export interface IngestionResponse {
  status: string;
  message: string;
  destination?: string;
  documentsIngested: number;
}

export interface RagDocumentResponse {
  id: string;
  text: string;
  metadata?: Record<string, unknown>;
  score?: number | null;
}
