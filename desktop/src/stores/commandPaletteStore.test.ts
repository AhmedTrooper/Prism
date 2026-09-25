import { describe, it, expect, beforeEach } from "vitest";

import {
  useCommandPaletteStore,
  scoreCommand,
  type PaletteCommand,
} from "@/stores/commandPaletteStore";

describe("commandPaletteStore", () => {
  beforeEach(() => {
    useCommandPaletteStore.getState().clear();
    useCommandPaletteStore.setState({ isOpen: false, query: "", page: "root" });
  });

  it("starts closed with no commands", () => {
    const s = useCommandPaletteStore.getState();
    expect(s.isOpen).toBe(false);
    expect(s.commands).toEqual([]);
    expect(s.page).toBe("root");
  });

  it("opens and closes", () => {
    useCommandPaletteStore.getState().open();
    expect(useCommandPaletteStore.getState().isOpen).toBe(true);
    useCommandPaletteStore.getState().close();
    expect(useCommandPaletteStore.getState().isOpen).toBe(false);
  });

  it("toggles", () => {
    useCommandPaletteStore.getState().toggle();
    expect(useCommandPaletteStore.getState().isOpen).toBe(true);
    useCommandPaletteStore.getState().toggle();
    expect(useCommandPaletteStore.getState().isOpen).toBe(false);
  });

  it("registers a command", () => {
    const cmd: PaletteCommand = { id: "x", title: "X", run: () => {} };
    useCommandPaletteStore.getState().register(cmd);
    expect(useCommandPaletteStore.getState().commands).toEqual([cmd]);
  });

  it("replaces an existing command by id", () => {
    useCommandPaletteStore
      .getState()
      .register({ id: "x", title: "X", run: () => {} });
    useCommandPaletteStore
      .getState()
      .register({ id: "x", title: "X v2", run: () => {} });
    const cmds = useCommandPaletteStore.getState().commands;
    expect(cmds).toHaveLength(1);
    expect(cmds[0]?.title).toBe("X v2");
  });

  it("unregisters by id", () => {
    useCommandPaletteStore
      .getState()
      .register({ id: "x", title: "X", run: () => {} });
    useCommandPaletteStore
      .getState()
      .register({ id: "y", title: "Y", run: () => {} });
    useCommandPaletteStore.getState().unregister("x");
    const cmds = useCommandPaletteStore.getState().commands;
    expect(cmds.map((c) => c.id)).toEqual(["y"]);
  });

  it("navigates pages", () => {
    useCommandPaletteStore.getState().pushPage("subpage");
    expect(useCommandPaletteStore.getState().page).toBe("subpage");
    useCommandPaletteStore.getState().popPage();
    expect(useCommandPaletteStore.getState().page).toBe("root");
    useCommandPaletteStore.getState().pushPage("a");
    useCommandPaletteStore.getState().pushPage("b");
    useCommandPaletteStore.getState().resetPage();
    expect(useCommandPaletteStore.getState().page).toBe("root");
  });

  it("scores exact title highest", () => {
    const cmd: PaletteCommand = { id: "x", title: "Open library", run: () => {} };
    expect(scoreCommand(cmd, "Open library")).toBe(100);
    // Case-insensitive but otherwise exact → strong match, just below case-perfect.
    expect(scoreCommand(cmd, "open library")).toBe(90);
    // Mid-title substring → moderate match.
    expect(scoreCommand(cmd, "library")).toBe(50);
    expect(scoreCommand(cmd, "nope")).toBe(0);
  });

  it("scores subtitles and keywords below titles", () => {
    const cmd: PaletteCommand = {
      id: "x",
      title: "Settings",
      subtitle: "Open preferences",
      keywords: ["preferences"],
      run: () => {},
    };
    expect(scoreCommand(cmd, "preferences")).toBeGreaterThanOrEqual(20);
  });

  it("empty query gives every command a non-zero score", () => {
    const cmd: PaletteCommand = { id: "x", title: "Anything", run: () => {} };
    expect(scoreCommand(cmd, "")).toBe(1);
  });
});
