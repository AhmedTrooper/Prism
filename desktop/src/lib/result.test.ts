import { describe, it, expect } from "vitest";

import { ok, err, tryAsync, trySync, isOk, isErr } from "@/lib/result";

describe("Result helpers", () => {
  it("ok / isOk / isErr", () => {
    const r = ok(42);
    expect(isOk(r)).toBe(true);
    expect(isErr(r)).toBe(false);
    if (r.ok) expect(r.value).toBe(42);
  });

  it("err / isOk / isErr", () => {
    const e = err(new Error("nope"));
    expect(isOk(e)).toBe(false);
    expect(isErr(e)).toBe(true);
    if (!e.ok) expect(e.error.message).toBe("nope");
  });

  it("trySync returns ok for a function that throws nothing", () => {
    const r = trySync(() => 7);
    expect(r.ok).toBe(true);
    if (r.ok) expect(r.value).toBe(7);
  });

  it("trySync catches thrown errors and wraps them", () => {
    const r = trySync(() => {
      throw new Error("boom");
    });
    expect(r.ok).toBe(false);
    if (!r.ok) expect(r.error.message).toBe("boom");
  });

  it("trySync coerces non-Error throws", () => {
    const r = trySync(() => {
      throw "string";
    });
    expect(r.ok).toBe(false);
    if (!r.ok) expect(r.error).toBeInstanceOf(Error);
    if (!r.ok) expect(r.error.message).toBe("string");
  });

  it("tryAsync returns ok for resolved promises", async () => {
    const r = await tryAsync(async () => "ok");
    expect(r.ok).toBe(true);
    if (r.ok) expect(r.value).toBe("ok");
  });

  it("tryAsync returns err for rejected promises", async () => {
    const r = await tryAsync(async () => {
      throw new Error("async-boom");
    });
    expect(r.ok).toBe(false);
    if (!r.ok) expect(r.error.message).toBe("async-boom");
  });
});
