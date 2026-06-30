// Utilities for comparing two wiki page revisions.
//
// Wiki content is stored as BlockNote JSON. The backend's `contentText` joins
// every text node with single spaces (no block boundaries), which is useless for
// a readable line-by-line diff. So we extract one line per block from the JSON
// here, then run a Longest-Common-Subsequence diff to align the two versions
// into side-by-side rows for the compare view.

export type DiffStatus = "same" | "changed" | "added" | "removed";

export interface DiffRow {
  /** Line from the current version (null when the row is a pure addition). */
  left: string | null;
  /** Line from the revision being restored (null when the row is a pure removal). */
  right: string | null;
  status: DiffStatus;
}

interface RawBlock {
  type?: string;
  content?: unknown;
  children?: unknown;
}

/** Recursively collect text from a block's inline content (handles links etc.). */
function inlineText(content: unknown): string {
  if (!Array.isArray(content)) return "";
  let out = "";
  for (const node of content) {
    if (node && typeof node === "object") {
      const n = node as { text?: unknown; content?: unknown };
      if (typeof n.text === "string") out += n.text;
      else if (n.content) out += inlineText(n.content);
    }
  }
  return out;
}

/**
 * Parse a BlockNote JSON document into one trimmed text line per block,
 * descending into nested children (list items, etc.). Blank blocks are dropped
 * so the diff stays focused on real content. Returns [] for null/invalid input.
 */
export function blocksToLines(contentJson: string | null | undefined): string[] {
  if (!contentJson) return [];
  let parsed: unknown;
  try {
    parsed = JSON.parse(contentJson);
  } catch {
    return [];
  }
  if (!Array.isArray(parsed)) return [];

  const lines: string[] = [];
  const walk = (blocks: RawBlock[]) => {
    for (const block of blocks) {
      if (!block || typeof block !== "object") continue;
      const text = inlineText(block.content).trim();
      if (text) lines.push(text);
      if (Array.isArray(block.children) && block.children.length > 0) {
        walk(block.children as RawBlock[]);
      }
    }
  };
  walk(parsed as RawBlock[]);
  return lines;
}

type Op = { type: "equal" | "del" | "ins"; value: string };

/** Classic LCS diff producing a forward-ordered op stream. */
function lcsOps(a: string[], b: string[]): Op[] {
  const n = a.length;
  const m = b.length;
  // dp[i][j] = length of LCS of a[i..] and b[j..]
  const dp: number[][] = Array.from({ length: n + 1 }, () =>
    new Array<number>(m + 1).fill(0)
  );
  for (let i = n - 1; i >= 0; i--) {
    for (let j = m - 1; j >= 0; j--) {
      dp[i][j] =
        a[i] === b[j]
          ? dp[i + 1][j + 1] + 1
          : Math.max(dp[i + 1][j], dp[i][j + 1]);
    }
  }

  const ops: Op[] = [];
  let i = 0;
  let j = 0;
  while (i < n && j < m) {
    if (a[i] === b[j]) {
      ops.push({ type: "equal", value: a[i] });
      i++;
      j++;
    } else if (dp[i + 1][j] >= dp[i][j + 1]) {
      ops.push({ type: "del", value: a[i] });
      i++;
    } else {
      ops.push({ type: "ins", value: b[j] });
      j++;
    }
  }
  while (i < n) ops.push({ type: "del", value: a[i++] });
  while (j < m) ops.push({ type: "ins", value: b[j++] });
  return ops;
}

/**
 * Diff two line arrays into side-by-side rows. Consecutive removals and
 * additions are zipped into "changed" rows (removed-left paired with
 * added-right); any surplus becomes pure "removed"/"added" rows.
 */
export function diffLines(
  currentLines: string[],
  revisionLines: string[]
): DiffRow[] {
  const ops = lcsOps(currentLines, revisionLines);
  const rows: DiffRow[] = [];
  let pendingDel: string[] = [];
  let pendingIns: string[] = [];

  const flush = () => {
    const paired = Math.min(pendingDel.length, pendingIns.length);
    for (let k = 0; k < paired; k++) {
      rows.push({ left: pendingDel[k], right: pendingIns[k], status: "changed" });
    }
    for (let k = paired; k < pendingDel.length; k++) {
      rows.push({ left: pendingDel[k], right: null, status: "removed" });
    }
    for (let k = paired; k < pendingIns.length; k++) {
      rows.push({ left: null, right: pendingIns[k], status: "added" });
    }
    pendingDel = [];
    pendingIns = [];
  };

  for (const op of ops) {
    if (op.type === "equal") {
      flush();
      rows.push({ left: op.value, right: op.value, status: "same" });
    } else if (op.type === "del") {
      pendingDel.push(op.value);
    } else {
      pendingIns.push(op.value);
    }
  }
  flush();
  return rows;
}

/** True when the two documents have no line-level text differences. */
export function hasNoChanges(rows: DiffRow[]): boolean {
  return rows.every((r) => r.status === "same");
}
