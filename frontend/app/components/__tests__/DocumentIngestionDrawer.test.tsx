import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import DocumentIngestionDrawer from "../knowledge/DocumentIngestionDrawer";
import * as knowledgeApi from "../../services/knowledgeApi";

jest.mock("../../services/knowledgeApi", () => ({
  getKnowledgeStatus: jest.fn(),
  uploadKnowledgeFile: jest.fn(),
  ingestKnowledgeArticles: jest.fn(),
  preloadKnowledge: jest.fn(),
  searchKnowledge: jest.fn(),
}));

describe("DocumentIngestionDrawer Component", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    (knowledgeApi.getKnowledgeStatus as jest.Mock).mockResolvedValue({
      status: "ready",
      totalDocumentsIngested: 8,
      destinations: ["Kyoto", "Osaka"],
      documentCountByDestination: { Kyoto: 5, Osaka: 3 },
      embeddingProvider: "google",
      embeddingModel: "gemini-embedding-001",
      embeddingDimensions: 3072,
      vectorTable: "vector_store_google_3072",
    });
  });

  it("renders trigger button with Knowledge Base label", () => {
    render(<DocumentIngestionDrawer defaultDestination="Kyoto" />);
    expect(
      screen.getByRole("button", { name: /knowledge base/i }),
    ).toBeInTheDocument();
  });

  it("opens drawer and displays status when clicked", async () => {
    const user = userEvent.setup();
    render(<DocumentIngestionDrawer defaultDestination="Kyoto" />);

    const trigger = screen.getByRole("button", { name: /knowledge base/i });
    await user.click(trigger);

    expect(
      await screen.findByText(/knowledge base & rag/i),
    ).toBeInTheDocument();
    expect(knowledgeApi.getKnowledgeStatus).toHaveBeenCalledTimes(1);

    await waitFor(() => {
      expect(screen.getByText(/vector_store_google_3072/i)).toBeInTheDocument();
      expect(screen.getByText(/8 docs/i)).toBeInTheDocument();
    });
  });

  it("switches to articles tab and ingests articles", async () => {
    const user = userEvent.setup();
    (knowledgeApi.ingestKnowledgeArticles as jest.Mock).mockResolvedValue({
      status: "success",
      message: "Successfully ingested 2 articles.",
      destination: "Kyoto",
      documentsIngested: 2,
    });

    render(<DocumentIngestionDrawer defaultDestination="Kyoto" />);

    await user.click(screen.getByRole("button", { name: /knowledge base/i }));

    const articlesTab = await screen.findByRole("button", {
      name: /articles & notes/i,
    });
    await user.click(articlesTab);

    const textarea = screen.getByPlaceholderText(/paste travel advice/i);
    await user.type(
      textarea,
      "Kinkaku-ji is golden.\n\nGinkaku-ji has silver sand.",
    );

    const submitBtn = screen.getByRole("button", { name: /ingest articles/i });
    await user.click(submitBtn);

    expect(knowledgeApi.ingestKnowledgeArticles).toHaveBeenCalledWith("Kyoto", [
      "Kinkaku-ji is golden.",
      "Ginkaku-ji has silver sand.",
    ]);

    expect(
      await screen.findByText(/successfully ingested 2 articles/i),
    ).toBeInTheDocument();
  });

  it("handles test search and displays similarity results", async () => {
    const user = userEvent.setup();
    (knowledgeApi.searchKnowledge as jest.Mock).mockResolvedValue([
      {
        id: "doc-1",
        text: "Kinkaku-ji is a Zen Buddhist temple in northern Kyoto.",
        metadata: { title: "Golden Pavilion", destination: "Kyoto" },
        score: 0.892,
      },
    ]);

    render(<DocumentIngestionDrawer defaultDestination="Kyoto" />);

    await user.click(screen.getByRole("button", { name: /knowledge base/i }));

    const searchTab = await screen.findByRole("button", {
      name: /test search/i,
    });
    await user.click(searchTab);

    const searchInput = screen.getByPlaceholderText(/test similarity search/i);
    await user.type(searchInput, "zen temple");

    const searchBtn = screen.getByRole("button", { name: /^search$/i });
    await user.click(searchBtn);

    expect(knowledgeApi.searchKnowledge).toHaveBeenCalledWith("zen temple", 4);
    expect(await screen.findByText(/golden pavilion/i)).toBeInTheDocument();
    expect(screen.getByText(/score: 0.892/i)).toBeInTheDocument();
  });
});
