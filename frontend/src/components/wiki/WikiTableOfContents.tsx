import { useMemo } from "react";
import { useTranslation } from "react-i18next";

interface TocEntry {
  level: number;
  text: string;
  // Synthesised anchor from text — best-effort, just for display purposes
  anchor: string;
}

/**
 * Extract heading entries from a BlockNote JSON content string.
 * BlockNote stores content as an array of block objects; heading blocks look like:
 *   { type: "heading", props: { level: 1|2|3 }, content: [{ type: "text", text: "…" }] }
 */
function extractHeadings(content: string | null): TocEntry[] {
  if (!content) return [];
  try {
    const blocks: unknown[] = JSON.parse(content);
    if (!Array.isArray(blocks)) return [];
    const entries: TocEntry[] = [];
    for (const block of blocks) {
      if (
        block !== null &&
        typeof block === "object" &&
        "type" in block &&
        (block as Record<string, unknown>).type === "heading"
      ) {
        const b = block as Record<string, unknown>;
        const props = (b.props ?? {}) as Record<string, unknown>;
        const level = typeof props.level === "number" ? props.level : 1;
        const contentArr = b.content;
        let text = "";
        if (Array.isArray(contentArr)) {
          for (const inline of contentArr) {
            if (
              inline !== null &&
              typeof inline === "object" &&
              "type" in inline &&
              (inline as Record<string, unknown>).type === "text"
            ) {
              const t = (inline as Record<string, unknown>).text;
              if (typeof t === "string") text += t;
            }
          }
        }
        if (text.trim()) {
          entries.push({
            level,
            text: text.trim(),
            anchor: text.trim().toLowerCase().replace(/\s+/g, "-"),
          });
        }
      }
    }
    return entries;
  } catch {
    return [];
  }
}

interface WikiTableOfContentsProps {
  content: string | null;
}

export default function WikiTableOfContents({ content }: WikiTableOfContentsProps) {
  const { t } = useTranslation();
  const headings = useMemo(() => extractHeadings(content), [content]);

  if (headings.length === 0) return null;

  return (
    <aside className="sticky top-4 text-sm space-y-1 min-w-[180px]">
      <p className="font-semibold text-foreground mb-2">
        {t("wiki.tableOfContents")}
      </p>
      <ul className="space-y-0.5">
        {headings.map((h, i) => (
          <li
            key={i}
            style={{ paddingLeft: `${(h.level - 1) * 12}px` }}
            className="text-muted-foreground hover:text-foreground transition-colors"
          >
            <a href={`#${h.anchor}`} className="hover:underline truncate block">
              {h.text}
            </a>
          </li>
        ))}
      </ul>
    </aside>
  );
}
