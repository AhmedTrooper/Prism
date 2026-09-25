import { describe, it, expect } from "vitest";

import { exampleItemSchema, exampleItemListSchema } from "@/features/_example";

describe("example schemas", () => {
  it("accepts a valid item", () => {
    const r = exampleItemSchema.safeParse({
      id: "ex_1",
      title: "Hello",
      createdAt: 100,
    });
    expect(r.success).toBe(true);
  });

  it("rejects empty title", () => {
    const r = exampleItemSchema.safeParse({
      id: "ex_1",
      title: "",
      createdAt: 100,
    });
    expect(r.success).toBe(false);
  });

  it("rejects negative createdAt", () => {
    const r = exampleItemSchema.safeParse({
      id: "ex_1",
      title: "Hello",
      createdAt: -1,
    });
    expect(r.success).toBe(false);
  });

  it("validates a list", () => {
    const r = exampleItemListSchema.safeParse([
      { id: "1", title: "a", createdAt: 1 },
      { id: "2", title: "b", createdAt: 2 },
    ]);
    expect(r.success).toBe(true);
  });

  it("rejects a list with an invalid item", () => {
    const r = exampleItemListSchema.safeParse([
      { id: "1", title: "ok", createdAt: 1 },
      { id: "2", title: "", createdAt: 2 },
    ]);
    expect(r.success).toBe(false);
  });
});
