/**
 * `cn` — className composer built on `clsx`.
 *
 * Use everywhere instead of template strings so callers can pass arrays,
 * conditionals, falsy values, and nested objects in any combination.
 *
 *   cn("btn", isActive && "btn-active", { "btn-disabled": disabled })
 */

import clsx, { type ClassValue } from "clsx";

export function cn(...inputs: ClassValue[]): string {
  return clsx(inputs);
}
