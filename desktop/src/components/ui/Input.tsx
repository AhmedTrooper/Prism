import { forwardRef, type InputHTMLAttributes } from "react";

import { cn } from "@/lib/cn";

export type InputProps = InputHTMLAttributes<HTMLInputElement> & {
  invalid?: boolean;
};

export const Input = forwardRef<HTMLInputElement, InputProps>(function Input(
  { className, invalid, type = "text", ...rest },
  ref,
) {
  return (
    <input
      ref={ref}
      type={type}
      aria-invalid={invalid || undefined}
      className={cn(
        "h-9 w-full rounded-md bg-surface text-on-surface px-3 text-sm",
        "ring-1 ring-inset ring-line placeholder:text-on-surface-muted",
        "focus:outline-none focus:ring-2 focus:ring-prism-primary",
        "disabled:opacity-50 disabled:cursor-not-allowed",
        invalid && "ring-status-danger focus:ring-status-danger",
        className,
      )}
      {...rest}
    />
  );
});
