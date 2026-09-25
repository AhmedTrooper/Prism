/**
 * API surface of `_example`. Stubbed out — replace each call with the
 * real `invokeCommand(...)` once the Rust side is wired up.
 */

import { ok, err, type Result } from "@/lib/result";
import { exampleItemListSchema } from "../schemas/exampleSchema";
import type { ExampleItem } from "../types";

export async function fetchExampleItems(): Promise<Result<ExampleItem[], Error>> {
  // Local-only stub. Real impl should call invokeCommand<...>("example:list", {}).
  const raw = window.localStorage.getItem("prism:example:items");
  if (!raw) return ok([]);
  try {
    const parsed = exampleItemListSchema.safeParse(JSON.parse(raw));
    if (!parsed.success) return err(new Error("example list corrupted"));
    return ok(parsed.data);
  } catch (cause) {
    return err(cause instanceof Error ? cause : new Error(String(cause)));
  }
}
