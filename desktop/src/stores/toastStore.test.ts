import { describe, it, expect, beforeEach, vi } from "vitest";

import { useToastStore } from "@/stores/toastStore";

describe("toastStore", () => {
  beforeEach(() => {
    useToastStore.getState().clear();
    vi.useRealTimers();
  });

  it("starts empty", () => {
    expect(useToastStore.getState().toasts).toEqual([]);
  });

  it("pushes a toast and assigns an id", () => {
    const id = useToastStore
      .getState()
      .push({ kind: "info", title: "Hi", ttlMs: 0 });
    const t = useToastStore.getState().toasts[0];
    expect(id).toBeTruthy();
    expect(t?.id).toBe(id);
    expect(t?.kind).toBe("info");
    expect(t?.title).toBe("Hi");
  });

  it("auto-dismisses after ttlMs", () => {
    vi.useFakeTimers();
    useToastStore.getState().push({ kind: "info", title: "Hi", ttlMs: 1000 });
    expect(useToastStore.getState().toasts).toHaveLength(1);
    vi.advanceTimersByTime(1000);
    expect(useToastStore.getState().toasts).toHaveLength(0);
  });

  it("does not auto-dismiss when ttlMs is 0", () => {
    vi.useFakeTimers();
    useToastStore.getState().push({ kind: "info", title: "Sticky", ttlMs: 0 });
    vi.advanceTimersByTime(60_000);
    expect(useToastStore.getState().toasts).toHaveLength(1);
  });

  it("dismisses by id", () => {
    const id = useToastStore
      .getState()
      .push({ kind: "warning", title: "Hi", ttlMs: 0 });
    useToastStore.getState().dismiss(id);
    expect(useToastStore.getState().toasts).toHaveLength(0);
  });
});
