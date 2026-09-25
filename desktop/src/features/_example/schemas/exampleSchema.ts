/**
 * Zod schemas for the `_example` feature. Mirrors `types.ts`; consumers
 * prefer the TS types but schemas validate any boundary input (IPC,
 * localStorage, drag-drop payload).
 */

import { z } from "zod";

export const exampleItemSchema = z.object({
  id: z.string().min(1),
  title: z.string().min(1).max(120),
  description: z.string().max(500).optional(),
  createdAt: z.number().int().nonnegative(),
});

export const exampleItemListSchema = z.array(exampleItemSchema);
