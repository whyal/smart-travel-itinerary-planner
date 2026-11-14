import { SavedItineraryItem } from "../../types/itinerary";
import { getHistoryList, saveToHistory } from "../historyStorage";

const makeItem = (id: string, textSize = 10): SavedItineraryItem => ({
  id,
  createdAt: Date.now(),
  formData: {
    destination: "Osaka",
    days: 1,
    pace: "Moderate",
    interests: "",
    budget: "Mid-range",
  },
  itinerary: { destination: "Osaka", days: [] },
  streamedText: "x".repeat(textSize),
});

describe("historyStorage.saveToHistory", () => {
  beforeEach(() => localStorage.clear());
  afterEach(() => jest.restoreAllMocks());

  it("keeps only the 30 most recent items", () => {
    for (let i = 0; i < 35; i++) saveToHistory(makeItem(`id-${i}`));

    const list = getHistoryList();
    expect(list).toHaveLength(30);
    expect(list[0].id).toBe("id-34");
  });

  it("drops the oldest items when storage quota is exceeded instead of losing the new one", () => {
    for (let i = 0; i < 5; i++) saveToHistory(makeItem(`old-${i}`));
    const realSetItem = Storage.prototype.setItem;
    jest.spyOn(Storage.prototype, "setItem").mockImplementation(function (
      this: Storage,
      key: string,
      value: string,
    ) {
      if (value.length > 1500)
        throw new DOMException("quota", "QuotaExceededError");
      realSetItem.call(this, key, value);
    });

    const updated = saveToHistory(makeItem("new", 600));

    expect(updated[0].id).toBe("new");
    expect(getHistoryList()[0].id).toBe("new");
    expect(updated.length).toBeLessThan(6);
  });
});
