import api from './api';
import { getStoredToken } from '../contexts';

export type PresenceEntityType = 'PITCH' | 'RETROSPECTIVE' | 'WIKI_PAGE';

export interface PresenceViewer {
  userId: number;
  displayName: string;
}

/** Payload of the `presence-update` SSE event (see NotificationCenter's generic forwarding). */
export interface PresenceUpdateEvent {
  entityType: PresenceEntityType;
  entityId: number;
  viewers: PresenceViewer[];
}

function presenceUrl(entityType: PresenceEntityType, entityId: number): string {
  return `/presence/${entityType}/${entityId}`;
}

export const presenceService = {
  /** POST /api/presence/{entityType}/{entityId}/heartbeat -> 204 No Content */
  heartbeat: (entityType: PresenceEntityType, entityId: number) =>
    api.post<void>(`${presenceUrl(entityType, entityId)}/heartbeat`),

  /**
   * DELETE /api/presence/{entityType}/{entityId} -> 204 No Content
   *
   * Best-effort "I'm leaving" signal. Uses a raw `fetch` with `keepalive: true`
   * (not the shared axios instance) because this is typically called from an
   * effect cleanup / unmount / navigation, where the browser may tear down the
   * page before an in-flight axios request completes — `keepalive` lets the
   * request survive that. A missed leave call is not a real bug: the presence
   * entry simply expires naturally ~45s later on the server.
   */
  leave: async (entityType: PresenceEntityType, entityId: number): Promise<void> => {
    try {
      const token = getStoredToken();
      await fetch(`/api${presenceUrl(entityType, entityId)}`, {
        method: 'DELETE',
        headers: token ? { Authorization: `Bearer ${token}` } : undefined,
        keepalive: true,
      });
    } catch {
      // Best-effort — ignore failures (see doc comment above).
    }
  },
};
