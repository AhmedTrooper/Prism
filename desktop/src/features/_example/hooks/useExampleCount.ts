import { useMemo } from "react";

import { useExampleStore } from "../stores/exampleStore";

/**
 * Derived hook. Returns live count of items in the example store.
 * Memoized so it only changes when items.length changes.
 */
export function useExampleCount(): number {
  const items = useExampleStore((s) => s.items);
  return useMemo(() => items.length, [items.length]);
}
