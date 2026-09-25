import { create } from "zustand";

export type PalettePage = "root" | string;

export type PaletteCommand = {
  id: string;
  title: string;
  subtitle?: string;
  keywords?: readonly string[];
  group?: string;
  shortcut?: string;
  run: () => void | Promise<void>;
  enabled?: boolean;
};

type CommandPaletteState = {
  isOpen: boolean;
  query: string;
  page: PalettePage;
  commands: PaletteCommand[];
  open: () => void;
  close: () => void;
  toggle: () => void;
  setQuery: (q: string) => void;
  pushPage: (page: PalettePage) => void;
  popPage: () => void;
  resetPage: () => void;
  register: (cmd: PaletteCommand) => void;
  registerMany: (cmds: readonly PaletteCommand[]) => void;
  unregister: (id: string) => void;
  clear: () => void;
};

/**
 * Cross-feature command palette store. Each feature registers its own
 * commands on mount, the palette reads them all and filters live.
 */
export const useCommandPaletteStore = create<CommandPaletteState>(
  (set) => ({
    isOpen: false,
    query: "",
    page: "root",
    commands: [],
    open: () => set({ isOpen: true, query: "", page: "root" }),
    close: () => set({ isOpen: false }),
    toggle: () => set((s) => ({ isOpen: !s.isOpen })),
    setQuery: (q) => set({ query: q }),
    pushPage: (page) => set({ page }),
    popPage: () => set({ page: "root" }),
    resetPage: () => set({ page: "root" }),
    register: (cmd) =>
      set((s) => {
        const idx = s.commands.findIndex((c) => c.id === cmd.id);
        if (idx >= 0) {
          const next = s.commands.slice();
          next[idx] = cmd;
          return { commands: next };
        }
        return { commands: [...s.commands, cmd] };
      }),
    registerMany: (cmds) =>
      set((s) => {
        const byId = new Map(s.commands.map((c) => [c.id, c]));
        for (const c of cmds) byId.set(c.id, c);
        return { commands: Array.from(byId.values()) };
      }),
    unregister: (id) =>
      set((s) => ({ commands: s.commands.filter((c) => c.id !== id) })),
    clear: () => set({ commands: [] }),
  }),
);

/** Score a command against a query. Higher is better. 0 means skip. */
export function scoreCommand(cmd: PaletteCommand, query: string): number {
  if (!query) return 1;
  const q = query.toLowerCase();
  const title = cmd.title.toLowerCase();
  // Case-sensitive exact match — highest tier.
  if (cmd.title === query) return 100;
  // Case-insensitive title match (equal or starts-with).
  if (title === q) return 90;
  if (title.startsWith(q)) return 80;
  if (title.includes(q)) return 50;
  if (cmd.subtitle?.toLowerCase().includes(q)) return 30;
  if (cmd.keywords?.some((k) => k.toLowerCase().includes(q))) return 20;
  return 0;
}
