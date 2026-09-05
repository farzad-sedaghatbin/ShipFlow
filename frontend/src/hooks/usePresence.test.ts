import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { usePresence } from './usePresence';

const heartbeatMock = vi.fn((_entityType: string, _entityId: number) => Promise.resolve());
const leaveMock = vi.fn((_entityType: string, _entityId: number) => Promise.resolve());

vi.mock('../services/presenceService', () => ({
  presenceService: {
    heartbeat: (entityType: string, entityId: number) => heartbeatMock(entityType, entityId),
    leave: (entityType: string, entityId: number) => leaveMock(entityType, entityId),
  },
}));

let currentUser: { userId: number } | null = { userId: 1 };
vi.mock('../contexts', () => ({
  useAuth: () => ({ user: currentUser }),
}));

interface TimerCall {
  cb: () => void;
  delay: number;
}

let intervalCalls: TimerCall[];

/**
 * Reuses the "stub the global timer function to capture calls" technique
 * established in useNotificationStream.test.ts (S63), adapted from
 * setTimeout to setInterval since usePresence re-sends its heartbeat on a
 * fixed interval rather than with backoff.
 */
function stubFakeInterval(): void {
  intervalCalls = [];
  vi.stubGlobal(
    'setInterval',
    ((cb: () => void, delay: number) => {
      const id = intervalCalls.length;
      intervalCalls.push({ cb, delay });
      return id as unknown as ReturnType<typeof setInterval>;
    }) as typeof setInterval
  );
  vi.stubGlobal('clearInterval', (() => {}) as typeof clearInterval);
}

describe('usePresence', () => {
  beforeEach(() => {
    heartbeatMock.mockClear();
    leaveMock.mockClear();
    currentUser = { userId: 1 };
    stubFakeInterval();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  it('sends a heartbeat immediately on mount', () => {
    renderHook(() => usePresence('PITCH', 42));

    expect(heartbeatMock).toHaveBeenCalledTimes(1);
    expect(heartbeatMock).toHaveBeenCalledWith('PITCH', 42);
  });

  it('schedules a repeat heartbeat every 20 seconds and fires it on each tick', () => {
    renderHook(() => usePresence('WIKI_PAGE', 7));

    expect(intervalCalls.length).toBe(1);
    expect(intervalCalls[0].delay).toBe(20_000);

    intervalCalls[0].cb();
    expect(heartbeatMock).toHaveBeenCalledTimes(2);

    intervalCalls[0].cb();
    expect(heartbeatMock).toHaveBeenCalledTimes(3);
    // Always the same entity on every tick.
    expect(heartbeatMock).toHaveBeenNthCalledWith(3, 'WIKI_PAGE', 7);
  });

  it('does nothing (no heartbeat, no interval) when entityId is nullish', () => {
    const { result, rerender } = renderHook(({ id }: { id: number | null }) => usePresence('PITCH', id), {
      initialProps: { id: null },
    });

    expect(heartbeatMock).not.toHaveBeenCalled();
    expect(intervalCalls.length).toBe(0);
    expect(result.current.viewers).toEqual([]);

    rerender({ id: undefined as unknown as null });
    expect(heartbeatMock).not.toHaveBeenCalled();
  });

  it('sends a best-effort leave call on unmount', () => {
    const { unmount } = renderHook(() => usePresence('RETROSPECTIVE', 99));

    expect(leaveMock).not.toHaveBeenCalled();
    unmount();

    expect(leaveMock).toHaveBeenCalledTimes(1);
    expect(leaveMock).toHaveBeenCalledWith('RETROSPECTIVE', 99);
  });

  it('updates viewers on a matching presence-update event, excluding the current user', () => {
    const { result } = renderHook(() => usePresence('PITCH', 42));

    act(() => {
      window.dispatchEvent(
        new CustomEvent('presence-update', {
          detail: {
            entityType: 'PITCH',
            entityId: 42,
            viewers: [
              { userId: 1, displayName: 'Me' },
              { userId: 2, displayName: 'Alice' },
              { userId: 3, displayName: 'Bob' },
            ],
          },
        })
      );
    });

    expect(result.current.viewers).toEqual([
      { userId: 2, displayName: 'Alice' },
      { userId: 3, displayName: 'Bob' },
    ]);
  });

  it('ignores a presence-update event for a different entityId', () => {
    const { result } = renderHook(() => usePresence('PITCH', 42));

    act(() => {
      window.dispatchEvent(
        new CustomEvent('presence-update', {
          detail: { entityType: 'PITCH', entityId: 99, viewers: [{ userId: 2, displayName: 'Alice' }] },
        })
      );
    });

    expect(result.current.viewers).toEqual([]);
  });

  it('ignores a presence-update event for a different entityType', () => {
    const { result } = renderHook(() => usePresence('PITCH', 42));

    act(() => {
      window.dispatchEvent(
        new CustomEvent('presence-update', {
          detail: { entityType: 'WIKI_PAGE', entityId: 42, viewers: [{ userId: 2, displayName: 'Alice' }] },
        })
      );
    });

    expect(result.current.viewers).toEqual([]);
  });
});
