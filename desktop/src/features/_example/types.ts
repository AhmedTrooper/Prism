/**
 * Public types of the `_example` feature.
 *
 * Each feature exports its own types from a `types.ts` so consumers can
 * pull them via `import type { ExampleItem } from "@/features/_example"`.
 */

export interface ExampleItem {
  id: string;
  title: string;
  description?: string;
  createdAt: number;
}
