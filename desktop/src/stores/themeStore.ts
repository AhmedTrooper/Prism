import { create } from "zustand";

import { usePersistedState } from "@/hooks/usePersistedState";

export type ThemeMode = "system" | "light" | "dark";

type ThemeState = {
  mode: ThemeMode;
  setMode: (mode: ThemeMode) => void;
  cycle: () => void;
};

const STORAGE_KEY = "prism:theme-mode";

function readInitial(): ThemeMode {
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    if (raw === "system" || raw === "light" || raw === "dark") return raw;
  } catch {
    // fall through
  }
  return "system";
}

/**
 * Cross-feature theme store. The `data-theme` attribute on `<html>` is
 * the single source of truth at render time; this store just decides
 * which attribute to set.
 */
export const useThemeStore = create<ThemeState>((set, get) => ({
  mode: readInitial(),
  setMode: (mode) => {
    try {
      window.localStorage.setItem(STORAGE_KEY, mode);
    } catch {
      // ignore
    }
    set({ mode });
    applyThemeAttribute(mode);
  },
  cycle: () => {
    const order: ThemeMode[] = ["system", "light", "dark"];
    const cur = get().mode;
    const idx = order.indexOf(cur);
    const next = order[(idx + 1) % order.length] ?? "system";
    get().setMode(next);
  },
}));

/**
 * Resolve "system" against the OS preference and write the resulting
 * `light` or `dark` to `<html data-theme>`. Idempotent.
 */
export function applyThemeAttribute(mode: ThemeMode): void {
  if (typeof document === "undefined") return;
  const resolved =
    mode === "system"
      ? window.matchMedia("(prefers-color-scheme: dark)").matches
        ? "dark"
        : "light"
      : mode;
  document.documentElement.setAttribute("data-theme", resolved);
}

// Wire the system-mode listener exactly once. Safe to import repeatedly.
let systemListenerAttached = false;
export function ensureThemeSystemListener(): void {
  if (systemListenerAttached || typeof window === "undefined") return;
  systemListenerAttached = true;
  window
    .matchMedia("(prefers-color-scheme: dark)")
    .addEventListener("change", () => {
      if (useThemeStore.getState().mode === "system") applyThemeAttribute("system");
    });
}

// Convenience hook: theme + persisted re-render trigger for components
// that watch localStorage directly.
export function useThemeMode(): [ThemeMode, (next: ThemeMode) => void] {
  // usePersistedState triggers a re-render when localStorage mutates from
  // outside the store (rare but possible). Most updates go through the
  // store anyway.
  const [, setStored] = usePersistedState<ThemeMode>(STORAGE_KEY, "system");
  const mode = useThemeStore((s) => s.mode);
  return [mode, (next) => setStored(next)];
}
