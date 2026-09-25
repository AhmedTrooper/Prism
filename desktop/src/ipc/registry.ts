/**
 * IPC command + event registry.
 *
 *   Each feature contributes its own commands. Importing the feature here
 *   side-effect-installs its schemas so the runtime wrapper can validate.
 *
 *   At build time, types are pulled from `ipc/types.ts` (which declares
 *   shape-only contracts); zod schemas are wired in below.
 */

import type { IpcCommandName, IpcCommands } from "./types";
import { greetArgsSchema, greetResultSchema } from "./schemas/greet";
import {
  pingResultSchema,
  appInfoArgsSchema,
  appInfoResultSchema,
} from "./schemas/app";

/**
 * Runtime registry — maps a command name to its declared args/result
 * plus optional zod schemas for runtime validation.
 *
 *   const r = invokeCommand("greet", { name: "x" });
 *
 * If a command is added to `IpcCommands` in `./types.ts`, adding it here
 * is required so the runtime side stays accurate.
 */
type RegistryRow<K extends IpcCommandName = IpcCommandName> = {
  [P in K]: IpcCommands[P] & {
    argsSchema?: unknown;
    resultSchema?: unknown;
  };
};

export const registry = {
  greet: {
    args: { name: "" } as IpcCommands["greet"]["args"],
    result: "" as unknown as IpcCommands["greet"]["result"],
    argsSchema: greetArgsSchema,
    resultSchema: greetResultSchema,
  },
  ping: {
    args: {} as IpcCommands["ping"]["args"],
    result: { ok: true, ts: 0 } as unknown as IpcCommands["ping"]["result"],
    resultSchema: pingResultSchema,
  },
  app_info: {
    args: {} as IpcCommands["app_info"]["args"],
    result: {
      name: "",
      version: "",
      buildType: "",
      platform: "linux",
    } as unknown as IpcCommands["app_info"]["result"],
    argsSchema: appInfoArgsSchema,
    resultSchema: appInfoResultSchema,
  },
} satisfies RegistryRow;
