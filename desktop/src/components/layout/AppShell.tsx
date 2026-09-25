import { type ReactNode } from "react";

import { cn } from "@/lib/cn";

export type AppShellProps = {
  sidebar?: ReactNode;
  toolbar?: ReactNode;
  statusBar?: ReactNode;
  children: ReactNode;
};

/**
 * Top-level grid: window-chrome, sidebar, toolbar, main, statusbar.
 * Layout reflows responsively when the sidebar collapses.
 */
export function AppShell({ sidebar, toolbar, statusBar, children }: AppShellProps) {
  return (
    <div
      className={cn(
        "h-full w-full grid bg-surface text-on-surface",
        // The grid below mirrors the data-sidebar-collapsed attribute toggled
        // by the windowStore. Default = sidebar expanded.
        "grid-cols-[260px_1fr] grid-rows-[auto_1fr_auto]",
        "[&[data-sidebar-collapsed]]:grid-cols-[56px_1fr]",
      )}
    >
      {sidebar && (
        <aside className="row-span-3 border-r border-line bg-surface-sunken overflow-y-auto">
          {sidebar}
        </aside>
      )}
      <div className="col-start-2 flex min-h-0 flex-col">
        {toolbar && (
          <div className="border-b border-line bg-surface">{toolbar}</div>
        )}
        <main className="min-h-0 flex-1 overflow-auto bg-surface">
          {children}
        </main>
        {statusBar && (
          <div className="border-t border-line bg-surface-sunken text-xs">
            {statusBar}
          </div>
        )}
      </div>
    </div>
  );
}
