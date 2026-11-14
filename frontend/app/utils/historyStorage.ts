import { SavedItineraryItem } from "../types/itinerary";

const HISTORY_KEY = "itinerary_history_list";
const MAX_HISTORY_ITEMS = 30;

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

export const saveToHistory = (
  item: SavedItineraryItem,
): SavedItineraryItem[] => {
  if (typeof window === "undefined") return [];
  const others = getHistoryList().filter((i) => i.id !== item.id);
  let updated = [item, ...others].slice(0, MAX_HISTORY_ITEMS);
  while (updated.length > 0) {
    try {
      localStorage.setItem(HISTORY_KEY, JSON.stringify(updated));
      return updated;
    } catch (e) {
      if (updated.length === 1) {
        console.error("Failed to save itinerary to history:", e);
        break;
      }
      updated = updated.slice(0, -1);
    }
  }
  return getHistoryList();
};

export const updateHistoryItemSavedToDb = (
  id: string,
  savedToDb: boolean = true,
): SavedItineraryItem[] => {
  if (typeof window === "undefined") return [];
  try {
    const current = getHistoryList();
    const updated = current.map((item) =>
      item.id === id ? { ...item, savedToDb } : item,
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
