import { type ReactNode, useEffect, useRef, useState } from "react";

import { cn } from "@/lib/cn";

export type TooltipProps = {
  label: ReactNode;
  side?: "top" | "right" | "bottom" | "left";
  delayMs?: number;
  className?: string;
  children: ReactElement;
};

import type { ReactElement } from "react";

const sideClasses = {
  top: "bottom-full left-1/2 -translate-x-1/2 mb-1.5",
  right: "left-full top-1/2 -translate-y-1/2 ml-1.5",
  bottom: "top-full left-1/2 -translate-x-1/2 mt-1.5",
  left: "right-full top-1/2 -translate-y-1/2 mr-1.5",
} as const;

/**
 * Lightweight tooltip. Wraps a single child, shows the label on hover
 * after `delayMs`. We use a native `<span>` wrapper so the consumer can
 * keep its own element type intact.
 */
export function Tooltip({
  label,
  side = "top",
  delayMs = 350,
  className,
  children,
}: TooltipProps) {
  const [open, setOpen] = useState(false);
  const timer = useRef<number | null>(null);

  useEffect(() => {
    return () => {
      if (timer.current !== null) window.clearTimeout(timer.current);
    };
  }, []);

  return (
    <span
      className={cn("relative inline-flex", className)}
      onMouseEnter={() => {
        if (timer.current !== null) window.clearTimeout(timer.current);
        timer.current = window.setTimeout(() => setOpen(true), delayMs);
      }}
      onMouseLeave={() => {
        if (timer.current !== null) window.clearTimeout(timer.current);
        setOpen(false);
      }}
      onFocus={() => setOpen(true)}
      onBlur={() => setOpen(false)}
    >
      {children}
      {open && (
        <span
          role="tooltip"
          className={cn(
            "pointer-events-none absolute z-50 rounded-md bg-surface-inverse text-on-surface-inverse",
            "px-2 py-1 text-[11px] font-medium whitespace-nowrap shadow-md",
            sideClasses[side],
          )}
        >
          {label}
        </span>
      )}
    </span>
  );
}
