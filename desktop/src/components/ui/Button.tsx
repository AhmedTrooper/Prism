import { forwardRef, type ButtonHTMLAttributes } from "react";

import { cn } from "@/lib/cn";
import {
  interactiveSurfaceClasses,
  sizeClasses,
  type Size,
  type SurfaceTone,
} from "./variants";

export type ButtonVariant = "primary" | "secondary" | "ghost" | "danger";

export type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: ButtonVariant;
  size?: Size;
  tone?: SurfaceTone;
  fullWidth?: boolean;
  loading?: boolean;
};

const variantClasses: Record<ButtonVariant, string> = {
  primary: "bg-prism-primary text-prism-on-primary hover:bg-prism-primary-dark",
  secondary:
    "bg-surface-raised text-on-surface ring-1 ring-inset ring-line hover:bg-surface",
  ghost: "bg-transparent text-on-surface hover:bg-surface-raised",
  danger: "bg-status-danger text-on-status-danger hover:opacity-90",
};

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  function Button(
    {
      className,
      variant = "secondary",
      size = "md",
      tone = "default",
      fullWidth = false,
      loading = false,
      disabled,
      type = "button",
      children,
      ...rest
    },
    ref,
  ) {
    return (
      <button
        ref={ref}
        type={type}
        disabled={disabled || loading}
        className={cn(
          "inline-flex items-center justify-center rounded-md font-medium",
          "transition-colors select-none",
          "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-prism-primary focus-visible:ring-offset-2 focus-visible:ring-offset-surface",
          "disabled:opacity-50 disabled:cursor-not-allowed",
          sizeClasses[size],
          variantClasses[variant],
          interactiveSurfaceClasses(tone, false),
          fullWidth && "w-full",
          className,
        )}
        {...rest}
      >
        {loading ? <ButtonSpinner size={size} /> : children}
      </button>
    );
  },
);

function ButtonSpinner({ size }: { size: Size }) {
  const dim = size === "lg" ? "h-4 w-4" : size === "md" ? "h-3.5 w-3.5" : "h-3 w-3";
  return (
    <span
      aria-hidden
      className={cn(
        "inline-block animate-spin rounded-full border-2 border-current border-t-transparent",
        dim,
      )}
    />
  );
}
