/**
 * IPC runtime helpers. Generic, no command names here.
 * Concrete command wrappers live in `ipc/commands.ts` and event wrappers
 * in `ipc/events.ts`.
 */

import { err, ok, type Result } from "@/lib/result";
import type { z } from "zod";

/**
 * Run a zod schema and return either the parsed value or a Result error.
 * Used by the typed wrappers below to validate payloads at the IPC boundary.
 */
export function parseOrFail<T>(
  schema: z.ZodType<T> | undefined,
  data: unknown,
  label: string,
): Result<T, Error> {
  if (!schema) return ok(data as T);
  const parsed = schema.safeParse(data);
  if (parsed.success) return ok(parsed.data);
  return err(new Error(`[ipc] ${label} failed schema check: ${parsed.error.message}`));
}
