import { describe, it, expect } from 'vitest';
import {
  extractPlainText,
  parsePageLinkTokens,
  referencedPageIds,
} from '../wikiTokens';

// Helper to build a minimal BlockNote paragraph block.
function paragraph(text: string) {
  return {
    id: Math.random().toString(36).slice(2),
    type: 'paragraph',
    props: {},
    content: [{ type: 'text', text, styles: {} }],
    children: [],
  };
}

describe('extractPlainText', () => {
  it('returns "" for null / empty / invalid input', () => {
    expect(extractPlainText(null)).toBe('');
    expect(extractPlainText('')).toBe('');
    expect(extractPlainText('not json')).toBe('');
    expect(extractPlainText('{"type":"paragraph"}')).toBe('');
  });

  it('joins text from multiple blocks with newlines', () => {
    const json = JSON.stringify([paragraph('Hello'), paragraph('World')]);
    expect(extractPlainText(json)).toBe('Hello\nWorld');
  });

  it('concatenates multiple inline runs within a block', () => {
    const block = {
      id: '1',
      type: 'paragraph',
      props: {},
      content: [
        { type: 'text', text: 'See ', styles: {} },
        { type: 'text', text: '[[42]]', styles: { bold: true } },
      ],
      children: [],
    };
    expect(extractPlainText(JSON.stringify([block]))).toBe('See [[42]]');
  });

  it('recurses into nested children and link inline content', () => {
    const block = {
      id: '1',
      type: 'paragraph',
      props: {},
      content: [
        {
          type: 'link',
          href: 'https://x',
          content: [{ type: 'text', text: 'inside link', styles: {} }],
        },
      ],
      children: [paragraph('child text')],
    };
    const out = extractPlainText(JSON.stringify([block]));
    expect(out).toContain('inside link');
    expect(out).toContain('child text');
  });
});

describe('parsePageLinkTokens', () => {
  it('returns [] when there are no tokens', () => {
    expect(parsePageLinkTokens('no links here')).toEqual([]);
    expect(parsePageLinkTokens(null)).toEqual([]);
  });

  it('parses a single [[id]] token with its index', () => {
    const tokens = parsePageLinkTokens('Go to [[42]] now');
    expect(tokens).toEqual([{ pageId: 42, index: 6 }]);
  });

  it('parses multiple tokens including duplicates', () => {
    const tokens = parsePageLinkTokens('[[1]] then [[2]] then [[1]]');
    expect(tokens.map((t) => t.pageId)).toEqual([1, 2, 1]);
  });

  it('tolerates whitespace inside the brackets', () => {
    expect(parsePageLinkTokens('[[ 7 ]]').map((t) => t.pageId)).toEqual([7]);
  });

  it('ignores non-numeric and malformed tokens', () => {
    expect(parsePageLinkTokens('[[abc]] [[1.5]] [[]]')).toEqual([]);
  });
});

describe('referencedPageIds', () => {
  it('returns distinct ids in first-appearance order from BlockNote content', () => {
    const json = JSON.stringify([
      paragraph('intro [[5]] middle'),
      paragraph('again [[3]] and [[5]]'),
    ]);
    expect(referencedPageIds(json)).toEqual([5, 3]);
  });

  it('returns [] for content without tokens', () => {
    expect(referencedPageIds(JSON.stringify([paragraph('plain')]))).toEqual([]);
  });
});
