import { z } from "zod";

export const greetArgsSchema = z.object({
  name: z.string().min(1).max(64),
});

export const greetResultSchema = z.string().min(1);
