import { useEffect, type ReactNode } from "react";
import { createPortal } from "react-dom";

import { cn } from "@/lib/cn";
import { useShortcut } from "@/hooks/useShortcut";
import { IconButton } from "./IconButton";

export type DialogProps = {
  open: boolean;
  onClose: () => void;
  title?: ReactNode;
  description?: ReactNode;
  /** Footer area for actions. */
  footer?: ReactNode;
  size?: "sm" | "md" | "lg" | "xl";
  className?: string;
  children?: ReactNode;
};

const sizeClasses = {
  sm: "max-w-sm",
  md: "max-w-md",
  lg: "max-w-2xl",
  xl: "max-w-4xl",
} as const;

/**
 * Modal dialog. Closes on Escape and on backdrop click. Renders into a
 * portal so it sits above all app chrome.
 */
export function Dialog({
  open,
  onClose,
  title,
  description,
  footer,
  size = "md",
  className,
  children,
}: DialogProps) {
  useShortcut("Escape", () => {
    if (open) onClose();
  });

  useEffect(() => {
    if (!open) return;
    const prev = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      document.body.style.overflow = prev;
    };
  }, [open]);

  if (!open) return null;
  if (typeof document === "undefined") return null;

  return createPortal(
    <div
      role="presentation"
      className="fixed inset-0 z-50 flex items-center justify-center p-6"
      onMouseDown={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div className="absolute inset-0 bg-surface-inverse/40 backdrop-blur-sm" />
      <div
        role="dialog"
        aria-modal="true"
        aria-label={typeof title === "string" ? title : undefined}
        className={cn(
          "relative z-10 w-full rounded-xl bg-surface text-on-surface shadow-xl ring-1 ring-line",
          "flex flex-col max-h-[80vh]",
          sizeClasses[size],
          className,
        )}
      >
        {(title || description) && (
          <header className="flex items-start justify-between gap-3 px-5 py-4 border-b border-line">
            <div className="min-w-0">
              {title && (
                <h2 className="text-base font-semibold leading-tight">
                  {title}
                </h2>
              )}
              {description && (
                <p className="text-sm text-on-surface-muted mt-1">
                  {description}
                </p>
              )}
            </div>
            <IconButton
              aria-hidden
              tabIndex={-1}
              label="Close"
              variant="ghost"
              size="sm"
              onClick={onClose}
              icon={<CloseGlyph />}
            />
          </header>
        )}
        <div className="flex-1 overflow-auto px-5 py-4">{children}</div>
        {footer && (
          <footer className="flex items-center justify-end gap-2 px-5 py-3 border-t border-line bg-surface-sunken">
            {footer}
          </footer>
        )}
      </div>
    </div>,
    document.body,
  );
}

function CloseGlyph() {
  return (
    <svg
      viewBox="0 0 16 16"
      width="16"
      height="16"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.5"
      strokeLinecap="round"
    >
      <path d="M3 3 L13 13 M13 3 L3 13" />
    </svg>
  );
}
