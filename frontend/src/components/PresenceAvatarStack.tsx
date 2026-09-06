import { useTranslation } from 'react-i18next';
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from './ui/tooltip';
import { cn } from '../lib/utils';
import type { PresenceViewer } from '../services/presenceService';

const MAX_VISIBLE = 3;

/** Deterministic (userId-based) background color so a given person's avatar stays consistent across renders/pages. */
const AVATAR_COLORS = [
  'bg-rose-500',
  'bg-amber-500',
  'bg-emerald-500',
  'bg-sky-500',
  'bg-violet-500',
  'bg-pink-500',
  'bg-teal-500',
  'bg-orange-500',
];

function colorForUserId(userId: number): string {
  const index = Math.abs(userId) % AVATAR_COLORS.length;
  return AVATAR_COLORS[index];
}

function getInitials(displayName: string): string {
  const parts = displayName.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) return '?';
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
  return (parts[0][0] + parts[1][0]).toUpperCase();
}

interface PresenceAvatarStackProps {
  viewers: PresenceViewer[];
}

/**
 * Small overlapping-circle "who else is viewing this" indicator (v1.13.0 S64).
 * Renders nothing when there are no other viewers. Wraps its own
 * `TooltipProvider` so it works standalone on pages that don't already have
 * one in their tree (e.g. PitchDetail, WikiPage) — nesting a second provider
 * inside a page that already has one (e.g. RetroBoard) is harmless.
 */
export function PresenceAvatarStack({ viewers }: PresenceAvatarStackProps) {
  const { t } = useTranslation();

  if (viewers.length === 0) return null;

  const visible = viewers.slice(0, MAX_VISIBLE);
  const overflowCount = viewers.length - visible.length;

  return (
    <TooltipProvider delayDuration={200}>
      <div
        className="flex items-center"
        role="group"
        aria-label={t('presence.viewingCount', { count: viewers.length })}
      >
        {visible.map((viewer, index) => (
          <Tooltip key={viewer.userId}>
            <TooltipTrigger asChild>
              <div
                className={cn(
                  'flex h-7 w-7 select-none items-center justify-center rounded-full border-2 border-background text-[10px] font-semibold text-white',
                  index > 0 && '-ml-2',
                  colorForUserId(viewer.userId)
                )}
              >
                {getInitials(viewer.displayName)}
              </div>
            </TooltipTrigger>
            <TooltipContent>{viewer.displayName}</TooltipContent>
          </Tooltip>
        ))}
        {overflowCount > 0 && (
          <div className="-ml-2 flex h-7 w-7 select-none items-center justify-center rounded-full border-2 border-background bg-muted text-[10px] font-semibold text-muted-foreground">
            +{overflowCount}
          </div>
        )}
      </div>
    </TooltipProvider>
  );
}

export default PresenceAvatarStack;
