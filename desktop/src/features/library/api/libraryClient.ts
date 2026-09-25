/**
 * Library feature API. All IPC calls go through `invokeCommand` so the
 * Rust side stays the single source of truth.
 */

import { ok, type Result } from "@/lib/result";

// We intentionally avoid importing `invokeCommand` here until those
// commands are registered in `ipc/registry.ts`. The `Result` shape is
// preserved for callers.

export async function pickFolder(): Promise<Result<string | null, Error>> {
  // TODO: replace with invokeCommand("library:pick-folder", {}) once the
  // Tauri command lands. Returning null means "user cancelled".
  return ok(null);
}

export async function startScan(
  _folderId: string,
): Promise<Result<{ startedAt: number }, Error>> {
  // TODO: invokeCommand("library:scan", { folderId })
  return ok({ startedAt: Date.now() });
}
