/**
 * Pure helpers for the wiki internal-link (`[[pageId]]`) and @mention features.
 *
 * The wiki page body is stored as BlockNote JSON. These helpers extract the
 * plain text from that JSON so token parsing can run over readable content,
 * independent of the BlockNote runtime (which cannot mount in jsdom).
 *
 * Everything here is side-effect free and unit-tested without a DOM.
 */

// Matches an internal page-link token: `[[123]]`. Whitespace around the id is
// tolerated, e.g. `[[ 123 ]]`. Only digits are accepted as a page id.
const PAGE_LINK_REGEX = /\[\[\s*(\d+)\s*\]\]/g;

export interface ParsedPageLinkToken {
  /** The referenced page id. */
  pageId: number;
  /** Character index of the `[[` in the source text. */
  index: number;
}

/**
 * Recursively pull every `text` run out of a BlockNote block tree and join it
 * into a single plain-text string (blocks separated by newlines). Returns "" on
 * null/invalid input rather than throwing, so callers can parse defensively.
 */
export function extractPlainText(content: string | null | undefined): string {
  if (!content) return '';
  let blocks: unknown;
  try {
    blocks = JSON.parse(content);
  } catch {
    return '';
  }
  if (!Array.isArray(blocks)) return '';

  const lines: string[] = [];

  const walkInline = (inline: unknown): string => {
    if (inline === null || typeof inline !== 'object') return '';
    const obj = inline as Record<string, unknown>;
    if (obj.type === 'text' && typeof obj.text === 'string') {
      return obj.text;
    }
    // Nested inline content (e.g. links wrap their own content array).
    if (Array.isArray(obj.content)) {
      return obj.content.map(walkInline).join('');
    }
    return '';
  };

  const walkBlock = (block: unknown): void => {
    if (block === null || typeof block !== 'object') return;
    const obj = block as Record<string, unknown>;
    if (Array.isArray(obj.content)) {
      const text = obj.content.map(walkInline).join('');
      if (text) lines.push(text);
    }
    if (Array.isArray(obj.children)) {
      obj.children.forEach(walkBlock);
    }
  };

  blocks.forEach(walkBlock);
  return lines.join('\n');
}

/**
 * Find every `[[pageId]]` token in a plain-text string, in order of appearance.
 * Duplicates are preserved (the same page may be referenced more than once);
 * callers that want a unique set should dedupe against the resolved link map.
 */
export function parsePageLinkTokens(text: string | null | undefined): ParsedPageLinkToken[] {
  if (!text) return [];
  const regex = new RegExp(PAGE_LINK_REGEX.source, PAGE_LINK_REGEX.flags);
  const out: ParsedPageLinkToken[] = [];
  let match: RegExpExecArray | null;
  while ((match = regex.exec(text)) !== null) {
    out.push({ pageId: Number(match[1]), index: match.index });
  }
  return out;
}

/**
 * Return the distinct page ids referenced by `[[pageId]]` tokens in the given
 * BlockNote content, preserving first-appearance order. Convenience wrapper over
 * {@link extractPlainText} + {@link parsePageLinkTokens}.
 */
export function referencedPageIds(content: string | null | undefined): number[] {
  const tokens = parsePageLinkTokens(extractPlainText(content));
  const seen = new Set<number>();
  const ids: number[] = [];
  for (const tok of tokens) {
    if (!seen.has(tok.pageId)) {
      seen.add(tok.pageId);
      ids.push(tok.pageId);
    }
  }
  return ids;
}
