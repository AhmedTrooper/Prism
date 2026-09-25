import { describe, it, expect, beforeEach } from "vitest";

import { useLibraryStore } from "@/features/library";
import type { LibraryFolder, LibraryVideo } from "@/features/library";

const folder: LibraryFolder = {
  id: "f1",
  path: "/x",
  label: "x",
  lastScannedAt: null,
};

const video: LibraryVideo = {
  id: "v1",
  folderId: "f1",
  path: "/x/a.mp4",
  title: "a",
  sizeBytes: 10,
  durationMs: 1000,
  modifiedAt: "2026-01-01T00:00:00Z",
};

describe("libraryStore", () => {
  beforeEach(() => {
    useLibraryStore.getState().reset();
  });

  it("starts empty", () => {
    const s = useLibraryStore.getState();
    expect(s.folders).toEqual([]);
    expect(s.videos).toEqual([]);
    expect(s.status).toBe("idle");
  });

  it("adds a folder only if not already present", () => {
    useLibraryStore.getState().addFolder(folder);
    useLibraryStore.getState().addFolder(folder);
    expect(useLibraryStore.getState().folders).toHaveLength(1);
  });

  it("removes a folder and its videos", () => {
    useLibraryStore.getState().addFolder(folder);
    useLibraryStore.getState().upsertVideos([video]);
    useLibraryStore.getState().removeFolder("f1");
    expect(useLibraryStore.getState().folders).toEqual([]);
    expect(useLibraryStore.getState().videos).toEqual([]);
  });

  it("upserts videos by id", () => {
    useLibraryStore.getState().addFolder(folder);
    useLibraryStore.getState().upsertVideos([video]);
    useLibraryStore
      .getState()
      .upsertVideos([{ ...video, title: "updated" }]);
    const v = useLibraryStore.getState().videos[0];
    expect(v?.title).toBe("updated");
    expect(useLibraryStore.getState().videos).toHaveLength(1);
  });

  it("clears videos for one folder only", () => {
    useLibraryStore.getState().addFolder(folder);
    useLibraryStore.getState().addFolder({ ...folder, id: "f2" });
    useLibraryStore.getState().upsertVideos([
      video,
      { ...video, id: "v2", folderId: "f2" },
    ]);
    useLibraryStore.getState().clearVideosForFolder("f1");
    const state = useLibraryStore.getState();
    expect(state.videos).toHaveLength(1);
    expect(state.videos[0]?.folderId).toBe("f2");
  });

  it("updates scan status and remembers last error", () => {
    useLibraryStore.getState().setStatus("scanning");
    expect(useLibraryStore.getState().status).toBe("scanning");
    useLibraryStore.getState().setStatus("error", "boom");
    const s = useLibraryStore.getState();
    expect(s.status).toBe("error");
    expect(s.lastError).toBe("boom");
  });
});
