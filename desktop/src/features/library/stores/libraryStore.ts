import { create } from "zustand";

import type { LibraryFolder, LibraryState, LibraryVideo } from "../types";

type LibraryActions = {
  addFolder: (folder: LibraryFolder) => void;
  removeFolder: (id: string) => void;
  upsertVideos: (videos: LibraryVideo[]) => void;
  clearVideosForFolder: (folderId: string) => void;
  setStatus: (status: LibraryState["status"], error?: string) => void;
  reset: () => void;
};

export type LibraryStore = LibraryState & LibraryActions;

const initial: LibraryState = {
  folders: [],
  videos: [],
  status: "idle",
};

/**
 * Feature-scoped library store. Single source of truth for folders,
 * videos, and scan status on the desktop frontend.
 */
export const useLibraryStore = create<LibraryStore>((set) => ({
  ...initial,
  addFolder: (folder) =>
    set((s) =>
      s.folders.some((f) => f.id === folder.id)
        ? s
        : { folders: [...s.folders, folder] },
    ),
  removeFolder: (id) =>
    set((s) => ({
      folders: s.folders.filter((f) => f.id !== id),
      videos: s.videos.filter((v) => v.folderId !== id),
    })),
  upsertVideos: (videos) =>
    set((s) => {
      const byId = new Map(s.videos.map((v) => [v.id, v]));
      for (const v of videos) byId.set(v.id, v);
      return { videos: Array.from(byId.values()) };
    }),
  clearVideosForFolder: (folderId) =>
    set((s) => ({ videos: s.videos.filter((v) => v.folderId !== folderId) })),
  setStatus: (status, error) =>
    set((s) => ({ status, lastError: error ?? s.lastError })),
  reset: () => set({ ...initial }),
}));
