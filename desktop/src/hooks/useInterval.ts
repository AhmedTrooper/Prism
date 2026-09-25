import { useEffect, useRef } from "react";

/**
 * Run `fn` every `delayMs`. Pause by passing `null`.
 *
 *   useInterval(() => ping(), isPlaying ? 1000 : null);
 */
export function useInterval(fn: () => void, delayMs: number | null): void {
  const saved = useRef(fn);
  useEffect(() => {
    saved.current = fn;
  }, [fn]);

  useEffect(() => {
    if (delayMs === null) return;
    const id = window.setInterval(() => saved.current(), delayMs);
    return () => window.clearInterval(id);
  }, [delayMs]);
}
