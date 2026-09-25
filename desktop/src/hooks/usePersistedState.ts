import { useCallback, useEffect, useState } from "react";

/**
 * useState that mirrors itself into window.localStorage. Falls back to the
 * in-memory value when storage is unavailable (private mode, SSR).
 */
export function usePersistedState<T>(
  key: string,
  initial: T | (() => T),
): [T, (next: T | ((prev: T) => T)) => void] {
  const read = useCallback((): T => {
    try {
      const raw = window.localStorage.getItem(key);
      if (raw === null) {
        return typeof initial === "function"
          ? (initial as () => T)()
          : initial;
      }
      return JSON.parse(raw) as T;
    } catch {
      return typeof initial === "function"
        ? (initial as () => T)()
        : initial;
    }
  }, [key, initial]);

  const [value, setValue] = useState<T>(read);

  useEffect(() => {
    try {
      window.localStorage.setItem(key, JSON.stringify(value));
    } catch {
      // Quota or no-storage environment — silently ignore.
    }
  }, [key, value]);

  const set = useCallback((next: T | ((prev: T) => T)) => {
    setValue((prev) =>
      typeof next === "function" ? (next as (p: T) => T)(prev) : next,
    );
  }, []);

  return [value, set];
}
