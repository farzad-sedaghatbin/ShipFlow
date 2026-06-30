import React from 'react';
import { MentionUser } from '../../services/commentService';
import UserProfilePopover from '../UserProfilePopover';

/**
 * Shared @mention parsing + rendering used by Comments and the Wiki page view.
 *
 * Mentions are stored as plain text in the body: `@SingleWord` or `@"Full Name"`.
 * The backend parses the same format on save to dispatch notifications, so the
 * frontend only needs to (1) help authors insert them and (2) render them.
 */

// Matches both `@"Full Name"` and `@SingleWord`. Unquoted names may contain
// letters/numbers with single dots/underscores between segments (no trailing
// punctuation). Unicode-aware so non-Latin display names work.
const MENTION_REGEX = /@"([^"]+)"|@([\p{L}\p{N}]+(?:[._][\p{L}\p{N}]+)*)/gu;

export interface ParsedMention {
  /** Resolved name (quotes stripped for `@"Full Name"`). */
  name: string;
  /** Character index of the `@` in the source text. */
  index: number;
}

/**
 * Extract every distinct @mention name from a plain-text string, preserving the
 * order of first appearance. Pure — safe to unit test without a DOM.
 */
export function parseMentions(text: string | null | undefined): ParsedMention[] {
  if (!text) return [];
  const regex = new RegExp(MENTION_REGEX.source, MENTION_REGEX.flags);
  const out: ParsedMention[] = [];
  const seen = new Set<string>();
  let match: RegExpExecArray | null;
  while ((match = regex.exec(text)) !== null) {
    const name = match[1] || match[2];
    if (!seen.has(name)) {
      seen.add(name);
      out.push({ name, index: match.index });
    }
  }
  return out;
}

/**
 * Format a display name as the mention token a user would type:
 * `@"Full Name"` when it contains a space, otherwise `@Name`.
 */
export function formatMentionToken(displayName: string): string {
  return displayName.includes(' ') ? `@"${displayName}"` : `@${displayName}`;
}

// ─── MentionLink ──────────────────────────────────────────────────────────────

interface MentionLinkProps {
  name: string;
  cachedUser?: MentionUser;
  onFetchUser: (name: string) => Promise<MentionUser | null>;
}

/**
 * Renders a clickable `@name` that opens a user profile popover. If the user is
 * not already cached it is fetched lazily on first click.
 */
export const MentionLink: React.FC<MentionLinkProps> = ({
  name,
  cachedUser,
  onFetchUser,
}) => {
  const [user, setUser] = React.useState<MentionUser | null>(cachedUser || null);
  const [loading, setLoading] = React.useState(false);

  const handleClick = async (e: React.MouseEvent) => {
    e.stopPropagation();
    if (!user && !loading) {
      setLoading(true);
      const fetchedUser = await onFetchUser(name);
      setUser(fetchedUser);
      setLoading(false);
    }
  };

  if (user) {
    return (
      <UserProfilePopover userId={user.id} displayName={user.displayName}>
        <button
          type="button"
          className="text-primary font-medium hover:underline cursor-pointer inline bg-transparent border-none p-0"
          onClick={(e) => e.stopPropagation()}
        >
          @{name}
        </button>
      </UserProfilePopover>
    );
  }

  return (
    <button
      type="button"
      className="text-primary font-medium hover:underline cursor-pointer inline bg-transparent border-none p-0"
      onClick={handleClick}
      disabled={loading}
    >
      @{name}
    </button>
  );
};

/**
 * Split a plain-text fragment into nodes, replacing @mentions with
 * {@link MentionLink} elements. Plain (non-mention) spans are returned as-is.
 */
export function splitMentionsInText(
  text: string,
  keyPrefix: string,
  options: {
    mentionUserCache: Map<string, MentionUser>;
    onFetchUser: (name: string) => Promise<MentionUser | null>;
  }
): React.ReactNode[] {
  const regex = new RegExp(MENTION_REGEX.source, MENTION_REGEX.flags);
  const out: React.ReactNode[] = [];
  let lastIndex = 0;
  let match: RegExpExecArray | null;
  while ((match = regex.exec(text)) !== null) {
    if (match.index > lastIndex) {
      out.push(text.substring(lastIndex, match.index));
    }
    const name = match[1] || match[2];
    out.push(
      <MentionLink
        key={`${keyPrefix}-mention-${match.index}`}
        name={name}
        cachedUser={options.mentionUserCache.get(name)}
        onFetchUser={options.onFetchUser}
      />
    );
    lastIndex = match.index + match[0].length;
  }
  if (lastIndex < text.length) {
    out.push(text.substring(lastIndex));
  }
  return out.length > 0 ? out : [text];
}
