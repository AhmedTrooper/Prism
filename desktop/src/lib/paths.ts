/**
 * Path and URL helpers.
 * Centralized here so features don't reinvent basename/extension logic.
 */

export function basename(path: string): string {
  // Strip any trailing separator(s) before extracting the last segment so
  // `/home/me/` resolves to `me` rather than empty.
  const trimmed = path.replace(/[\\/]+$/, "");
  const ix = Math.max(trimmed.lastIndexOf("/"), trimmed.lastIndexOf("\\"));
  return ix < 0 ? trimmed : trimmed.slice(ix + 1);
}

export function dirname(path: string): string {
  // POSIX convention: a path with no separators names the current
  // directory (`.`). Path with trailing separators peels the empty
  // trailing segment first.
  const trimmed = path.replace(/[\\/]+$/, "");
  const ix = Math.max(trimmed.lastIndexOf("/"), trimmed.lastIndexOf("\\"));
  if (ix < 0) return ".";
  if (ix === 0) return "/";
  return trimmed.slice(0, ix);
}

export function extname(path: string): string {
  const base = basename(path);
  const dot = base.lastIndexOf(".");
  return dot <= 0 ? "" : base.slice(dot);
}

/** Extension without the dot, preserving case. Empty string if none. */
export function ext(path: string): string {
  return extname(path).slice(1);
}

/** True for common video file extensions. */
export function isVideoPath(path: string): boolean {
  return VIDEO_EXTS.has(ext(path).toLowerCase());
}

/** True for common audio file extensions. */
export function isAudioPath(path: string): boolean {
  return AUDIO_EXTS.has(ext(path).toLowerCase());
}

/** True for common subtitle file extensions. */
export function isSubtitlePath(path: string): boolean {
  return SUBTITLE_EXTS.has(ext(path).toLowerCase());
}

export const VIDEO_EXTS = new Set([
  "mp4",
  "mkv",
  "webm",
  "avi",
  "mov",
  "ts",
  "m2ts",
  "flv",
  "wmv",
  "mpg",
  "mpeg",
  "m4v",
  "3gp",
  "ogv",
]);

export const AUDIO_EXTS = new Set([
  "mp3",
  "flac",
  "m4a",
  "aac",
  "ogg",
  "opus",
  "wav",
  "wma",
  "ac3",
  "eac3",
  "dts",
]);

export const SUBTITLE_EXTS = new Set(["srt", "ass", "ssa", "vtt", "sub"]);
