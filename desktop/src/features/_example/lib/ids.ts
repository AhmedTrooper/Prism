/**
 * Pure helpers used by `_example`. No React, no IPC — fully unit-testable.
 */

import type { ExampleItem } from "../types";

let counter = 0;
export function nextExampleId(): string {
  counter += 1;
  return `ex_${Date.now().toString(36)}_${counter}`;
}

export function newExampleItem(
  title: string,
  description?: string,
): ExampleItem {
  return {
    id: nextExampleId(),
    title,
    description,
    createdAt: Date.now(),
  };
}

export function sortByCreatedAt(items: ExampleItem[]): ExampleItem[] {
  return items.slice().sort((a, b) => a.createdAt - b.createdAt);
}
