import { useEffect, useRef, useState } from 'react';
import { useAuth } from '../contexts';
import { presenceService, PresenceEntityType, PresenceUpdateEvent, PresenceViewer } from '../services/presenceService';

/** How often to re-send a heartbeat while the entity stays mounted (ms). */
const HEARTBEAT_INTERVAL_MS = 20_000;

/**
 * Tracks "who else is viewing this" for a Pitch / Retrospective / Wiki page
 * (v1.13.0 S64). Sends a heartbeat on mount and every {@link HEARTBEAT_INTERVAL_MS},
 * sends a best-effort "leave" on unmount (or when the entity changes away),
 * and listens for the `presence-update` SSE event (forwarded as a `window`
 * CustomEvent by NotificationCenter.tsx) to keep the viewer list live.
 *
 * The current user's own id is always excluded from the returned `viewers` —
 * a user should never see themselves in their own "who else is here" stack.
 *
 * No-ops entirely (no heartbeat, empty viewers) while `entityId` is nullish —
 * this covers a page mounting before its data (and therefore its id) has loaded.
 */
export function usePresence(
  entityType: PresenceEntityType,
  entityId: number | null | undefined
): { viewers: PresenceViewer[] } {
  const { user } = useAuth();
  const [viewers, setViewers] = useState<PresenceViewer[]>([]);
  const currentUserIdRef = useRef<number | undefined>(user?.userId);
  currentUserIdRef.current = user?.userId;

  useEffect(() => {
    if (entityId == null) {
      setViewers([]);
      return;
    }

    presenceService.heartbeat(entityType, entityId).catch(() => {
      // Best-effort — a missed heartbeat just means this viewer drops out of
      // the presence set until the next successful one.
    });

    const intervalId = setInterval(() => {
      presenceService.heartbeat(entityType, entityId).catch(() => {});
    }, HEARTBEAT_INTERVAL_MS);

    const handlePresenceUpdate = (event: Event) => {
      const detail = (event as CustomEvent<PresenceUpdateEvent>).detail;
      if (!detail || detail.entityType !== entityType || detail.entityId !== entityId) {
        return;
      }
      const others = detail.viewers.filter((v) => v.userId !== currentUserIdRef.current);
      setViewers(others);
    };
    window.addEventListener('presence-update', handlePresenceUpdate);

    return () => {
      clearInterval(intervalId);
      window.removeEventListener('presence-update', handlePresenceUpdate);
      // Reset immediately so switching to a different entity (without a full
      // unmount) doesn't briefly show the previous entity's stale viewers.
      setViewers([]);
      presenceService.leave(entityType, entityId).catch(() => {});
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [entityType, entityId]);

  return { viewers };
}
