import { type HTMLAttributes, type ReactNode } from "react";

import { cn } from "@/lib/cn";

export type BadgeTone =
  | "neutral"
  | "primary"
  | "success"
  | "warning"
  | "danger";

export type BadgeProps = HTMLAttributes<HTMLSpanElement> & {
  tone?: BadgeTone;
  children?: ReactNode;
};

const toneClasses: Record<BadgeTone, string> = {
  neutral: "bg-surface-sunken text-on-surface-muted ring-line",
  primary: "bg-prism-primary/15 text-prism-primary ring-prism-primary/30",
  success: "bg-status-success/15 text-status-success ring-status-success/30",
  warning: "bg-status-warning/15 text-status-warning ring-status-warning/30",
  danger: "bg-status-danger/15 text-status-danger ring-status-danger/30",
};

export function Badge({
  className,
  tone = "neutral",
  children,
  ...rest
}: BadgeProps) {
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1 rounded-full px-2 h-5 text-[11px] font-medium ring-1 ring-inset",
        toneClasses[tone],
        className,
      )}
      {...rest}
    >
      {children}
    </span>
  );
}
