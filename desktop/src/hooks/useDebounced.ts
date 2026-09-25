import { useEffect, useState } from "react";

/**
 * Debounce a value so it only updates after `delayMs` of stability.
 * Useful for slider thumbs, search boxes, anything that would otherwise
 * spam IPC during interaction.
 */
export function useDebounced<T>(value: T, delayMs: number): T {
  const [debounced, setDebounced] = useState(value);
  useEffect(() => {
    const id = window.setTimeout(() => setDebounced(value), delayMs);
    return () => window.clearTimeout(id);
  }, [value, delayMs]);
  return debounced;
}
