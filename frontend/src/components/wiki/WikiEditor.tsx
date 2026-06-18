import "@blocknote/core/fonts/inter.css";
import "@blocknote/mantine/style.css";

import { type Block, type PartialBlock } from "@blocknote/core";
import { useCreateBlockNote } from "@blocknote/react";
// Use the Mantine flavor's BlockNoteView — it wires the default UI (SideMenu,
// formatting toolbar, slash menu). @blocknote/react only exports the bare
// BlockNoteViewRaw, which leaves those components undefined and crashes in the
// production bundle ("undefined is not an object (evaluating '…SideMenu')").
import { BlockNoteView } from "@blocknote/mantine";
import { useEffect } from "react";

// ─── Pure helpers (exported for unit tests without mounting the editor) ───────

/**
 * Parse a JSON string produced by serializeBlockNoteContent back into
 * a PartialBlock array. Returns undefined on null input or invalid JSON
 * so that BlockNote can start with an empty document.
 */
export function parseBlockNoteContent(
  jsonStr: string | null
): PartialBlock[] | undefined {
  if (!jsonStr) return undefined;
  try {
    const parsed = JSON.parse(jsonStr);
    if (!Array.isArray(parsed)) return undefined;
    return parsed as PartialBlock[];
  } catch {
    return undefined;
  }
}

/**
 * Serialize the current editor block array to a JSON string for persistence.
 */
export function serializeBlockNoteContent(blocks: Block[]): string {
  return JSON.stringify(blocks);
}

// ─── Component ────────────────────────────────────────────────────────────────

interface WikiEditorProps {
  initialContent: string | null;
  editable: boolean;
  onChange: (json: string) => void;
}

export default function WikiEditor({
  initialContent,
  editable,
  onChange,
}: WikiEditorProps) {
  // Create the editor with an empty document. We deliberately do NOT pass
  // initialContent here: useCreateBlockNote throws synchronously if the stored
  // JSON is not a structurally valid BlockNote document (e.g. legacy/imported/
  // hand-authored content), which would white-screen the whole page. Instead we
  // load the content defensively below.
  const editor = useCreateBlockNote();

  useEffect(() => {
    const blocks = parseBlockNoteContent(initialContent);
    if (!blocks || blocks.length === 0) return;
    try {
      editor.replaceBlocks(editor.document, blocks);
    } catch (e) {
      // Malformed stored content — start from an empty document rather than crash.
      console.warn("WikiEditor: could not load stored content, starting empty", e);
    }
    // Only re-run when the source content changes.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [initialContent]);

  return (
    <BlockNoteView
      editor={editor}
      editable={editable}
      onChange={() => {
        onChange(serializeBlockNoteContent(editor.document));
      }}
    />
  );
}
