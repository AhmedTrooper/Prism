/**
 * IPC command & event type registry.
 *
 * Every Tauri command is declared here with its argument type and return
 * type. The runtime wrapper (`ipc/commands.ts`) reads this registry so
 * callers get full autocomplete and type-checking on `invokeCommand(...)`.
 *
 * Mirror this registry on the Rust side in `src-tauri/src/commands/`.
 */

import type { z } from "zod";

/**
 * A single command row.
 *  - `args` is the shape of the parameter object passed to `invoke`
 *  - `argsSchema` is a zod schema for runtime validation (optional)
 *  - `result` is the shape of the value returned from Rust
 *  - `resultSchema` is a zod schema for runtime validation (optional)
 */
export type CommandDef<A, R> = {
  args: A;
  result: R;
  argsSchema?: z.ZodType<A>;
  resultSchema?: z.ZodType<R>;
};

/** Registry. Add new commands by extending this interface. */
export interface IpcCommands {
  greet: CommandDef<{ name: string }, string>;
  ping: CommandDef<Record<string, never>, { ok: true; ts: number }>;
  app_info: CommandDef<Record<string, never>, AppInfo>;
}

/** Registry for events emitted by Rust (`app.emit(...)`). */
export interface IpcEvents {
  "library:scanned": LibraryScannedPayload;
  "player:track-changed": TrackChangedPayload;
  "player:position": PositionPayload;
  "player:eof": Record<string, never>;
}

export interface AppInfo {
  name: string;
  version: string;
  buildType: string;
  platform: "windows" | "macos" | "linux";
}

export interface LibraryScannedPayload {
  folders: number;
  videos: number;
  durationMs: number;
}

export interface TrackChangedPayload {
  trackType: "audio" | "sub" | "video";
  mpvId: number;
  title?: string;
  language?: string;
}

export interface PositionPayload {
  positionSec: number;
  durationSec: number;
  paused: boolean;
}

export type IpcEventName = keyof IpcEvents;
export type IpcCommandName = keyof IpcCommands;
