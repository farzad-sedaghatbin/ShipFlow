import "@blocknote/core/fonts/inter.css";
import "@blocknote/mantine/style.css";

import { type Block, type PartialBlock } from "@blocknote/core";
import { BlockNoteViewRaw as BlockNoteView, useCreateBlockNote } from "@blocknote/react";

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
  const editor = useCreateBlockNote({
    initialContent: parseBlockNoteContent(initialContent),
  });

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
