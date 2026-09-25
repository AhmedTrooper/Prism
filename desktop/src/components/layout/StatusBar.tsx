import { type ReactNode } from "react";

import { cn } from "@/lib/cn";

export type StatusItem = {
  id: string;
  label: ReactNode;
  tone?: "default" | "success" | "warning" | "danger" | "info";
};

export type StatusBarProps = {
  items: StatusItem[];
  className?: string;
};

const toneClasses = {
  default: "text-on-surface-muted",
  success: "text-status-success",
  warning: "text-status-warning",
  danger: "text-status-danger",
  info: "text-prism-primary",
} as const;

export function StatusBar({ items, className }: StatusBarProps) {
  return (
    <div
      role="status"
      aria-live="polite"
      className={cn(
        "flex h-7 items-center gap-4 px-3 text-[11px]",
        className,
      )}
    >
      {items.map((it, i) => (
        <div key={it.id} className="flex items-center gap-1.5">
          {i > 0 && (
            <span aria-hidden className="h-3 w-px bg-line" />
          )}
          <span className={cn("font-medium", toneClasses[it.tone ?? "default"])}>
            {it.label}
          </span>
        </div>
      ))}
    </div>
  );
}
