import { describe, it, expect } from "vitest";

import {
  nextExampleId,
  newExampleItem,
  sortByCreatedAt,
} from "@/features/_example";

describe("example helpers", () => {
  it("generates unique IDs", () => {
    const a = nextExampleId();
    const b = nextExampleId();
    expect(a).not.toBe(b);
    expect(a).toMatch(/^ex_/);
  });

  it("builds a fresh item with createdAt set", () => {
    const item = newExampleItem("hello");
    expect(item.title).toBe("hello");
    expect(item.description).toBeUndefined();
    expect(item.createdAt).toBeGreaterThan(0);
    expect(item.id).toMatch(/^ex_/);
  });

  it("sorts items by createdAt ascending", () => {
    const a = { id: "1", title: "a", createdAt: 100 };
    const b = { id: "2", title: "b", createdAt: 300 };
    const c = { id: "3", title: "c", createdAt: 200 };
    const sorted = sortByCreatedAt([a, b, c]);
    expect(sorted.map((i) => i.id)).toEqual(["1", "3", "2"]);
  });

  it("does not mutate the input array", () => {
    const items = [
      { id: "1", title: "a", createdAt: 100 },
      { id: "2", title: "b", createdAt: 50 },
    ];
    const sorted = sortByCreatedAt(items);
    expect(items[0]?.id).toBe("1");
    expect(items[1]?.id).toBe("2");
    expect(sorted[0]?.id).toBe("2");
  });
});
