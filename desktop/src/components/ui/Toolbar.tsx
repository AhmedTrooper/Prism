import { type HTMLAttributes, type ReactNode } from "react";

import { cn } from "@/lib/cn";

export type ToolbarProps = HTMLAttributes<HTMLDivElement> & {
  children?: ReactNode;
};

export function Toolbar({ className, children, ...rest }: ToolbarProps) {
  return (
    <div
      role="toolbar"
      className={cn(
        "flex items-center gap-2 px-3 h-11 border-b border-line bg-surface text-on-surface",
        className,
      )}
      {...rest}
    >
      {children}
    </div>
  );
}

export function ToolbarDivider() {
  return (
    <span
      aria-hidden
      className="inline-block h-6 w-px bg-line mx-1"
    />
  );
}

export function ToolbarSpacer() {
  return <span className="flex-1" />;
}

export function ToolbarGroup({ children }: { children: ReactNode }) {
  return <div className="flex items-center gap-1">{children}</div>;
}
