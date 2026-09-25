export interface LibraryFolder {
  id: string;
  path: string;
  label: string;
  /** ISO timestamp of last scan, or null if never scanned. */
  lastScannedAt: string | null;
}

export interface LibraryVideo {
  id: string;
  folderId: string;
  path: string;
  title: string;
  sizeBytes: number;
  durationMs: number | null;
  modifiedAt: string;
  /** Optional thumbnail path, rendered by the player shell. */
  thumbnailPath?: string;
}

export interface LibraryState {
  folders: LibraryFolder[];
  videos: LibraryVideo[];
  status: "idle" | "scanning" | "ready" | "error";
  lastError?: string;
}
