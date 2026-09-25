import { describe, it, expect } from "vitest";

import {
  basename,
  dirname,
  extname,
  ext,
  isVideoPath,
  isAudioPath,
  isSubtitlePath,
} from "@/lib/paths";

describe("paths.basename", () => {
  it("returns last segment of a forward-slash path", () => {
    expect(basename("/home/me/file.mp4")).toBe("file.mp4");
  });

  it("returns last segment of a back-slash path", () => {
    expect(basename("C:\\Users\\me\\file.mkv")).toBe("file.mkv");
  });

  it("returns the input when no separators are present", () => {
    expect(basename("movie.mp4")).toBe("movie.mp4");
  });

  it("strips trailing separators", () => {
    expect(basename("/home/me/")).toBe("me");
  });
});

describe("paths.dirname", () => {
  it("returns the parent path", () => {
    expect(dirname("/home/me/file.mp4")).toBe("/home/me");
  });

  it("returns '.' for a single-segment input", () => {
    expect(dirname("file.mp4")).toBe(".");
  });

  it("supports trailing slashes without mutating the parent", () => {
    expect(dirname("/home/me/")).toBe("/home");
  });
});

describe("paths.extname / ext", () => {
  it("returns leading-dot extension or empty string", () => {
    expect(extname("/home/me/file.mp4")).toBe(".mp4");
    expect(extname("/home/me/file")).toBe("");
  });

  it("ext returns the extension without the dot", () => {
    expect(ext("clip.MKV")).toBe("MKV");
    expect(ext("clip")).toBe("");
  });
});

describe("paths.isVideoPath", () => {
  it("accepts common video extensions", () => {
    expect(isVideoPath("/x/y/clip.mp4")).toBe(true);
    expect(isVideoPath("/x/y/clip.mkv")).toBe(true);
    expect(isVideoPath("/x/y/clip.webm")).toBe(true);
    expect(isVideoPath("/x/y/clip.mov")).toBe(true);
  });

  it("rejects audio and unknown extensions", () => {
    expect(isVideoPath("/x/y/clip.mp3")).toBe(false);
    expect(isVideoPath("/x/y/clip.txt")).toBe(false);
  });
});

describe("paths.isAudioPath", () => {
  it("accepts common audio extensions", () => {
    expect(isAudioPath("/x/y/track.mp3")).toBe(true);
    expect(isAudioPath("/x/y/track.flac")).toBe(true);
    expect(isAudioPath("/x/y/track.aac")).toBe(true);
  });

  it("rejects video and subtitle extensions", () => {
    expect(isAudioPath("/x/y/track.mp4")).toBe(false);
    expect(isAudioPath("/x/y/track.srt")).toBe(false);
  });
});

describe("paths.isSubtitlePath", () => {
  it("accepts common subtitle extensions", () => {
    expect(isSubtitlePath("/x/y/cap.srt")).toBe(true);
    expect(isSubtitlePath("/x/y/cap.vtt")).toBe(true);
    expect(isSubtitlePath("/x/y/cap.ass")).toBe(true);
  });

  it("rejects video and audio extensions", () => {
    expect(isSubtitlePath("/x/y/cap.mp4")).toBe(false);
    expect(isSubtitlePath("/x/y/cap.mp3")).toBe(false);
  });
});
