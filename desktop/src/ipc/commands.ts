/**
 * Typed Tauri command wrappers.
 *
 *   const result = await invokeCommand("greet", { name: "World" });
 *
 * The compiler enforces that the second argument matches the command's
 * declared args shape, and the return value is the declared result shape.
 * Optional zod schemas validate both ends at runtime.
 */

import { invoke } from "@tauri-apps/api/core";
import type { ZodType } from "zod";

import type { Result } from "@/lib/result";
import { tryAsync } from "@/lib/result";
import { parseOrFail } from "./runtime";
import type { IpcCommandName, IpcCommands } from "./types";
import { registry } from "./registry";

/**
 * Strictly-typed wrapper around `invoke`.
 *
 * Optional zod schemas activate runtime validation. Disable with
 * `{ validate: false }` for hot paths where the schema is too costly.
 */
export async function invokeCommand<K extends IpcCommandName>(
  name: K,
  args: IpcCommands[K]["args"],
  opts: { validate?: boolean } = {},
): Promise<Result<IpcCommands[K]["result"], Error>> {
  const validate = opts.validate ?? true;
  const def = registry[name] as IpcCommands[K] & {
    argsSchema?: unknown;
    resultSchema?: unknown;
  };

  // Args-side validation
  if (validate && def.argsSchema) {
    const checked = parseOrFail(
      def.argsSchema as ZodType<unknown>,
      args as unknown,
      `args of ${name}`,
    );
    if (!checked.ok) return checked;
  }

  // Invoke the command. We drop the precise overload return type to
  // avoid the union cascading through `tryAsync`; the runtime registry
  // (and result-side schema) gives us the real shape.
  const exec = await tryAsync<unknown>(() =>
    invoke(name, args as unknown as Record<string, unknown>),
  );
  if (!exec.ok) return exec as unknown as Result<IpcCommands[K]["result"], Error>;

  // Result-side validation
  if (validate && def.resultSchema) {
    const parsed = parseOrFail(
      def.resultSchema as ZodType<unknown>,
      exec.value,
      `result of ${name}`,
    );
    if (!parsed.ok) return parsed as unknown as Result<IpcCommands[K]["result"], Error>;
    return parsed as unknown as Result<IpcCommands[K]["result"], Error>;
  }
  return { ok: true, value: exec.value } as Result<IpcCommands[K]["result"], Error>;
}
