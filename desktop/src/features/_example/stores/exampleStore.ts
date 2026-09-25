import { create } from "zustand";

import { newExampleItem, sortByCreatedAt } from "../lib/ids";
import type { ExampleItem } from "../types";

type ExampleState = {
  items: ExampleItem[];
  add: (title: string, description?: string) => void;
  remove: (id: string) => void;
  reset: () => void;
  sorted: () => ExampleItem[];
};

/**
 * Feature-scoped store. Lives under the feature, not in `src/stores/`,
 * so deleting the feature also deletes its state.
 */
export const useExampleStore = create<ExampleState>((set, get) => ({
  items: [],
  add: (title, description) =>
    set((s) => ({ items: [...s.items, newExampleItem(title, description)] })),
  remove: (id) => set((s) => ({ items: s.items.filter((i) => i.id !== id) })),
  reset: () => set({ items: [] }),
  sorted: () => sortByCreatedAt(get().items),
}));
