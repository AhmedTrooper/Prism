import { describe, it, expect } from "vitest";

import {
  labelFromPath,
  groupVideosByFolder,
  totalDurationMs,
  emptyFolder,
} from "@/features/library";
import type { LibraryVideo } from "@/features/library";

const video = (
  id: string,
  folderId: string,
  durationMs: number | null,
): LibraryVideo => ({
  id,
  folderId,
  path: `/videos/${id}.mp4`,
  title: id,
  sizeBytes: 1000,
  durationMs,
  modifiedAt: "2026-01-01T00:00:00Z",
});

describe("library helpers", () => {
  it("labels by last path segment", () => {
    expect(labelFromPath("/home/me/Movies")).toBe("Movies");
    expect(labelFromPath("C:\\Movies\\Anime")).toBe("Anime");
    expect(labelFromPath("single")).toBe("single");
  });

  it("strips trailing separators before labelling", () => {
    expect(labelFromPath("/home/me/Movies/")).toBe("Movies");
    expect(labelFromPath("C:\\Movies\\Anime\\")).toBe("Anime");
  });

  it("groups videos by folder", () => {
    const a = video("a", "f1", 1000);
    const b = video("b", "f1", 2000);
    const c = video("c", "f2", 500);
    const groups = groupVideosByFolder([a, b, c]);
    expect(groups.get("f1")?.length).toBe(2);
    expect(groups.get("f2")?.length).toBe(1);
    expect(groups.get("missing")).toBeUndefined();
  });

  it("sums durations across videos, skipping missing durations", () => {
    expect(totalDurationMs([video("a", "f", 1000), video("b", "f", 2500)])).toBe(3500);
    expect(totalDurationMs([video("a", "f", 1000), video("b", "f", null)])).toBe(1000);
    expect(totalDurationMs([])).toBe(0);
  });

  it("produces a new empty folder with no label and no path", () => {
    const f = emptyFolder();
    expect(f.path).toBe("");
    expect(f.label).toBe("");
    expect(f.lastScannedAt).toBeNull();
    expect(f.id).toMatch(/^fld_/);
  });
});
