import { useEffect } from "react";

/**
 * Run cleanup when the keybinding changes or the component unmounts.
 * Compose multiple keybindings safely with this helper.
 *
 *   useKeybinding({
 *     "Mod+K": () => openPalette(),
 *     "Space":  () => player.toggle(),
 *   });
 *
 * Internally registers a single window keydown listener and dispatches
 * to the matching handler. Editable-target detection is shared with
 * useShortcut.
 */
export type KeybindingMap = Record<string, (event: KeyboardEvent) => void>;

export function useKeybinding(map: KeybindingMap): void {
  useEffect(() => {
    function onKeyDown(e: KeyboardEvent) {
      for (const [combo, handler] of Object.entries(map)) {
        if (matchesExact(combo, e)) {
          if (isEditableTarget(e.target)) return;
          e.preventDefault();
          handler(e);
          return;
        }
      }
    }
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
    // Map identity changes every render unless wrapped in useMemo; we
    // accept that and re-bind — handlers are expected to be cheap to set up.
  }, [map]);
}

function matchesExact(combo: string, e: KeyboardEvent): boolean {
  const parts = combo
    .split("+")
    .map((p) => p.trim().toLowerCase())
    .filter(Boolean);
  let key = "";
  let mod = false;
  let shift = false;
  let alt = false;
  for (const p of parts) {
    if (p === "mod" || p === "cmd" || p === "ctrl" || p === "meta") mod = true;
    else if (p === "shift") shift = true;
    else if (p === "alt" || p === "option") alt = true;
    else key = p;
  }
  if (key !== e.key.toLowerCase()) return false;
  if (mod !== (e.metaKey || e.ctrlKey)) return false;
  if (shift !== e.shiftKey) return false;
  if (alt !== e.altKey) return false;
  return true;
}

function isEditableTarget(target: EventTarget | null): boolean {
  if (!(target instanceof HTMLElement)) return false;
  const tag = target.tagName;
  if (tag === "INPUT" || tag === "TEXTAREA" || tag === "SELECT") return true;
  if (target.isContentEditable) return true;
  return false;
}
