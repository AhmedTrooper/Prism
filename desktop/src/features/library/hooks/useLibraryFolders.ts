import { useMemo } from "react";

import { useLibraryStore } from "../stores/libraryStore";
import { groupVideosByFolder } from "../lib/folder";
import type { LibraryFolder, LibraryVideo } from "../types";

export type FolderWithVideos = {
  folder: LibraryFolder;
  videos: LibraryVideo[];
};

/**
 * Hook returning folders paired with their videos, memoized against
 * store changes.
 */
export function useLibraryFolders(): FolderWithVideos[] {
  const folders = useLibraryStore((s) => s.folders);
  const videos = useLibraryStore((s) => s.videos);

  return useMemo(() => {
    const grouped = groupVideosByFolder(videos);
    return folders.map((folder) => ({
      folder,
      videos: grouped.get(folder.id) ?? [],
    }));
  }, [folders, videos]);
}
