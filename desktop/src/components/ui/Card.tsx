import { type HTMLAttributes, type ReactNode } from "react";

import { cn } from "@/lib/cn";

export type CardProps = HTMLAttributes<HTMLDivElement> & {
  title?: ReactNode;
  subtitle?: ReactNode;
  actions?: ReactNode;
  bodyClassName?: string;
};

export function Card({
  className,
  title,
  subtitle,
  actions,
  bodyClassName,
  children,
  ...rest
}: CardProps) {
  return (
    <section
      className={cn(
        "rounded-lg bg-surface-raised text-on-surface ring-1 ring-line overflow-hidden",
        className,
      )}
      {...rest}
    >
      {(title || subtitle || actions) && (
        <header className="flex items-start justify-between gap-3 px-4 py-3 border-b border-line">
          <div className="min-w-0">
            {title && (
              <h3 className="text-sm font-semibold leading-tight text-on-surface">
                {title}
              </h3>
            )}
            {subtitle && (
              <p className="text-xs text-on-surface-muted mt-0.5">{subtitle}</p>
            )}
          </div>
          {actions && <div className="flex items-center gap-1">{actions}</div>}
        </header>
      )}
      <div className={cn("p-4", bodyClassName)}>{children}</div>
    </section>
  );
}
