import { useToastStore, type Toast as ToastT } from "@/stores/toastStore";
import { cn } from "@/lib/cn";
import { IconButton } from "./IconButton";

const toneStyles: Record<ToastT["kind"], string> = {
  info: "ring-prism-primary/40 bg-prism-primary/10 text-prism-primary",
  success: "ring-status-success/40 bg-status-success/10 text-status-success",
  warning: "ring-status-warning/40 bg-status-warning/10 text-status-warning",
  error: "ring-status-danger/40 bg-status-danger/10 text-status-danger",
};

const toneGlyph: Record<ToastT["kind"], string> = {
  info: "i",
  success: "✓",
  warning: "!",
  error: "×",
};

/**
 * Toast layer. Place once at the app root; reads from `useToastStore`.
 */
export function Toaster() {
  const toasts = useToastStore((s) => s.toasts);
  const dismiss = useToastStore((s) => s.dismiss);
  return (
    <div className="pointer-events-none fixed bottom-4 right-4 z-50 flex w-80 flex-col gap-2">
      {toasts.map((t) => (
        <ToastItem key={t.id} toast={t} onDismiss={() => dismiss(t.id)} />
      ))}
    </div>
  );
}

function ToastItem({
  toast,
  onDismiss,
}: {
  toast: ToastT;
  onDismiss: () => void;
}) {
  return (
    <div
      role="status"
      className={cn(
        "pointer-events-auto rounded-lg bg-surface text-on-surface ring-1 shadow-lg overflow-hidden",
        toneStyles[toast.kind],
      )}
    >
      <div className="flex items-start gap-3 p-3">
        <span
          aria-hidden
          className={cn(
            "mt-0.5 inline-flex h-5 w-5 items-center justify-center rounded-full text-xs font-bold",
            toneStyles[toast.kind],
          )}
        >
          {toneGlyph[toast.kind]}
        </span>
        <div className="min-w-0 flex-1">
          <div className="text-sm font-semibold leading-tight">
            {toast.title}
          </div>
          {toast.body && (
            <div className="mt-1 text-xs text-on-surface-muted">{toast.body}</div>
          )}
        </div>
        <IconButton
          label="Dismiss"
          variant="ghost"
          size="sm"
          onClick={onDismiss}
          icon={<ToastCloseGlyph />}
        />
      </div>
    </div>
  );
}

function ToastCloseGlyph() {
  return (
    <svg
      viewBox="0 0 16 16"
      width="14"
      height="14"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.5"
      strokeLinecap="round"
    >
      <path d="M3 3 L13 13 M13 3 L3 13" />
    </svg>
  );
}
