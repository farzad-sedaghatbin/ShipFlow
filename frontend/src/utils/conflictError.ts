import { AxiosError } from 'axios';

/**
 * Optimistic-lock conflict body (v1.13.0 S64). Returned with HTTP 409 by the
 * Pitch / RetroItem / WikiPage update endpoints when the request's
 * `expectedVersion` no longer matches the entity's current version.
 *
 * The backend's `GlobalExceptionHandler` builds every error response as a flat
 * `Map<String, Object>` (see e.g. its `handleDataIntegrityViolationException` —
 * `message`/`messageKey`/`status` sit as plain sibling keys, never nested under
 * an envelope object), so these fields are expected as direct siblings of the
 * usual `message`/`status`/`timestamp` keys, not nested under e.g. `error` or
 * `details`.
 */
export interface OptimisticLockConflictBody<T = unknown> {
  /**
   * NOTE: distinct from (and not to be confused with) the presence API's
   * `PresenceEntityType` ("PITCH" | "RETROSPECTIVE" | "WIKI_PAGE") — a retro
   * item's *conflict* entityType is "RETRO_ITEM" because the versioned entity
   * being edited is the individual item, not the retrospective as a whole.
   */
  entityType: 'PITCH' | 'RETRO_ITEM' | 'WIKI_PAGE';
  entityId: number;
  currentVersion: number;
  current: T;
  // Other envelope fields (message, messageKey, status, timestamp, …) may also
  // be present but aren't needed by callers of this helper.
  [key: string]: unknown;
}

/**
 * If `error` is an axios error carrying an HTTP 409 optimistic-lock conflict
 * body (see {@link OptimisticLockConflictBody}), return that body. Otherwise
 * return `null` so the caller falls back to normal error handling.
 *
 * Detection is structural (`typeof data.currentVersion === 'number'`) rather
 * than just `status === 409`, because a 409 can also mean a plain
 * duplicate-data conflict (see `services/api.ts`'s response interceptor) that
 * has no `currentVersion`/`current` fields and should NOT open a conflict
 * dialog.
 */
export function getConflictBody<T = unknown>(error: unknown): OptimisticLockConflictBody<T> | null {
  const axiosError = error as AxiosError<Partial<OptimisticLockConflictBody<T>>> | undefined;
  const data = axiosError?.response?.data;
  if (
    axiosError?.response?.status === 409 &&
    data &&
    typeof data === 'object' &&
    typeof data.currentVersion === 'number' &&
    'current' in data
  ) {
    return data as OptimisticLockConflictBody<T>;
  }
  return null;
}
