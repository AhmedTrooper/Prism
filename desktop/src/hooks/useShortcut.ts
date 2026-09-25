import { useEffect } from "react";

/**
 * Listen for a single keyboard shortcut on the window.
 *
 *   useShortcut("Space", () => player.toggle());
 *   useShortcut("Mod+K", () => openPalette()); // Cmd on macOS, Ctrl elsewhere
 *
 * The handler fires only when the shortcut is matched exactly and no
 * editable element is focused. Editable detection is intentionally
 * conservative (input, textarea, contenteditable) so we never hijack text.
 */
export function useShortcut(
  combo: string,
  handler: (event: KeyboardEvent) => void,
): void {
  useEffect(() => {
    const parsed = parseCombo(combo);
    function onKeyDown(e: KeyboardEvent) {
      if (!matchesCombo(parsed, e)) return;
      if (isEditableTarget(e.target)) return;
      e.preventDefault();
      handler(e);
    }
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [combo, handler]);
}

type ParsedCombo = {
  key: string;
  mod: boolean;
  shift: boolean;
  alt: boolean;
};

function parseCombo(combo: string): ParsedCombo {
  const parts = combo
    .split("+")
    .map((p) => p.trim())
    .filter(Boolean);
  let key = "";
  let mod = false;
  let shift = false;
  let alt = false;
  for (const p of parts) {
    const lower = p.toLowerCase();
    if (lower === "mod" || lower === "cmd" || lower === "ctrl" || lower === "meta") {
      mod = true;
    } else if (lower === "shift") {
      shift = true;
    } else if (lower === "alt" || lower === "option") {
      alt = true;
    } else {
      key = p.toLowerCase();
    }
  }
  return { key, mod, shift, alt };
}

function matchesCombo(parsed: ParsedCombo, e: KeyboardEvent): boolean {
  const key = e.key.toLowerCase();
  if (key !== parsed.key) return false;
  const wantsMod = parsed.mod;
  const hasMod = e.metaKey || e.ctrlKey;
  if (wantsMod !== hasMod) return false;
  if (parsed.shift !== e.shiftKey) return false;
  if (parsed.alt !== e.altKey) return false;
  return true;
}

function isEditableTarget(target: EventTarget | null): boolean {
  if (!(target instanceof HTMLElement)) return false;
  const tag = target.tagName;
  if (tag === "INPUT" || tag === "TEXTAREA" || tag === "SELECT") return true;
  if (target.isContentEditable) return true;
  return false;
}
