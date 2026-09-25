import { forwardRef, type ButtonHTMLAttributes, type ReactNode } from "react";

import { cn } from "@/lib/cn";
import {
  iconSizeClasses,
  type Size,
} from "./variants";

export type IconButtonVariant = "primary" | "secondary" | "ghost" | "danger";

export type IconButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  icon: ReactNode;
  label: string;
  variant?: IconButtonVariant;
  size?: Size;
  pressed?: boolean;
};

const variantClasses: Record<IconButtonVariant, string> = {
  primary: "bg-prism-primary text-prism-on-primary hover:bg-prism-primary-dark",
  secondary:
    "bg-surface-raised text-on-surface ring-1 ring-inset ring-line hover:bg-surface",
  ghost: "bg-transparent text-on-surface hover:bg-surface-raised",
  danger: "bg-status-danger text-on-status-danger hover:opacity-90",
};

export const IconButton = forwardRef<HTMLButtonElement, IconButtonProps>(
  function IconButton(
    {
      className,
      icon,
      label,
      variant = "ghost",
      size = "md",
      pressed = false,
      disabled,
      type = "button",
      ...rest
    },
    ref,
  ) {
    return (
      <button
        ref={ref}
        type={type}
        aria-label={label}
        title={label}
        aria-pressed={pressed}
        disabled={disabled}
        className={cn(
          "inline-flex items-center justify-center rounded-md transition-colors select-none",
          "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-prism-primary focus-visible:ring-offset-2 focus-visible:ring-offset-surface",
          "disabled:opacity-50 disabled:cursor-not-allowed",
          iconSizeClasses[size],
          variantClasses[variant],
          pressed && "bg-prism-primary/15 text-prism-primary",
          className,
        )}
        {...rest}
      >
        {icon}
      </button>
    );
  },
);
