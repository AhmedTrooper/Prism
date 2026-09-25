/**
 * Folder-level helpers. Keep this file dependency-free so it stays
 * trivially unit-testable.
 */

import type { LibraryFolder, LibraryVideo } from "../types";

let counter = 0;
export function nextFolderId(): string {
  counter += 1;
  return `fld_${Date.now().toString(36)}_${counter}`;
}

let videoCounter = 0;
export function nextVideoId(): string {
  videoCounter += 1;
  return `vid_${Date.now().toString(36)}_${videoCounter}`;
}

export function labelFromPath(path: string): string {
  // Trim trailing slashes and grab the last segment as a friendly label.
  const trimmed = path.replace(/[\\/]+$/, "");
  const parts = trimmed.split(/[\\/]/);
  return parts[parts.length - 1] ?? trimmed;
}

export function groupVideosByFolder(
  videos: LibraryVideo[],
): Map<string, LibraryVideo[]> {
  const out = new Map<string, LibraryVideo[]>();
  for (const v of videos) {
    const list = out.get(v.folderId) ?? [];
    list.push(v);
    out.set(v.folderId, list);
  }
  return out;
}

export function totalDurationMs(videos: LibraryVideo[]): number {
  return videos.reduce(
    (acc, v) => acc + (v.durationMs ?? 0),
    0,
  );
}

export function emptyFolder(): LibraryFolder {
  return {
    id: nextFolderId(),
    path: "",
    label: "",
    lastScannedAt: null,
  };
}
