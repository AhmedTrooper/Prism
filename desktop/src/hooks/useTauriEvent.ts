import { useEffect } from "react";

import { listenEvent, listenEventOnce } from "@/ipc/events";
import type { IpcEventName, IpcEvents } from "@/ipc/types";

/**
 * Subscribe to a typed Tauri event for the lifetime of the calling component.
 *
 *   useTauriEvent("player:position", (p) => setPosition(p.positionSec));
 */
export function useTauriEvent<K extends IpcEventName>(
  name: K,
  handler: (payload: IpcEvents[K]) => void,
): void {
  useEffect(() => {
    let unlisten: Awaited<ReturnType<typeof listenEvent<K>>> | null = null;
    let cancelled = false;
    listenEvent<K>(name, handler).then((un) => {
      if (cancelled) un?.();
      else unlisten = un;
    });
    return () => {
      cancelled = true;
      unlisten?.();
    };
    // We intentionally depend on string `name` only — pass a stable handler
    // or wrap with useCallback in the caller. Avoids stale-closure bugs.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [name]);
}

/**
 * Listen once for an event and auto-unlisten after the first payload.
 */
export function useTauriEventOnce<K extends IpcEventName>(
  name: K,
  handler: (payload: IpcEvents[K]) => void,
): void {
  useEffect(() => {
    let unlisten: Awaited<ReturnType<typeof listenEventOnce<K>>> | null = null;
    let cancelled = false;
    listenEventOnce<K>(name, handler).then((un) => {
      if (cancelled) un?.();
      else unlisten = un;
    });
    return () => {
      cancelled = true;
      unlisten?.();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [name]);
}
