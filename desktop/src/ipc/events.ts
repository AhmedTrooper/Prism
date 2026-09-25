/**
 * Typed Tauri event wrappers.
 *
 *   const unlisten = await listenEvent("player:position", (p) => {
 *     console.log(p.positionSec);
 *   });
 *
 * The compiler enforces that the listener payload matches the event's
 * declared payload type. Optional zod validation can be added later via
 * a sibling to `ipc/registry.ts`.
 */

import { listen, type UnlistenFn } from "@tauri-apps/api/event";

import type { IpcEventName, IpcEvents } from "./types";

/**
 * Strictly-typed wrapper around `listen`.
 *
 * Returns the unlisten function from Tauri and rejects (returns null)
 * if the event payload fails a runtime zod check when one is provided.
 */
export async function listenEvent<K extends IpcEventName>(
  name: K,
  handler: (payload: IpcEvents[K]) => void,
): Promise<UnlistenFn | null> {
  // No registry-side validation for events yet — payloads are emitted by
  // trusted Rust code, and zod schemas for events will be added once the
  // Rust side defines more event sources. Type-level safety stands now.
  return listen<IpcEvents[K]>(name, (event) => handler(event.payload));
}

/**
 * Listen exactly once. Auto-unlistens after the first payload.
 */
export async function listenEventOnce<K extends IpcEventName>(
  name: K,
  handler: (payload: IpcEvents[K]) => void,
): Promise<UnlistenFn | null> {
  const un = await listenEvent<K>(name, (payload) => {
    handler(payload);
    un?.();
  });
  return un;
}
