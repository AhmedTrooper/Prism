import { Badge, Button, Card, Separator } from "@/components/ui";
import { useLibraryFolders } from "../hooks/useLibraryFolders";
import { useLibraryStore } from "../stores/libraryStore";
import { formatBytes, formatDuration } from "@/lib/format";
import { totalDurationMs } from "../lib/folder";

export function LibraryView() {
  const foldersWithVideos = useLibraryFolders();
  const status = useLibraryStore((s) => s.status);
  const lastError = useLibraryStore((s) => s.lastError);

  if (foldersWithVideos.length === 0) {
    return <LibraryEmptyState />;
  }

  return (
    <div className="flex flex-col gap-6 p-6">
      <header className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-semibold">Library</h1>
          <p className="text-sm text-on-surface-muted">
            {status === "scanning"
              ? "Scanning folders…"
              : status === "error"
                ? `Error: ${lastError ?? "unknown"}`
                : `${foldersWithVideos.length} folder${foldersWithVideos.length === 1 ? "" : "s"}`}
          </p>
        </div>
        <Button variant="primary">Add folder</Button>
      </header>
      <Separator />
      <ul className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
        {foldersWithVideos.map(({ folder, videos }) => (
          <li key={folder.id}>
            <Card
              title={folder.label}
              subtitle={
                folder.lastScannedAt
                  ? `Last scanned ${new Date(folder.lastScannedAt).toLocaleString()}`
                  : "Not yet scanned"
              }
              actions={
                videos.length > 0 ? (
                  <Badge tone="primary">
                    {videos.length} video{videos.length === 1 ? "" : "s"}
                  </Badge>
                ) : null
              }
            >
              <p className="text-xs text-on-surface-muted break-all">
                {folder.path}
              </p>
              {videos.length > 0 && (
                <p className="mt-3 text-xs text-on-surface-muted">
                  {formatBytes(videos.reduce((a, v) => a + v.sizeBytes, 0))} •{" "}
                  {formatDuration(totalDurationMs(videos) / 1000)} total
                </p>
              )}
            </Card>
          </li>
        ))}
      </ul>
    </div>
  );
}

function LibraryEmptyState() {
  return (
    <div className="flex h-full items-center justify-center p-12">
      <Card className="max-w-md text-center">
        <h2 className="text-base font-semibold">No folders yet</h2>
        <p className="mt-2 text-sm text-on-surface-muted">
          Add a folder containing videos to start your library. Prism will
          index each file and surface thumbnails here.
        </p>
        <div className="mt-4 flex justify-center">
          <Button variant="primary">Add folder</Button>
        </div>
      </Card>
    </div>
  );
}
