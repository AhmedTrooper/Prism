import { create } from "zustand";

export type WindowChrome = "platform" | "frameless";

type WindowState = {
  chrome: WindowChrome;
  sidebarCollapsed: boolean;
  statusBarVisible: boolean;
  setChrome: (chrome: WindowChrome) => void;
  toggleSidebar: () => void;
  setSidebarCollapsed: (collapsed: boolean) => void;
  toggleStatusBar: () => void;
};

/**
 * Cross-feature window chrome store. Driven by `data-*` attributes on
 * `<html>` so CSS handles the actual visual changes.
 */
export const useWindowStore = create<WindowState>((set, get) => ({
  chrome: "platform",
  sidebarCollapsed: false,
  statusBarVisible: true,
  setChrome: (chrome) => {
    set({ chrome });
    if (typeof document !== "undefined") {
      document.documentElement.setAttribute(
        "data-window-chrome",
        chrome,
      );
    }
  },
  toggleSidebar: () => {
    const next = !get().sidebarCollapsed;
    set({ sidebarCollapsed: next });
    if (typeof document !== "undefined") {
      document.documentElement.toggleAttribute("data-sidebar-collapsed", next);
    }
  },
  setSidebarCollapsed: (collapsed) => {
    set({ sidebarCollapsed: collapsed });
    if (typeof document !== "undefined") {
      document.documentElement.toggleAttribute(
        "data-sidebar-collapsed",
        collapsed,
      );
    }
  },
  toggleStatusBar: () => {
    const next = !get().statusBarVisible;
    set({ statusBarVisible: next });
    if (typeof document !== "undefined") {
      document.documentElement.toggleAttribute("data-statusbar-hidden", !next);
    }
  },
}));
