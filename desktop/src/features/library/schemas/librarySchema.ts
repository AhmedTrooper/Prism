import { z } from "zod";

const isoString = z
  .string()
  .refine((s) => !Number.isNaN(Date.parse(s)), { message: "invalid ISO timestamp" });

export const libraryFolderSchema = z.object({
  id: z.string().min(1),
  path: z.string().min(1),
  label: z.string().min(1).max(120),
  lastScannedAt: isoString.nullable(),
});

export const libraryVideoSchema = z.object({
  id: z.string().min(1),
  folderId: z.string().min(1),
  path: z.string().min(1),
  title: z.string().min(1),
  sizeBytes: z.number().int().nonnegative(),
  durationMs: z.number().int().nonnegative().nullable(),
  modifiedAt: isoString,
  thumbnailPath: z.string().optional(),
});

export const libraryStateSchema = z.object({
  folders: z.array(libraryFolderSchema),
  videos: z.array(libraryVideoSchema),
  status: z.enum(["idle", "scanning", "ready", "error"]),
  lastError: z.string().optional(),
});
