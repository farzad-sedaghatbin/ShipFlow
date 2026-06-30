/**
 * Tests for the pure helper functions exported from WikiEditor.
 *
 * IMPORTANT: We do NOT import the default export (the React component) here
 * because BlockNote cannot run in jsdom. We import only the named exports so
 * no DOM or React context is needed.
 */
import { describe, it, expect, vi } from 'vitest';

// Mock @blocknote/react and @blocknote/core so the module can be imported
// without the full BlockNote DOM environment.
vi.mock('@blocknote/react', () => ({
  useCreateBlockNote: vi.fn(),
  BlockNoteView: vi.fn(),
  SuggestionMenuController: vi.fn(),
}));
vi.mock('@blocknote/mantine', () => ({
  BlockNoteView: vi.fn(),
}));
vi.mock('@blocknote/core', () => ({
  // nothing needed for the pure helpers
}));
vi.mock('@blocknote/mantine/style.css', () => ({}));
vi.mock('@blocknote/core/fonts/inter.css', () => ({}));

import {
  parseBlockNoteContent,
  serializeBlockNoteContent,
} from '../../../components/wiki/WikiEditor';

// ─── parseBlockNoteContent ────────────────────────────────────────────────────

describe('parseBlockNoteContent', () => {
  it('returns undefined for null input', () => {
    expect(parseBlockNoteContent(null)).toBeUndefined();
  });

  it('returns undefined for invalid JSON', () => {
    expect(parseBlockNoteContent('invalid json')).toBeUndefined();
  });

  it('returns undefined for valid JSON that is not an array', () => {
    expect(parseBlockNoteContent('{"type":"paragraph"}')).toBeUndefined();
  });

  it('returns the parsed array for valid BlockNote JSON', () => {
    const block = {
      id: '1',
      type: 'paragraph',
      props: {},
      content: [{ type: 'text', text: 'Hello', styles: {} }],
      children: [],
    };
    const jsonStr = JSON.stringify([block]);
    const result = parseBlockNoteContent(jsonStr);
    expect(Array.isArray(result)).toBe(true);
    expect(result).toHaveLength(1);
    expect(result![0]).toMatchObject({ id: '1', type: 'paragraph' });
  });
});

// ─── serializeBlockNoteContent ────────────────────────────────────────────────

describe('serializeBlockNoteContent', () => {
  it('serializes a block array to a JSON string', () => {
    const blocks = [
      {
        id: '1',
        type: 'paragraph' as const,
        props: {} as never,
        content: [] as never,
        children: [] as never,
      },
    ];
    const result = serializeBlockNoteContent(blocks as never);
    expect(typeof result).toBe('string');
    const parsed = JSON.parse(result);
    expect(Array.isArray(parsed)).toBe(true);
    expect(parsed[0].id).toBe('1');
  });

  it('returns "[]" for an empty array', () => {
    const result = serializeBlockNoteContent([] as never);
    expect(result).toBe('[]');
  });
});
