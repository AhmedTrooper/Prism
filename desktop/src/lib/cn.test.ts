import { describe, it, expect } from "vitest";

import { cn } from "@/lib/cn";

describe("cn", () => {
  it("returns a single string for a single input", () => {
    expect(cn("foo")).toBe("foo");
  });

  it("joins truthy class names with spaces", () => {
    expect(cn("foo", "bar")).toBe("foo bar");
  });

  it("filters out falsy values", () => {
    expect(cn("foo", false, null, undefined, 0, "", "bar")).toBe("foo bar");
  });

  it("supports arrays of class names", () => {
    expect(cn(["foo", "bar"], "baz")).toBe("foo bar baz");
  });

  it("supports objects", () => {
    expect(cn({ foo: true, bar: false, baz: true })).toBe("foo baz");
  });
});
