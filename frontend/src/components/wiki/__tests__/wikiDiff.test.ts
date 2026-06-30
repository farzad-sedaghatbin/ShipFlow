import { describe, it, expect } from "vitest";
import { blocksToLines, diffLines, hasNoChanges } from "../wikiDiff";

// Build a minimal BlockNote-shaped document JSON string for tests.
function doc(...blocks: unknown[]): string {
  return JSON.stringify(blocks);
}
function para(text: string, children?: unknown[]) {
  return {
    type: "paragraph",
    content: [{ type: "text", text }],
    ...(children ? { children } : {}),
  };
}

describe("blocksToLines", () => {
  it("returns one trimmed line per block", () => {
    const json = doc(para("Hello"), para("World"));
    expect(blocksToLines(json)).toEqual(["Hello", "World"]);
  });

  it("returns [] for null, empty, or invalid JSON", () => {
    expect(blocksToLines(null)).toEqual([]);
    expect(blocksToLines(undefined)).toEqual([]);
    expect(blocksToLines("")).toEqual([]);
    expect(blocksToLines("{not json")).toEqual([]);
    expect(blocksToLines('{"not":"array"}')).toEqual([]);
  });

  it("skips blank blocks but keeps content blocks", () => {
    const json = doc(para("Keep"), para("   "), para("Also keep"));
    expect(blocksToLines(json)).toEqual(["Keep", "Also keep"]);
  });

  it("descends into nested children (e.g. list items)", () => {
    const json = doc(para("Parent", [para("Child A"), para("Child B")]));
    expect(blocksToLines(json)).toEqual(["Parent", "Child A", "Child B"]);
  });

  it("concatenates multiple inline runs and nested link content", () => {
    const json = doc({
      type: "paragraph",
      content: [
        { type: "text", text: "See " },
        { type: "link", content: [{ type: "text", text: "the docs" }] },
      ],
    });
    expect(blocksToLines(json)).toEqual(["See the docs"]);
  });
});

describe("diffLines", () => {
  it("marks identical content as all 'same'", () => {
    const rows = diffLines(["a", "b", "c"], ["a", "b", "c"]);
    expect(rows.map((r) => r.status)).toEqual(["same", "same", "same"]);
    expect(hasNoChanges(rows)).toBe(true);
  });

  it("detects a pure addition (left null, right set)", () => {
    const rows = diffLines(["a", "c"], ["a", "b", "c"]);
    const added = rows.find((r) => r.status === "added");
    expect(added).toEqual({ left: null, right: "b", status: "added" });
    expect(hasNoChanges(rows)).toBe(false);
  });

  it("detects a pure removal (left set, right null)", () => {
    const rows = diffLines(["a", "b", "c"], ["a", "c"]);
    const removed = rows.find((r) => r.status === "removed");
    expect(removed).toEqual({ left: "b", right: null, status: "removed" });
  });

  it("pairs a removed line with an added line as a single 'changed' row", () => {
    const rows = diffLines(["a", "old", "z"], ["a", "new", "z"]);
    const changed = rows.find((r) => r.status === "changed");
    expect(changed).toEqual({ left: "old", right: "new", status: "changed" });
    // a and z stay aligned and unchanged
    expect(rows.filter((r) => r.status === "same").map((r) => r.left)).toEqual([
      "a",
      "z",
    ]);
  });

  it("emits surplus removals/additions when run lengths differ", () => {
    const rows = diffLines(["a", "x1", "x2"], ["a", "y1"]);
    // x1 pairs with y1 (changed); x2 has no partner (removed)
    expect(rows.find((r) => r.status === "changed")).toEqual({
      left: "x1",
      right: "y1",
      status: "changed",
    });
    expect(rows.find((r) => r.status === "removed")).toEqual({
      left: "x2",
      right: null,
      status: "removed",
    });
  });

  it("handles empty current (everything added)", () => {
    const rows = diffLines([], ["a", "b"]);
    expect(rows.every((r) => r.status === "added")).toBe(true);
    expect(rows.map((r) => r.right)).toEqual(["a", "b"]);
  });
});
