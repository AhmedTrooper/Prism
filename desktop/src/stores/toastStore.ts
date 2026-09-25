import { create } from "zustand";

export type ToastKind = "info" | "success" | "warning" | "error";

export type Toast = {
  id: string;
  kind: ToastKind;
  title: string;
  body?: string;
  /** Milliseconds before auto-dismiss. 0 means sticky. */
  ttlMs: number;
  createdAt: number;
};

type ToastState = {
  toasts: Toast[];
  push: (t: Omit<Toast, "id" | "createdAt"> & { id?: string }) => string;
  dismiss: (id: string) => void;
  clear: () => void;
};

let counter = 0;
function nextId(): string {
  counter += 1;
  return `t_${Date.now().toString(36)}_${counter}`;
}

/**
 * Cross-feature toast store. Any feature can `push()` a toast; the UI
 * subscribes and renders them.
 */
export const useToastStore = create<ToastState>((set, get) => ({
  toasts: [],
  push: (t) => {
    const id = t.id ?? nextId();
    const toast: Toast = {
      id,
      kind: t.kind,
      title: t.title,
      body: t.body,
      ttlMs: t.ttlMs,
      createdAt: Date.now(),
    };
    set((s) => ({ toasts: [...s.toasts, toast] }));
    if (toast.ttlMs > 0) {
      window.setTimeout(() => get().dismiss(id), toast.ttlMs);
    }
    return id;
  },
  dismiss: (id) =>
    set((s) => ({ toasts: s.toasts.filter((t) => t.id !== id) })),
  clear: () => set({ toasts: [] }),
}));
