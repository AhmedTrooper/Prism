/**
 * Public surface of the `_example` feature. Copy this directory, rename
 * the namespace, and you have a new feature ready for components, hooks,
 * stores, schemas, lib, api, types.
 */

export { ExampleCard } from "./components/ExampleCard";
export { useExampleCount } from "./hooks/useExampleCount";
export { useExampleStore } from "./stores/exampleStore";
export {
  exampleItemSchema,
  exampleItemListSchema,
} from "./schemas/exampleSchema";
export {
  nextExampleId,
  newExampleItem,
  sortByCreatedAt,
} from "./lib/ids";
export { fetchExampleItems } from "./api/exampleClient";
export type { ExampleItem } from "./types";
