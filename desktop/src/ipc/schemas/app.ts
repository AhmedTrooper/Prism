import { z } from "zod";

export const pingResultSchema = z.object({
  ok: z.literal(true),
  ts: z.number().int().nonnegative(),
});

export const appPlatformSchema = z.enum(["windows", "macos", "linux"]);

export const appInfoArgsSchema = z.object({}).strict();

export const appInfoResultSchema = z.object({
  name: z.string().min(1),
  version: z.string().min(1),
  buildType: z.string().min(1),
  platform: appPlatformSchema,
});

export const appInfoSchema = appInfoResultSchema;
