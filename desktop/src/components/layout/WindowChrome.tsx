import { type ReactNode } from "react";

import { cn } from "@/lib/cn";

export type WindowChromeProps = {
  title: ReactNode;
  className?: string;
  controls?: ReactNode;
};

/**
 * Draggable titlebar strip. Renders above the toolbar. On platforms
 * where we use the native chrome, this slot is simply not present.
 */
export function WindowChrome({ title, className, controls }: WindowChromeProps) {
  return (
    <div
      data-tauri-drag-region
      className={cn(
        "flex h-9 items-center gap-2 border-b border-line bg-surface-sunken px-3 text-xs text-on-surface-muted",
        className,
      )}
    >
      <span data-tauri-drag-region className="flex-1 truncate font-medium">
        {title}
      </span>
      {controls}
    </div>
  );
}
