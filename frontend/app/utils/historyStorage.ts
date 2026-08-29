import { SavedItineraryItem } from "../types/itinerary";

const HISTORY_KEY = "itinerary_history_list";

export const getHistoryList = (): SavedItineraryItem[] => {
  if (typeof window === "undefined") return [];
  try {
    const raw = localStorage.getItem(HISTORY_KEY);
    return raw ? JSON.parse(raw) : [];
  } catch (e) {
    console.error("Failed to load itinerary history:", e);
    return [];
  }
};

export const saveToHistory = (item: SavedItineraryItem): SavedItineraryItem[] => {
  if (typeof window === "undefined") return [];
  try {
    const current = getHistoryList();
    const filtered = current.filter((i) => i.id !== item.id);
    const updated = [item, ...filtered];
    localStorage.setItem(HISTORY_KEY, JSON.stringify(updated));
    return updated;
  } catch (e) {
    console.error("Failed to save itinerary to history:", e);
    return getHistoryList();
  }
};

export const updateHistoryItemSavedToDb = (
  id: string,
  savedToDb: boolean = true
): SavedItineraryItem[] => {
  if (typeof window === "undefined") return [];
  try {
    const current = getHistoryList();
    const updated = current.map((item) =>
      item.id === id ? { ...item, savedToDb } : item
    );
    localStorage.setItem(HISTORY_KEY, JSON.stringify(updated));
    return updated;
  } catch (e) {
    console.error("Failed to update itinerary history item:", e);
    return getHistoryList();
  }
};

export const deleteFromHistory = (id: string): SavedItineraryItem[] => {
  if (typeof window === "undefined") return [];
  try {
    const current = getHistoryList();
    const updated = current.filter((i) => i.id !== id);
    localStorage.setItem(HISTORY_KEY, JSON.stringify(updated));
    return updated;
  } catch (e) {
    console.error("Failed to delete itinerary from history:", e);
    return getHistoryList();
  }
};
