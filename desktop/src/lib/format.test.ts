import { describe, it, expect } from "vitest";

import {
  formatDuration,
  formatBytes,
  formatRelativeDate,
  formatSeekDelta,
} from "@/lib/format";

describe("formatDuration", () => {
  it("formats zero", () => {
    expect(formatDuration(0)).toBe("0:00");
  });

  it("formats sub-minute durations with leading zero", () => {
    expect(formatDuration(5)).toBe("0:05");
    expect(formatDuration(59)).toBe("0:59");
  });

  it("formats minutes:seconds past one minute", () => {
    expect(formatDuration(60)).toBe("1:00");
    expect(formatDuration(125)).toBe("2:05");
  });

  it("switches to h:mm:ss past one hour", () => {
    expect(formatDuration(3600)).toBe("1:00:00");
    expect(formatDuration(3661)).toBe("1:01:01");
  });

  it("handles negative input as 0", () => {
    expect(formatDuration(-5)).toBe("0:00");
  });
});

describe("formatBytes", () => {
  it("formats zero", () => {
    expect(formatBytes(0)).toBe("0 B");
  });

  it("formats bytes", () => {
    expect(formatBytes(512)).toBe("512 B");
  });

  it("formats kilobytes", () => {
    expect(formatBytes(1024)).toBe("1.0 KB");
    expect(formatBytes(1536)).toBe("1.5 KB");
  });

  it("formats megabytes", () => {
    expect(formatBytes(1024 * 1024)).toBe("1.0 MB");
  });

  it("formats gigabytes", () => {
    expect(formatBytes(1024 ** 3)).toBe("1.0 GB");
  });

  it("formats terabytes", () => {
    expect(formatBytes(1024 ** 4)).toBe("1.0 TB");
  });
});

describe("formatRelativeDate", () => {
  it("returns 'just now' for very recent timestamps", () => {
    const now = Date.now();
    expect(formatRelativeDate(now - 30_000, now)).toBe("just now");
  });

  it("returns minutes for sub-hour differences", () => {
    const now = 1_700_000_000_000;
    expect(formatRelativeDate(now - 5 * 60_000, now)).toBe("5m ago");
  });

  it("returns hours for sub-day differences", () => {
    const now = 1_700_000_000_000;
    expect(formatRelativeDate(now - 3 * 3_600_000, now)).toBe("3h ago");
  });

  it("returns days for sub-month differences", () => {
    const now = 1_700_000_000_000;
    expect(formatRelativeDate(now - 2 * 86_400_000, now)).toBe("2d ago");
  });
});

describe("formatSeekDelta", () => {
  it("renders positive seconds with + prefix", () => {
    expect(formatSeekDelta(5)).toBe("+5s");
  });

  it("renders positive minutes", () => {
    expect(formatSeekDelta(60)).toBe("+1m");
    expect(formatSeekDelta(125)).toBe("+2m 5s");
  });

  it("renders negative seconds with - prefix", () => {
    expect(formatSeekDelta(-30)).toBe("-30s");
  });

  it("renders negative minutes", () => {
    expect(formatSeekDelta(-90)).toBe("-1m 30s");
  });
});
