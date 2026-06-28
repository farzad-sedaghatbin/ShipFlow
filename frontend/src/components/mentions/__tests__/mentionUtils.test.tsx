import { describe, it, expect, vi } from 'vitest';

// UserProfilePopover pulls in API/context wiring we don't need for the pure
// helpers; stub it so the module imports cleanly in jsdom.
vi.mock('../../UserProfilePopover', () => ({
  default: ({ children }: { children: React.ReactNode }) => children,
}));

import { parseMentions, formatMentionToken } from '../mentionUtils';

describe('parseMentions', () => {
  it('returns [] for null / empty / no-mention text', () => {
    expect(parseMentions(null)).toEqual([]);
    expect(parseMentions('')).toEqual([]);
    expect(parseMentions('no mentions here')).toEqual([]);
  });

  it('parses a single-word @mention', () => {
    expect(parseMentions('hi @alice there')).toEqual([
      { name: 'alice', index: 3 },
    ]);
  });

  it('parses a quoted @"Full Name" mention', () => {
    expect(parseMentions('ping @"Jane Doe" please')).toEqual([
      { name: 'Jane Doe', index: 5 },
    ]);
  });

  it('parses names with dots/underscores between segments', () => {
    expect(parseMentions('@john.doe and @a_b').map((m) => m.name)).toEqual([
      'john.doe',
      'a_b',
    ]);
  });

  it('dedupes repeated mentions, keeping first appearance order', () => {
    const out = parseMentions('@bob @alice @bob');
    expect(out.map((m) => m.name)).toEqual(['bob', 'alice']);
  });
});

describe('formatMentionToken', () => {
  it('wraps names with spaces in quotes', () => {
    expect(formatMentionToken('Jane Doe')).toBe('@"Jane Doe"');
  });

  it('leaves single-word names unquoted', () => {
    expect(formatMentionToken('alice')).toBe('@alice');
  });
});
