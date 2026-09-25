export { LibraryView } from "./components/LibraryView";
export { useLibraryStore } from "./stores/libraryStore";
export { useLibraryFolders, type FolderWithVideos } from "./hooks/useLibraryFolders";
export {
  libraryFolderSchema,
  libraryVideoSchema,
  libraryStateSchema,
} from "./schemas/librarySchema";
export {
  nextFolderId,
  nextVideoId,
  labelFromPath,
  groupVideosByFolder,
  totalDurationMs,
  emptyFolder,
} from "./lib/folder";
export { pickFolder, startScan } from "./api/libraryClient";
export type { LibraryFolder, LibraryVideo, LibraryState } from "./types";
