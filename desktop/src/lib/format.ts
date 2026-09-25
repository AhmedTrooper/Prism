/**
 * Display formatters used across player UI: time, byte size, duration.
 * Pure functions, no DOM — safe to call from any feature.
 */

/** Format a number of seconds as `H:MM:SS` or `M:SS`. */
export function formatDuration(totalSeconds: number): string {
  if (!Number.isFinite(totalSeconds) || totalSeconds < 0) return "0:00";
  const s = Math.floor(totalSeconds);
  const hours = Math.floor(s / 3600);
  const minutes = Math.floor((s % 3600) / 60);
  const seconds = s % 60;
  const pad = (n: number) => n.toString().padStart(2, "0");
  if (hours > 0) return `${hours}:${pad(minutes)}:${pad(seconds)}`;
  return `${minutes}:${pad(seconds)}`;
}

/** Format a byte count as a human-readable size (`1.4 MB`, `812 KB`, …). */
export function formatBytes(bytes: number): string {
  if (!Number.isFinite(bytes) || bytes < 0) return "0 B";
  const units = ["B", "KB", "MB", "GB", "TB"];
  let value = bytes;
  let unit = 0;
  while (value >= 1024 && unit < units.length - 1) {
    value /= 1024;
    unit++;
  }
  // Bytes stay integer. Larger units render with one decimal.
  if (unit === 0) return `${Math.round(value)} ${units[unit]}`;
  const rounded = Number(value.toFixed(1));
  return `${rounded.toFixed(1)} ${units[unit]}`;
}

/**
 * Format an epoch-ms timestamp as a short relative string
 * (`just now`, `5m ago`, `3h ago`, `2d ago`). Falls back to a locale
 * date string for anything older than 7 days.
 */
export function formatRelativeDate(epochMs: number, now: number = Date.now()): string {
  const diff = now - epochMs;
  if (diff < 60_000) return "just now";
  const minutes = Math.floor(diff / 60_000);
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.floor(diff / 3_600_000);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.floor(diff / 86_400_000);
  if (days < 30) return `${days}d ago`;
  return new Date(epochMs).toLocaleDateString();
}

/**
 * Format a signed seek delta as `+5s`, `-30s`, `+1m 30s`, etc. Used by
 * the player HUD when the user taps forward/back.
 */
export function formatSeekDelta(seconds: number): string {
  const sign = seconds >= 0 ? "+" : "-";
  const abs = Math.abs(Math.floor(seconds));
  const m = Math.floor(abs / 60);
  const s = abs % 60;
  if (m === 0) return `${sign}${s}s`;
  if (s === 0) return `${sign}${m}m`;
  return `${sign}${m}m ${s}s`;
}
