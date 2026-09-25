import { useCallback, useRef, useState } from "react";

/**
 * Track the size of a DOM element with ResizeObserver. Returns the latest
 * content-box size, in CSS pixels.
 *
 *   const { ref, size } = useResizeObserver<HTMLDivElement>();
 *   return <div ref={ref}>{size.width}×{size.height}</div>;
 */
export function useResizeObserver<T extends Element>(): {
  ref: (el: T | null) => void;
  size: { width: number; height: number };
} {
  const [size, setSize] = useState({ width: 0, height: 0 });
  const observerRef = useRef<ResizeObserver | null>(null);

  const ref = useCallback((el: T | null) => {
    if (observerRef.current) {
      observerRef.current.disconnect();
      observerRef.current = null;
    }
    if (!el) return;
    const ro = new ResizeObserver((entries) => {
      const entry = entries[0];
      if (!entry) return;
      const { width, height } = entry.contentRect;
      setSize({ width, height });
    });
    ro.observe(el);
    // Seed from the current rect synchronously when the ref is attached.
    const rect = el.getBoundingClientRect();
    setSize({ width: rect.width, height: rect.height });
    observerRef.current = ro;
  }, []);

  return { ref, size };
}
