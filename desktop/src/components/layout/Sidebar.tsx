import { type ReactNode } from "react";

import { cn } from "@/lib/cn";

export type SidebarItem = {
  id: string;
  label: string;
  icon?: ReactNode;
  badge?: ReactNode;
  active?: boolean;
  onSelect?: () => void;
};

export type SidebarProps = {
  brand?: ReactNode;
  items: SidebarItem[];
  footer?: ReactNode;
};

export function Sidebar({ brand, items, footer }: SidebarProps) {
  return (
    <nav
      aria-label="Primary"
      className="flex h-full flex-col px-2 py-3 text-on-surface"
    >
      {brand && (
        <div className="mb-3 px-2 text-sm font-semibold tracking-wide">
          {brand}
        </div>
      )}
      <ul className="flex flex-1 flex-col gap-0.5">
        {items.map((item) => (
          <li key={item.id}>
            <button
              type="button"
              onClick={item.onSelect}
              aria-current={item.active ? "page" : undefined}
              className={cn(
                "group flex w-full items-center gap-3 rounded-md px-2.5 h-9 text-sm",
                "transition-colors hover:bg-surface-raised",
                "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-prism-primary",
                item.active &&
                  "bg-prism-primary/15 text-prism-primary hover:bg-prism-primary/20",
              )}
            >
              {item.icon && (
                <span
                  aria-hidden
                  className="inline-flex h-5 w-5 items-center justify-center text-on-surface-muted group-aria-[current=page]:text-prism-primary"
                >
                  {item.icon}
                </span>
              )}
              <span className="flex-1 truncate text-left">{item.label}</span>
              {item.badge && (
                <span className="ml-auto text-[11px] text-on-surface-muted">
                  {item.badge}
                </span>
              )}
            </button>
          </li>
        ))}
      </ul>
      {footer && (
        <div className="mt-2 border-t border-line pt-2 px-1">{footer}</div>
      )}
    </nav>
  );
}
