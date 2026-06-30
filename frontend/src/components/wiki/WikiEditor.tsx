import "@blocknote/core/fonts/inter.css";
import "@blocknote/mantine/style.css";

import { type Block, type PartialBlock } from "@blocknote/core";
import {
  useCreateBlockNote,
  SuggestionMenuController,
  type DefaultReactSuggestionItem,
} from "@blocknote/react";
// Use the Mantine flavor's BlockNoteView — it wires the default UI (SideMenu,
// formatting toolbar, slash menu). @blocknote/react only exports the bare
// BlockNoteViewRaw, which leaves those components undefined and crashes in the
// production bundle ("undefined is not an object (evaluating '…SideMenu')").
import { BlockNoteView } from "@blocknote/mantine";
import { forwardRef, useEffect, useImperativeHandle } from "react";
import { useTranslation } from "react-i18next";
import { useTheme } from "../../contexts";
import commentService, { type MentionUser } from "../../services/commentService";
import { formatMentionToken } from "../mentions/mentionUtils";

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

/**
 * Imperative handle exposed to parents (e.g. WikiPage) so they can insert an
 * internal `[[pageId]]` link token at the editor's current cursor position.
 */
export interface WikiEditorHandle {
  insertText: (text: string) => void;
}

const WikiEditor = forwardRef<WikiEditorHandle, WikiEditorProps>(function WikiEditor(
  { initialContent, editable, onChange },
  ref,
) {
  // Create the editor with an empty document. We deliberately do NOT pass
  // initialContent here: useCreateBlockNote throws synchronously if the stored
  // JSON is not a structurally valid BlockNote document (e.g. legacy/imported/
  // hand-authored content), which would white-screen the whole page. Instead we
  // load the content defensively below.
  const editor = useCreateBlockNote();
  const { t } = useTranslation();
  // Keep the editor's theme in sync with the app theme; otherwise BlockNote
  // keeps whatever color scheme it mounted with (e.g. a dark editor surface
  // lingering after switching the app back to light mode).
  const { actualMode } = useTheme();

  // Build the `@`-mention suggestion items. Typing `@` opens this menu, which
  // searches the same people source as Comments (`searchUsersForMention`).
  // Selecting a teammate inserts a PLAIN-TEXT token — `@Name` or `@"Full Name"`
  // — identical to the comment format, so the backend's WikiService mention
  // parser (which scans the page's extracted text) resolves and notifies the
  // mentioned user on save. We deliberately do NOT use a custom inline content
  // spec: a styled inline node would not survive BlockNote's plain-text
  // extraction and the mention would never be detected on the server.
  const getMentionMenuItems = async (
    query: string,
  ): Promise<DefaultReactSuggestionItem[]> => {
    let users: MentionUser[] = [];
    try {
      const response = await commentService.searchUsersForMention(query);
      users = response.data;
    } catch (e) {
      console.warn("WikiEditor: could not search users for mention", e);
      users = [];
    }
    return users.map((user) => ({
      title: user.displayName,
      subtext: t("wiki.mentionItemSubtitle", { username: user.username }),
      onItemClick: () => {
        // Trailing space ends the token and separates it from following text.
        editor.insertInlineContent([
          `${formatMentionToken(user.displayName)} `,
        ]);
      },
    }));
  };

  // Expose an imperative insert so the page-link picker can drop a `[[id]]`
  // token at the cursor. After inserting, push the change up immediately so the
  // draft content reflects the new token without waiting for another keystroke.
  useImperativeHandle(
    ref,
    () => ({
      insertText: (text: string) => {
        try {
          editor.insertInlineContent([text]);
          onChange(serializeBlockNoteContent(editor.document));
        } catch (e) {
          console.warn("WikiEditor: could not insert text", e);
        }
      },
    }),
    [editor, onChange],
  );

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
      theme={actualMode}
      onChange={() => {
        onChange(serializeBlockNoteContent(editor.document));
      }}
    >
      {/* `@`-mention autocomplete. Passing this child does NOT suppress the
          default UI (slash menu, formatting toolbar, etc.) — BlockNote renders
          BlockNoteDefaultUI alongside children. Only mount it in edit mode. */}
      {editable && (
        <SuggestionMenuController
          triggerCharacter="@"
          getItems={getMentionMenuItems}
        />
      )}
    </BlockNoteView>
  );
});

export default WikiEditor;
