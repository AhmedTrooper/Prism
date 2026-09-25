/**
 * Tailwind class combinations shared by UI primitives.
 * Keeps `className`-shaped APIs ergonomic without pulling in cva.
 */

import { cn } from "@/lib/cn";

export type SurfaceTone = "default" | "raised" | "sunken" | "inverse";

export const surfaceClasses: Record<SurfaceTone, string> = {
  default: "bg-surface text-on-surface",
  raised: "bg-surface-raised text-on-surface",
  sunken: "bg-surface-sunken text-on-surface",
  inverse: "bg-surface-inverse text-on-surface-inverse",
};

export const interactiveSurfaceClasses = (
  tone: SurfaceTone,
  hover: boolean,
): string =>
  cn(
    surfaceClasses[tone],
    hover &&
      "transition-colors hover:bg-surface-raised active:bg-surface-sunken",
  );

export type Size = "xs" | "sm" | "md" | "lg";

export const sizeClasses: Record<Size, string> = {
  xs: "h-6 px-2 text-xs gap-1",
  sm: "h-8 px-3 text-sm gap-1.5",
  md: "h-10 px-4 text-sm gap-2",
  lg: "h-12 px-5 text-base gap-2.5",
};

export const iconSizeClasses: Record<Size, string> = {
  xs: "h-6 w-6",
  sm: "h-8 w-8",
  md: "h-10 w-10",
  lg: "h-12 w-12",
};
